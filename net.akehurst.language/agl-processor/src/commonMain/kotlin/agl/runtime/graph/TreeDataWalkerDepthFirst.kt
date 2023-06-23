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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.api.sppt.IndexOfTotal
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.sppt.SpptWalker
import net.akehurst.language.collections.MutableStack

internal class TreeDataWalkerDepthFirst<CN : SpptDataNode>(
    private val treeData: TreeDataComplete<CN>
) {

    companion object {
        private data class StackInfo(
            val done: Boolean,
            override val node: SpptDataNode,
            override val option: IndexOfTotal,
            override val child: IndexOfTotal,

            override val numChildren: Int,
            override val numSkipChildren: Int
        ) : SpptDataNodeInfo
    }

    private val path = MutableStack<SpptDataNode>()
    private val stack = MutableStack<StackInfo>()

    fun traverse(callback: SpptWalker, skipDataAsTree: Boolean) {
        // handle <GOAL>
        val goal = treeData.root!!
        val userRoot = treeData.childrenFor(goal)[0].second[0]
        val urchildOfTotal = IndexOfTotal(0, 1)
        val uralternatives = treeData.childrenFor(userRoot)
        if (path.elements.contains(userRoot)) {
            path.push(userRoot)
            callback.error("Loop in Tree, Grammar has a recursive path that does not consume any input.") {
                path.elements
            }
        } else {
            path.push(userRoot)
            for ((alt, children) in uralternatives) {
                val optionOfTotal = IndexOfTotal(alt, uralternatives.size)
                val urnumSkipChildren = numSkipChildren(treeData.initialSkip) +
                        children.filter { it.rule.isTerminal }
                            .fold(0) { acc, it -> treeData.skipDataAfter(it)?.let { acc + numSkipChildren(it) } ?: acc }
                stack.push(StackInfo(true, userRoot, optionOfTotal, urchildOfTotal, children.size, urnumSkipChildren))
                callback.beginBranch(stack.peek())
                this.traverseSkipData(callback, skipDataAsTree, treeData.initialSkip)
                for (i in children.indices.reversed()) {
                    val ch = children[i]
                    val childOfTotal = IndexOfTotal(i, children.size)
                    stack.push(StackInfo(false, ch, optionOfTotal, childOfTotal, -1, -1))
                }
            }
        }

        while (stack.isNotEmpty) {
            val info = stack.pop()
            if (info.node.rule.isTerminal) {
                traverseLeaf(callback, skipDataAsTree, info)
            } else {
                traverseBranch(callback, skipDataAsTree, info)
            }
        }

    }

    private fun numSkipChildren(skipData: TreeDataComplete<CN>?): Int {
        return if (null == skipData) {
            0
        } else {
            val goal = skipData.root!!
            val skipMulti = skipData.childrenFor(goal)[0].second[0]
            val multi = skipData.childrenFor(skipMulti)[0].second
            multi.size
        }
    }

    fun traverseSkip(callback: SpptWalker, skipDataAsTree: Boolean) {
        // handle <GOAL>, <SKIP-MULTI> & <SKIP-CHOICE>
        val goal = treeData.root!!
        val skipMulti = treeData.childrenFor(goal)[0].second[0]
        val multi = treeData.childrenFor(skipMulti)[0].second
        for (i in multi.indices.reversed()) {
            val n = multi[i]
            val childOfTotal = IndexOfTotal(i, multi.size + 1) //TODO: should get the 1 passed in as actual number of siblings
            val skp = treeData.childrenFor(n)[0].second[0]
            stack.push(StackInfo(false, skp, IndexOfTotal(0, 1), childOfTotal, -1, -1))
        }

        while (stack.isNotEmpty) {
            val info = stack.pop()
            if (info.node.rule.isTerminal) {
                traverseLeaf(callback, skipDataAsTree, info)
            } else {
                traverseBranch(callback, skipDataAsTree, info)
            }
        }

    }

    private fun traverseSkipData(callback: SpptWalker, skipDataAsTree: Boolean, skipData: TreeDataComplete<CN>?) {
        if (null != skipData) {
            if (skipDataAsTree) {
                val walker = TreeDataWalkerDepthFirst<CN>(skipData)
                walker.traverseSkip(callback, skipDataAsTree)
            } else {
                callback.skip(skipData.root!!.startPosition, skipData.root!!.nextInputPosition)
            }
        }
    }

    private fun traverseLeaf(callback: SpptWalker, skipDataAsTree: Boolean, info: StackInfo) {
        if (info.node.rule.isEmbedded) {
            val ed = treeData.embeddedFor(info.node) ?: error("Cannot find embedded TreeData for '${info.node}'")
            callback.beginEmbedded(info)
            ed.traverseTreeDepthFirst(callback, skipDataAsTree)
            this.traverseSkipData(callback, skipDataAsTree, treeData.skipDataAfter(info.node))
            callback.endEmbedded(info)
        } else {
            callback.leaf(info)
            this.traverseSkipData(callback, skipDataAsTree, treeData.skipDataAfter(info.node))
        }
    }

    private fun traverseBranch(callback: SpptWalker, skipDataAsTree: Boolean, info: StackInfo) {
        if (info.done) {
            callback.endBranch(info)
            path.pop()
        } else {
            if (path.elements.contains(info.node)) {
                path.push(info.node)
                callback.error("Loop in Tree, Grammar has a recursive path that does not consume any input.") {
                    path.elements
                }
            } else {
                path.push(info.node)
                val alternatives = treeData.childrenFor(info.node)
                for ((alt, children) in alternatives) {
                    val option = IndexOfTotal(alt, alternatives.size)
                    val nnumSkipChildren = children.filter { it.rule.isTerminal }
                        .fold(0) { acc, it -> treeData.skipDataAfter(it)?.let { acc + numSkipChildren(it) } ?: acc }
                    stack.push(StackInfo(true, info.node, option, info.child, children.size, nnumSkipChildren))
                    callback.beginBranch(stack.peek())
                    val totChildrenIncSkip = children.size + nnumSkipChildren
                    for (i in children.indices.reversed()) {
                        val ch = children[i]
                        val nchildOfTotal = IndexOfTotal(i, totChildrenIncSkip)
                        stack.push(StackInfo(false, ch, option, nchildOfTotal, -1, -1))
                    }
                }
            }
        }
    }
}