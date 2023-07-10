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

import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.sppt.SpptWalker
import net.akehurst.language.collections.mutableStackOf

typealias BranchHandler2<T> = (SpptDataNodeInfo, children: List<Any>, Any?) -> T

abstract class SyntaxAnalyserFromTreeDataAbstract<out AsmType : Any> : SyntaxAnalyser<AsmType> {

    private val branchHandlers: MutableMap<String, BranchHandler2<*>> = mutableMapOf()

    override val locationMap = mutableMapOf<Any, InputLocation>()

    protected fun <T> register(branchName: String, handler: BranchHandler2<T>) {
        this.branchHandlers[branchName] = handler
    }

    private fun <T> findBranchHandler(branchName: String): BranchHandler2<T> {
        val handler: BranchHandler2<T>? = this.branchHandlers[branchName] as BranchHandler2<T>?
        return handler ?: error("Cannot find SyntaxAnalyser branch handler method named $branchName")
    }

    fun <T> walkTree(sentence: String, treeData: TreeDataComplete<out SpptDataNode>, arg: Any?): T {
        val stack = mutableStackOf<Any>()
        val walker = object : SpptWalker {
            override fun leaf(nodeInfo: SpptDataNodeInfo) {
                val matchedText = sentence.substring(nodeInfo.node.startPosition, nodeInfo.node.nextInputPosition)
                stack.push(matchedText)
            }

            override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun endBranch(nodeInfo: SpptDataNodeInfo) {
                val opt = nodeInfo.alt.option
                val numChildren = nodeInfo.numChildrenAlternatives[opt]!!
                val children = stack.pop(numChildren)
                val branchName = nodeInfo.node.rule.tag
                val handler = this@SyntaxAnalyserFromTreeDataAbstract.findBranchHandler<Any>(branchName)
                val obj = handler.invoke(nodeInfo, children, arg)
                stack.push(obj)
            }

            override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun endEmbedded(nodeInfo: SpptDataNodeInfo) {
                TODO("not implemented")
            }

            override fun error(msg: String, path: () -> List<SpptDataNode>) {
                TODO("not implemented")
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                TODO("not implemented")
            }
        }
        treeData.traverseTreeDepthFirst(walker, true)

        val root = stack.pop()
        return root as T
    }

}