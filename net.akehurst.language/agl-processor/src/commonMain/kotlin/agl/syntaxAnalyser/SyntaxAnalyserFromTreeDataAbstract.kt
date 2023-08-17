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

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsEmbedded
import net.akehurst.language.agl.sppt.SPPTFromTreeData
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.sppt.SpptWalker
import net.akehurst.language.collections.mutableStackOf
import kotlin.reflect.KFunction3

typealias BranchHandler2<T> = (SpptDataNodeInfo, children: List<Any?>, Any?) -> T?

val SpptDataNode.isEmptyMatch get() = this.startPosition == this.nextInputPosition
fun SpptDataNode.locationIn(sentence: String) = InputFromString.locationFor(sentence, this.startPosition, this.nextInputPosition - this.startPosition)
fun SpptDataNode.matchedTextNoSkip(sentence: String) = sentence.substring(this.startPosition, this.nextInputNoSkip)

abstract class SyntaxAnalyserFromTreeDataAbstract<out AsmType : Any> : SyntaxAnalyser<AsmType> {

    private val branchHandlers: MutableMap<String, BranchHandler2<*>> = mutableMapOf()
    private val branchHandlers_begin: MutableMap<String, BranchHandler2<*>> = mutableMapOf()

    override val locationMap = mutableMapOf<Any, InputLocation>()
    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    override fun clear() {
        this.locationMap.clear()
        this.issues.clear()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem): SyntaxAnalysisResult<AsmType> {
        val sentence = (sppt as SPPTFromTreeData).originalSentence
        val treeData = (sppt as SPPTFromTreeData).treeData
        val grammars = this.walkTree<AsmType>(sentence, treeData, false)
        this.embeddedSyntaxAnalyser.values.forEach {
            this.issues.addAll((it as SyntaxAnalyserFromTreeDataAbstract).issues)
        }
        return SyntaxAnalysisResultDefault(grammars, issues, locationMap)
    }


    protected fun <T : Any?> register(handler: KFunction3<SpptDataNodeInfo, List<Any?>, String, T>) {
        this.branchHandlers[handler.name] = handler as BranchHandler2<T>
    }

    protected fun <T : Any?> registerFor(branchName: String, handler: BranchHandler2<T>) {
        this.branchHandlers[branchName] = handler
    }

    protected fun <T : Any?> registerForBeginHandler(branchName: String, handler: KFunction3<SpptDataNodeInfo, List<Any?>, String, T>) {
        this.branchHandlers_begin[branchName] = handler as BranchHandler2<T>
    }

    private fun <T : Any?> findBranchHandler(branchName: String, begin: Boolean): BranchHandler2<T>? {
        return when {
            begin -> this.branchHandlers_begin[branchName] as BranchHandler2<T>?
            else -> this.branchHandlers[branchName] as BranchHandler2<T>?
        }
    }

    fun <T> walkTree(sentence: String, treeData: TreeDataComplete<out SpptDataNode>, skipDataAsTree: Boolean): T {
        val syntaxAnalyserStack = mutableStackOf(this)
        val stack = mutableStackOf<Any?>()
        val walker = object : SpptWalker {
            override fun beginTree() {}
            override fun endTree() {}
            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                when {
                    nodeInfo.node.rule.isEmptyTerminal -> {
                        stack.push(null)
                    }

                    else -> {
                        val handlerName = nodeInfo.node.rule.tag
                        val handler = syntaxAnalyserStack.peek().findBranchHandler<Any>(handlerName, false)
                        val obj = when (handler) {
                            null -> sentence.substring(nodeInfo.node.startPosition, nodeInfo.node.nextInputNoSkip)
                            else -> {
                                val res = handler.invoke(nodeInfo, emptyList(), sentence)
                                res?.let { locationMap[res] = nodeInfo.node.locationIn(sentence) }
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
                val adjChildren = when {
                    nodeInfo.node.rule.isList -> children.reversed() //listOf(children.filterNotNull())
                    else -> children.reversed()
                }

                when {
                    nodeInfo.node.rule.isOptional -> {
                        val obj = adjChildren[0]
                        obj?.let { locationMap[obj] = nodeInfo.node.locationIn(sentence) }
                        stack.push(obj)
                    }

                    else -> {
                        val branchName = nodeInfo.node.rule.tag
                        val handler = syntaxAnalyserStack.peek().findBranchHandler<Any>(branchName, false)
                        if (null == handler) {
                            //error("Cannot find SyntaxAnalyser branch handler method named $branchName or ${branchName}_end")
                            stack.push(adjChildren)
                        } else {
                            val obj = handler.invoke(nodeInfo, adjChildren, sentence)
                            obj?.let { locationMap[obj] = nodeInfo.node.locationIn(sentence) }
                            stack.push(obj)
                        }
                    }
                }
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                val embeddedRhs = (nodeInfo.node.rule as RuntimeRule).rhs as RuntimeRuleRhsEmbedded
                val embName = embeddedRhs.embeddedRuntimeRuleSet.name
                val embSyntaxAnalyser = embeddedSyntaxAnalyser[embName] as SyntaxAnalyserFromTreeDataAbstract? ?: error("Embedded SyntaxAnalyser not found for '$embName'")
                syntaxAnalyserStack.push(embSyntaxAnalyser)
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                syntaxAnalyserStack.pop()
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                TODO("not implemented")
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                //TODO: ?
            }
        }
        treeData.traverseTreeDepthFirst(walker, skipDataAsTree)
        val root = stack.pop()
        return root as T
    }

}