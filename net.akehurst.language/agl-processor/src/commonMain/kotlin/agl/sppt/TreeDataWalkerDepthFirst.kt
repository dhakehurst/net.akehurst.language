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

package net.akehurst.language.agl.sppt

import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.MutableStack

internal interface StackData {

}

internal data class StackInfo(
    val done: Boolean,
    override val node: SpptDataNode,

    override val parentAlt: AltInfo,
    /**
     * (option number matched, total num options matched)
     */
    override val alt: AltInfo,
    /**
     * (child index in parent, total num children of parent)
     */
    override val child: ChildInfo,


    /**
     * num children for each alternative set for this node
     */
    override val numChildrenAlternatives: Map<Int, Int>,

    /**
     * num skip children of this node
     */
    override val numSkipChildren: Int
) : SpptDataNodeInfo, StackData {

    override val totalChildrenFromAllAlternatives: Int get() = numChildrenAlternatives.values.fold(0) { acc, it -> acc + it }
}

internal data class AlternativesInfo(
    val node: SpptDataNode,
    val isRoot: Boolean
) : StackData {
    val alternatives = mutableListOf<Pair<StackInfo, List<SpptDataNode>>>()
}

internal class TreeDataWalkerDepthFirst<CN : SpptDataNode>(
    private val treeData: TreeDataComplete<CN>
) {

    companion object {

    }

    // stack of (Node, Alternatives) Alternatives decremented each pop, until 0 then really pop
    private val path = MutableStack<AlternativesInfo>()
    private val stack = MutableStack<StackData>()

    fun traverse(callback: SpptWalker, skipDataAsTree: Boolean) {
        callback.beginTree()
        // handle <GOAL>
        val goal = treeData.root!!
        val userRoot = treeData.childrenFor(goal)[0].second[0]
        val parentAlt = AltInfo(0, 0, 1) //no parent alts for root, probably not used
        when {
            userRoot.rule.isTerminal -> {
                val urchildOfTotal = ChildInfo(0, 0, 1)
                val altOfTotal = AltInfo(0, 0, 1)
                stack.push(StackInfo(false, userRoot, parentAlt, altOfTotal, urchildOfTotal, emptyMap(), -1))
            }

            else -> {
                val urchildOfTotal = ChildInfo(0, 0, 1)
                val uralternatives = treeData.childrenFor(userRoot).sortedBy { it.first }
                val altInfo = AlternativesInfo(userRoot, true)
                path.push(altInfo)
                stack.push(altInfo)
                val numChildrenAlternatives = uralternatives.associate { Pair(it.first, it.second.size) }
                for (i in uralternatives.indices.reversed()) {
                    val (alt, children) = uralternatives[i]
                    val altOfTotal = AltInfo(alt, i, uralternatives.size)
                    //val numChildrenAlternatives = numChildrenAlternatives(children)
                    val urnumSkipChildren = numSkipChildren(treeData.initialSkip) +
                            children.filter { it.rule.isTerminal }
                                .fold(0) { acc, it -> treeData.skipDataAfter(it)?.let { acc + numSkipChildren(it) } ?: acc }
                    val stackData = StackInfo(true, userRoot, parentAlt, altOfTotal, urchildOfTotal, numChildrenAlternatives, urnumSkipChildren)
                    altInfo.alternatives.add(Pair(stackData, children))
                }
            }
        }

        while (stack.isNotEmpty) {
            val info = stack.pop()
            traverseStackData(callback, skipDataAsTree, info)
        }
        callback.endTree()
    }

    /**
     * for each child, the number of alternatives
     */
    private fun numChildrenAlternatives(children: List<CN>) =
        children.mapIndexed { index, it ->
            when {
                it.rule.isEmbedded -> Pair(index, 1)
                it.rule.isTerminal -> Pair(index, 1)
                else -> {
                    val x = treeData.childrenFor(it)
                    Pair(index, x.size)
                }
            }
        }.associate { it }

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
        val parentAlt = AltInfo(0, 0, 1) //no parent alts for root, probably not used
        for (i in multi.indices.reversed()) {
            val n = multi[i]
            val childOfTotal = ChildInfo(i, i, multi.size + 1) //TODO: is this correct ?
            val skp = treeData.childrenFor(n)[0].second[0]
            stack.push(StackInfo(false, skp, parentAlt, AltInfo(0, 0, 1), childOfTotal, emptyMap(), -1))
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
            callback.beginBranch(nodeInfo)
            if (info.isRoot) {
                this.traverseSkipData(callback, skipDataAsTree, treeData.initialSkip)
            }
            val totChildrenIncSkip = children.size + nodeInfo.numSkipChildren
            for (i in children.indices.reversed()) {
                val ch = children[i]
                val propertyIndex = when {
                    nodeInfo.node.rule.isList -> 0
                    else -> i
                }
                val childOfTotal = ChildInfo(propertyIndex, i, totChildrenIncSkip)
                // carry the childOfTotal, rest is unused
                stack.push(StackInfo(false, ch, nodeInfo.parentAlt, nodeInfo.alt, childOfTotal, emptyMap(), -1))
            }
        }
    }

    private fun traverseBranch(callback: SpptWalker, skipDataAsTree: Boolean, info: StackInfo) {
        if (info.done) {
            callback.endBranch(info)
        } else {
            val altInfo = AlternativesInfo(info.node, false)
            if (path.elements.any { it.node == altInfo.node }) {
                callback.error("Loop in Tree, Grammar has a recursive path that does not consume any input.") {
                    path.elements.map { it.node }
                }
            } else {
                val alternatives = treeData.childrenFor(info.node).sortedBy { it.first }
                path.push(altInfo)
                stack.push(altInfo)
                val numChildrenAlternatives = alternatives.associate { Pair(it.first, it.second.size) }
                for (i in alternatives.indices.reversed()) {
                    val (alt, children) = alternatives[i]
                    val altOfTotal = AltInfo(alt, i, alternatives.size)
                    //val numChildrenAlternatives = numChildrenAlternatives(children)
                    val numSkipChildren = children.filter { it.rule.isTerminal }
                        .fold(0) { acc, it -> treeData.skipDataAfter(it)?.let { acc + numSkipChildren(it) } ?: acc }
                    val stackData = StackInfo(true, info.node, info.alt, altOfTotal, info.child, numChildrenAlternatives, numSkipChildren)
                    altInfo.alternatives.add(Pair(stackData, children))
                }
            }
        }
    }

    private fun traverseLeaf(callback: SpptWalker, skipDataAsTree: Boolean, info: StackInfo) {
        if (info.node.rule.isEmbedded) {
            val ed = treeData.embeddedFor(info.node) ?: error("Cannot find embedded TreeData for '${info.node}'")
            val numChildrenAlternatives = mapOf(0 to 1) //FIXME: should recalc this from embedded data
            val numSkipChildren = treeData.skipDataAfter(info.node)?.let { numSkipChildren(it) } ?: 0
            val stackData = StackInfo(true, info.node, info.parentAlt, info.alt, info.child, numChildrenAlternatives, numSkipChildren)
            callback.beginEmbedded(stackData)
            ed.traverseTreeDepthFirst(callback, skipDataAsTree)
            this.traverseSkipData(callback, skipDataAsTree, treeData.skipDataAfter(info.node))
            callback.endEmbedded(stackData)
        } else {
            callback.leaf(info)
            this.traverseSkipData(callback, skipDataAsTree, treeData.skipDataAfter(info.node))
        }
    }

}