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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.sppt.SpptWalker

data class CompleteKey(
    val rule: Rule,
    val startPosition: Int,
    val nextInputPosition: Int,
)

// public so it can be serialised
class TreeDataComplete2(
    val forStateSetNumber: Int,
) : TreeData {
    companion object {
        //private val CompleteKey.preferred get() = PreferredNode(this.rule, this.startPosition)
        private val SpptDataNode.preferred get() = PreferredNode(this.rule, this.startPosition)
        //private val SpptDataNode.completeKey get() = CompleteKey(this.rule, this.startPosition, this.nextInputPosition)
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

    //TODO: is _preferred actually useful ??
    override fun childrenFor(node: SpptDataNode): List<Pair<Int, List<SpptDataNode>>> {
//        val keys = this._complete.keys.filter {
//            it.startPosition == node.startPosition && it.nextInputPosition == node.nextInputPosition && it.rule == node.rule
//        }
        val alternatives = this._complete[node]
        return when (alternatives) {
            null -> emptyList()
            //1 -> this._complete[keys[0]]!!.entries.map { Pair(it.key, it.value) }
            else -> {
//                val preferred = node.preferred
                //if (Debug.CHECK) check(keys.all { it.preferred == preferred })
                //               if (_preferred.containsKey(preferred)) {
                //                   this._complete[_preferred[preferred]!!]!!.entries.map { Pair(it.key, it.value) }
                //               } else {
                alternatives.map { Pair(it.key, it.value) }
                //               }
            }
        }
    }

    override fun skipDataAfter(node: SpptDataNode) = this._skipDataAfter[node]
    override fun embeddedFor(node: SpptDataNode) = this._embeddedFor[node]

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

    override fun setRoot(root: SpptDataNode) {
        this.root = root
    }

    override fun start(initialSkipData: TreeData?) {
        this.initialSkip = initialSkipData
    }

    override fun remove(node: SpptDataNode) {
        this._complete[node]?.remove(node.option)
        if (this._complete[node]?.isEmpty() == true) this._complete.remove(node)
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
        val ck = parent
        var alternatives = this._complete[ck]
        if (null == alternatives) {
            alternatives = mutableMapOf(parent.option to children)
            this._complete[ck] = alternatives
            if (isAlternative) {
                //ensure other is not preferred
                this._preferred.remove(ck.preferred)
            } else {
                this._preferred[ck.preferred] = parent
            }
        } else {
            if (isAlternative) {
                //ensure other is not preferred
                this._preferred.remove(ck.preferred)
            } else {
                this._preferred[ck.preferred] = parent
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
        other !is TreeDataComplete2 -> false
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