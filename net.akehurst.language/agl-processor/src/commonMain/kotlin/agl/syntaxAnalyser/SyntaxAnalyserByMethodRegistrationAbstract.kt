/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.MutableStack
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.*
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.sppt.treedata.matchedTextNoSkip
import kotlin.js.JsExport
import kotlin.reflect.KFunction3

typealias BranchHandler<T> = KFunction3<SpptDataNodeInfo, List<Any?>, Sentence, T?>

abstract class SyntaxAnalyserByMethodRegistrationAbstract<AsmType : Any>
    : SyntaxAnalyserFromTreeDataAbstract<AsmType>() {

    companion object {
        private val EMPTY_LIST_VALUE = object {
            override fun toString(): String = "EMPTY_LIST_VALUE"
        }
        private val EMPTY_TERMINAL_VALUE = object {
            override fun toString(): String = "EMPTY_TERMINAL_VALUE"
        }
    }

    abstract fun registerHandlers()

    override val asm: AsmType get() = _root ?: error("Root of asm not set, walk must have failed")

    override fun walkTree(sentence: Sentence, treeData: TreeData, skipDataAsTree: Boolean) {
        val syntaxAnalyserStack: MutableStack<SyntaxAnalyserByMethodRegistrationAbstract<*>> = mutableStackOf(this)
        val stack = mutableStackOf<Any?>()
        val walker = object : SpptWalker {
            override fun beginTree() {}

            override fun endTree() {}

            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                when {
                    nodeInfo.node.rule.isEmptyTerminal -> {
                        stack.push(EMPTY_TERMINAL_VALUE)
                    }

                    nodeInfo.node.rule.isEmptyListTerminal -> {
                        stack.push(EMPTY_LIST_VALUE)
                    }

                    else -> {
                        val handlerName = nodeInfo.node.rule.tag
                        val handler = syntaxAnalyserStack.peek().findBranchHandler<Any>(handlerName, false)
                        val obj = when (handler) {
                            null -> sentence.matchedTextNoSkip(nodeInfo.node)
                            else -> {
                                val res = handler.invoke(nodeInfo, emptyList(), sentence)
                                res?.let { setLocationFor(res, nodeInfo, sentence) }
                                res
                            }
                        }
                        stack.push(obj)
                    }
                }
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                val branchName = nodeInfo.node.rule.tag
                val handler = syntaxAnalyserStack.peek().findBranchHandler<Any>(branchName, true)
                if (null == handler) {
                    // no begin handler
                } else {
                    val numChildren = nodeInfo.child.index
                    val siblings = stack.peek(numChildren)
                    handler.invoke(nodeInfo, siblings, sentence)
                }
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val reverseChildren = children.reversed()
                val branchName = nodeInfo.node.rule.tag
                val handler = syntaxAnalyserStack.peek().findBranchHandler<Any>(branchName, false)
                when {
                    nodeInfo.node.rule.isOptional -> {
                        val v = reverseChildren[0]
                        val child = when (v) {
                            EMPTY_TERMINAL_VALUE -> null
                            EMPTY_LIST_VALUE -> error("should not happen")
                            else -> v
                        }
                        when (handler) {
                            null -> stack.push(child)
                            else -> {
                                val obj = handler.invoke(nodeInfo, listOf(child), sentence)
                                obj?.let { setLocationFor(obj, nodeInfo, sentence)  }
                                stack.push(obj)
                            }
                        }
                    }

                    nodeInfo.node.rule.isList -> {
                        val chldn = when {
                            nodeInfo.node.rule.isListOptional && 1 == children.size && EMPTY_LIST_VALUE == children[0] -> emptyList<Any>()
                            else -> reverseChildren
                        }
                        when (handler) {
                            null -> stack.push(chldn)
                            else -> {
                                val obj = handler.invoke(nodeInfo, chldn, sentence)
                                obj?.let { setLocationFor(obj, nodeInfo, sentence) }
                                stack.push(obj)
                            }
                        }
                    }

                    else -> when (handler) {
                        null -> stack.push(reverseChildren)
                        else -> {
                            val obj = handler.invoke(nodeInfo, reverseChildren, sentence)
                            when (obj) {
                                null -> Unit
                                is Function<*> -> Unit // no point setting location for function/lambda
                                else -> setLocationFor(obj,nodeInfo,sentence)
                            }
                            stack.push(obj)
                        }
                    }
                }
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embeddedRhs = (nodeInfo.node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
                val embName = embeddedRhs.embeddedRuntimeRuleSet.qualifiedName
                val embSyntaxAnalyser = syntaxAnalyserStack.peek().embeddedSyntaxAnalyser[QualifiedName(embName)] as SyntaxAnalyserByMethodRegistrationAbstract? ?: error("Embedded SyntaxAnalyser not found for '$embName'")
                syntaxAnalyserStack.push(embSyntaxAnalyser)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                syntaxAnalyserStack.pop()

                val handlerName = nodeInfo.node.rule.tag
                val handler = syntaxAnalyserStack.peek().findBranchHandler<Any>(handlerName, false)
                val children = stack.pop(1)
                val obj = when (handler) {
                    null -> children[0]
                    else -> {
                        val obj = handler.invoke(nodeInfo, children, sentence)
                        obj?.let { setLocationFor(obj, nodeInfo, sentence) }
                        obj
                    }
                }
                stack.push(obj)
            }

            override fun treeError(msg: String, path: PathFunction) {
                kotlin.error(msg)
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                //TODO: ?
            }
        }
        treeData.traverseTreeDepthFirst(walker, skipDataAsTree)
        val root = stack.pop()
        this._root = root as AsmType
    }

    // --- ---

    protected fun <T : Any?> register(handler: BranchHandler<T>) {
        this.branchHandlers[handler.name] = handler as BranchHandler<T>
    }

    protected fun <T : Any?> registerFor(branchName: String, handler: BranchHandler<T>) {
        this.branchHandlers[branchName] = handler
    }

    protected fun <T : Any?> registerForBeginHandler(branchName: String, handler: BranchHandler<T>) {
        this.branchHandlers_begin[branchName] = handler as BranchHandler<T>
    }

    // --- implementation ---

    private val branchHandlers: MutableMap<String, BranchHandler<*>> = mutableMapOf()
    private val branchHandlers_begin: MutableMap<String, BranchHandler<*>> = mutableMapOf()

    private fun <T : Any?> findBranchHandler(branchName: String, begin: Boolean): BranchHandler<T>? {
        if (branchHandlers.isEmpty()) {
            registerHandlers()
        }
        //try to find handler in this SyntaxAnalyser
        val selfHandler = when {
            begin -> this.branchHandlers_begin[branchName] as BranchHandler<T>?
            else -> this.branchHandlers[branchName] as BranchHandler<T>?
        }
        // if not found then try extended SyntaxAnalysers
        val handler = selfHandler ?: extendsSyntaxAnalyser.firstNotNullOfOrNull {
            (it.value as SyntaxAnalyserByMethodRegistrationAbstract<AsmType>).findBranchHandler(branchName, begin)
        }
        return handler
    }

    private var _root: AsmType? = null


}