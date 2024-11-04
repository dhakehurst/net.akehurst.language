/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.sppt.treedata

import net.akehurst.language.agl.util.Debug
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.SpptWalker
import net.akehurst.language.sppt.api.TreeData

data class PreferredNode(
    val rule: Rule,
    val startPosition: Int
) {
    override fun toString(): String = "PN(${rule.tag},$startPosition)"
}

class CompleteTreeDataNode(
    override val rule: Rule,
    override val startPosition: Int,
    override val nextInputPosition: Int,
    override val nextInputNoSkip: Int,
    override val option: Int,
    override val dynamicPriority: Int
) : SpptDataNode {

    private val _hashCode_cache = arrayOf(rule, startPosition, nextInputPosition).contentHashCode()
    override fun hashCode(): Int = _hashCode_cache
    override fun equals(other: Any?): Boolean = when {
        other !is SpptDataNode -> false
        this.startPosition != other.startPosition -> false
        this.nextInputPosition != other.nextInputPosition -> false
        this.rule != other.rule -> false
        else -> true
    }

    override fun toString(): String = "CN(${rule.tag}|${option},$startPosition-$nextInputPosition)"
}

fun treeData(forStateSetNumber: Int): TreeData = TreeDataComplete2(forStateSetNumber)

// public so it can be serialised
class TreeDataComplete(
    val forStateSetNumber: Int,
    // the following are optional arguments to allow for serialisation
//    root: SpptDataNode? = null,
//    initialSkip: TreeData? = null
) : TreeData {

    companion object {
        private val SpptDataNode.preferred get() = PreferredNode(this.rule, this.startPosition)
    }

    override val isEmpty: Boolean get() = null == root && null == initialSkip && this._complete.isEmpty() && this._skipDataAfter.isEmpty() && this._embeddedFor.isEmpty()

    // made public for serialisation support
    val completeChildren: Map<SpptDataNode, Map<Int, List<SpptDataNode>>> get() = this._complete

    override var root: SpptDataNode? = null; private set
    override var initialSkip: TreeData? = null; private set

    override val userRoot get() = childrenFor(root!!).first().second.first()

    override fun setUserGoalChildrenAfterInitialSkip(nug: SpptDataNode, userGoalChildren: List<SpptDataNode>) {
        this._complete[nug] = mutableMapOf(0 to userGoalChildren.toMutableList())
    }

    override fun childrenFor(node: SpptDataNode): List<Pair<Int, List<SpptDataNode>>> {
        val keys = this._complete.keys.filter {
            it.startPosition == node.startPosition
                    && it.nextInputPosition == node.nextInputPosition
                    && it.rule == node.rule
        }
        return when (keys.size) {
            0 -> emptyList()
            1 -> this._complete[keys[0]]!!.entries.map { Pair(it.key, it.value) }
            else -> {
                val preferred = keys[0].preferred
                if (Debug.CHECK) check(keys.all { it.preferred == preferred })
                if (_preferred.containsKey(preferred)) {
                    this._complete[_preferred[preferred]]!!.entries.map { Pair(it.key, it.value) }
                } else {
                    keys.flatMap {
                        this._complete[it]!!.entries.map { Pair(it.key, it.value) }
                    }
                }
            }
        }
    }

    override fun skipDataAfter(node: SpptDataNode): TreeData? = this._skipDataAfter[node]
    override fun embeddedFor(node: SpptDataNode): TreeData? = this._embeddedFor[node]

    override fun skipNodesAfter(node: SpptDataNode): List<SpptDataNode> {
        /* remember
         * <SKIP-MULTI> = <SKIP-CHOICE>+
         * <SKIP-CHOICE> = SR-0 | ... | SR-n
         */
        val skipTreeData = this.skipDataAfter(node)
        return when (skipTreeData) {
            null -> emptyList()
            else -> {
                val sur = skipTreeData.userRoot
                val skpCh = skipTreeData.childrenFor(sur)
                return skpCh[0].second
            }
        }
    }
    // --- private implementation ---

    // index --> map-of-alternatives (optionList,lists-of-children)
    //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = hashMapOf<SpptDataNode, MutableMap<Int, List<SpptDataNode>>>()

    // map startPosition -> CN
    private val _preferred = hashMapOf<PreferredNode, SpptDataNode>()
    private val _skipDataAfter = hashMapOf<SpptDataNode, TreeData>()
    private val _embeddedFor = hashMapOf<SpptDataNode, TreeData>()

    // --- used only by TreeData ---
    fun reset() {
        this.root = null
        this.initialSkip = null
        this._complete.clear()
        this._preferred.clear()
        this._skipDataAfter.clear()
        this._embeddedFor.clear()
    }

    override fun preferred(node: SpptDataNode): SpptDataNode? = this._preferred[node.preferred]

    override fun setRootTo(root: SpptDataNode) {
        this.root = root
    }

    override fun start(initialSkipData: TreeData?) {
        this.initialSkip = initialSkipData
    }

    override fun remove(node: SpptDataNode) {
        this._complete.remove(node)
        this._preferred.remove(node.preferred)
        this._skipDataAfter.remove(node)
    }

    private fun setEmbeddedChild(parent: SpptDataNode, child: SpptDataNode) {
        val completeChildren = listOf(child)
        this.setCompletedBy(parent, completeChildren, true)  //might it ever not be preferred!
    }

    override fun setChildren(parent: SpptDataNode, completeChildren: List<SpptDataNode>, isAlternative: Boolean) {
        this.setCompletedBy(parent, completeChildren, isAlternative)
    }

    override fun setSkipDataAfter(leafNodeIndex: SpptDataNode, skipData: TreeData) {
        _skipDataAfter[leafNodeIndex] = skipData
    }

    override fun setEmbeddedTreeFor(n: SpptDataNode, treeData: TreeData) {
        _embeddedFor[n] = treeData
    }

    private fun setCompletedBy(parent: SpptDataNode, children: List<SpptDataNode>, isAlternative: Boolean) {
        var alternatives = this._complete[parent]
        if (null == alternatives) {
            alternatives = mutableMapOf(parent.option to children)
            this._complete[parent] = alternatives
            if (isAlternative) {
                //ensure other is not preferred
                this._preferred.remove(parent.preferred)
            } else {
                this._preferred[parent.preferred] = parent
            }
        } else {
            if (isAlternative) {
                //ensure other is not preferred
                this._preferred.remove(parent.preferred)
            } else {
                this._preferred[parent.preferred] = parent
                alternatives.clear()
            }
            alternatives[parent.option] = children
        }
    }

    override fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean) {
        val walker = TreeDataWalkerDepthFirst<SpptDataNode>(this)
        walker.traverse(callback, skipDataAsTree)
    }

    override fun matches(other: TreeData) = when {
        other !is TreeDataComplete -> false
        this.initialSkip != other.initialSkip -> false
        this._embeddedFor != other._embeddedFor -> false
        this._skipDataAfter != other._skipDataAfter -> false
        this._preferred != other._preferred -> false
        this._complete != other._complete -> false
        else -> true
    }

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeDataComplete -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"
}