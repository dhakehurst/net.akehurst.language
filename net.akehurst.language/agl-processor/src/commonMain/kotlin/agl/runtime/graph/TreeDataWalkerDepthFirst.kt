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

import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.MutableStack

internal class TreeDataWalkerDepthFirst<CN : SpptDataNode>(
    private val treeData: TreeDataComplete<CN>
) {

    companion object {
        private interface StackData {

        }

        private data class StackInfo(
            val done: Boolean,
            override val node: SpptDataNode,
            /**
             * (option number matched, total num options matched)
             */
            override val alt: AltInfo,
            /**
             * (child index in parent, total num children of parent)
             */
            override val child: ChildInfo,
            /**
             * num children of this node
             */
            override val numChildren: Int,
            /**
             * num skip children of this node
             */
            override val numSkipChildren: Int
        ) : SpptDataNodeInfo, StackData

        private data class AlternativesInfo(
            val node: SpptDataNode
        ) : StackData {
            val alternatives = mutableListOf<Pair<StackInfo, List<SpptDataNode>>>()
        }
    }

    // stack of (Node, Alternaitives) Alternatives decremented each pop, until 0 the realy pop
    private val path = MutableStack<AlternativesInfo>()
    private val stack = MutableStack<StackData>()

    fun traverse(callback: SpptWalker, skipDataAsTree: Boolean) {
        // handle <GOAL>
        val goal = treeData.root!!
        val userRoot = treeData.childrenFor(goal)[0].second[0]
        val urchildOfTotal = ChildInfo(0, 1)
        val uralternatives = treeData.childrenFor(userRoot).sortedBy { it.first }
        val altInfo = AlternativesInfo(userRoot)
        path.push(altInfo)
        stack.push(altInfo)
        for (i in uralternatives.indices.reversed()) {
            val (alt, children) = uralternatives[i]
            val altOfTotal = AltInfo(alt, i, uralternatives.size)
            val urnumSkipChildren = numSkipChildren(treeData.initialSkip) +
                    children.filter { it.rule.isTerminal }
                        .fold(0) { acc, it -> treeData.skipDataAfter(it)?.let { acc + numSkipChildren(it) } ?: acc }
            val stackData = StackInfo(true, userRoot, altOfTotal, urchildOfTotal, children.size, urnumSkipChildren)
            altInfo.alternatives.add(Pair(stackData, children))
            this.traverseSkipData(callback, skipDataAsTree, treeData.initialSkip)
        }

        while (stack.isNotEmpty) {
            val info = stack.pop()
            traverseStackData(callback, skipDataAsTree, info)
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
            val childOfTotal = ChildInfo(i, multi.size + 1) //TODO: should get the 1 passed in as actual number of siblings
            val skp = treeData.childrenFor(n)[0].second[0]
            stack.push(StackInfo(false, skp, AltInfo(0, 0, 1), childOfTotal, -1, -1))
        }

        while (stack.isNotEmpty) {
            val info = stack.pop()
            traverseStackData(callback, skipDataAsTree, info)
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

    private fun traverseStackData(callback: SpptWalker, skipDataAsTree: Boolean, stackData: StackData) {
        when (stackData) {
            is StackInfo -> {
                if (stackData.node.rule.isTerminal) {
                    traverseLeaf(callback, skipDataAsTree, stackData)
                } else {
                    traverseBranch(callback, skipDataAsTree, stackData)
                }
            }

            is AlternativesInfo -> traverseAlternative(callback, skipDataAsTree, stackData)

            else -> error("Internal Error: Should not happen")
        }
    }

    private fun traverseAlternative(callback: SpptWalker, skipDataAsTree: Boolean, info: AlternativesInfo) {
        if (0 == info.alternatives.size) {
            // end of alternatives
            path.pop()
        } else {
            val altChildrenInfo = info.alternatives.removeLast()
            val nodeInfo = altChildrenInfo.first
            val children = altChildrenInfo.second
            stack.push(info)
            stack.push(nodeInfo)
            val altOfTotal = nodeInfo.alt
            callback.beginBranch(nodeInfo)
            val totChildrenIncSkip = children.size + nodeInfo.numSkipChildren
            for (i in children.indices.reversed()) {
                val ch = children[i]
                val childOfTotal = ChildInfo(i, totChildrenIncSkip)
                // carry the childOfTotal, rest is unused
                stack.push(StackInfo(false, ch, altOfTotal, childOfTotal, -1, -1))
            }
        }
    }

    private fun traverseBranch(callback: SpptWalker, skipDataAsTree: Boolean, info: StackInfo) {
        if (info.done) {
            callback.endBranch(info)
        } else {
            val alternatives = treeData.childrenFor(info.node).sortedBy { it.first }
            val altInfo = AlternativesInfo(info.node)
            if (path.elements.any { it.node == altInfo.node }) {
                callback.error("Loop in Tree, Grammar has a recursive path that does not consume any input.") {
                    path.elements.map { it.node }
                }
            } else {
                path.push(altInfo)
                stack.push(altInfo)
                for (i in alternatives.indices.reversed()) {
                    val (alt, children) = alternatives[i]
                    val altOfTotal = AltInfo(alt, i, alternatives.size)
                    val numSkipChildren = children.filter { it.rule.isTerminal }
                        .fold(0) { acc, it -> treeData.skipDataAfter(it)?.let { acc + numSkipChildren(it) } ?: acc }
                    val stackData = StackInfo(true, info.node, altOfTotal, info.child, children.size, numSkipChildren)
                    altInfo.alternatives.add(Pair(stackData, children))
                }
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

}