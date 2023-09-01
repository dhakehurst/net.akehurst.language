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
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.sppt.SpptDataNode
import net.akehurst.language.api.sppt.SpptWalker

internal data class PreferredNode(
    val rule: Rule,
    val startPosition: Int
) {
    override fun toString(): String = "PN(${rule.tag},$startPosition)"
}

data class CompleteTreeDataNode(
    override val rule: Rule,
    override val startPosition: Int,
    override val nextInputPosition: Int,
    override val nextInputNoSkip: Int,
    override val option: Int
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

// public so it can be serialised
class TreeDataComplete<CN : SpptDataNode>(
    val forStateSetNumber: Int,
    // the following are optional arguments to allow for serialisation
    root: CN? = null,
    initialSkip: TreeDataComplete<CN>? = null
) {

    companion object {
        private val SpptDataNode.preferred get() = PreferredNode(this.rule, this.startPosition)
    }

    val isEmpty: Boolean get() = null == root && null == initialSkip && this._complete.isEmpty() && this._skipDataAfter.isEmpty() && this._embeddedFor.isEmpty()
    val completeChildren: Map<CN, Map<Int, List<CN>>> get() = this._complete
    var root: CN? = root; private set
    var initialSkip: TreeDataComplete<CN>? = initialSkip; private set

    val userRoot get() = childrenFor(root!!).first().second.first()

    fun setUserGoalChildrenAfterInitialSkip(nug: CN, userGoalChildren: List<CN>) {
        this._complete[nug] = mutableMapOf(0 to userGoalChildren.toMutableList())
    }

    fun childrenFor(branch: SpptDataNode): List<Pair<Int, List<CN>>> {
        val keys = this._complete.keys.filter {
            it.startPosition == branch.startPosition && it.nextInputPosition == branch.nextInputPosition && it.rule == branch.rule
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

    fun skipDataAfter(nodeIndex: SpptDataNode) = this._skipDataAfter[nodeIndex]
    fun embeddedFor(nodeIndex: SpptDataNode) = this._embeddedFor[nodeIndex]

    // --- private implementation ---

    // index --> map-of-alternatives (optionList,lists-of-children)
    //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = hashMapOf<CN, MutableMap<Int, List<CN>>>()

    // map startPosition -> CN
    private val _preferred = hashMapOf<PreferredNode, CN>()
    private val _skipDataAfter = hashMapOf<CN, TreeDataComplete<CN>>()
    private val _embeddedFor = hashMapOf<CN, TreeDataComplete<CN>>()

    // --- used only by TreeData ---
    fun reset() {
        this.root = null
        this.initialSkip = null
        this._complete.clear()
        this._preferred.clear()
        this._skipDataAfter.clear()
        this._embeddedFor.clear()
    }

    fun preferred(node: SpptDataNode): SpptDataNode? = this._preferred[node.preferred]

    fun setRoot(root: CN) {
        this.root = root
    }

    fun start(initialSkipData: TreeDataComplete<CN>?) {
        this.initialSkip = initialSkipData
    }

    fun remove(node: CN) {
        this._complete.remove(node)
        this._preferred.remove(node.preferred)
        this._skipDataAfter.remove(node)
    }

    private fun setEmbeddedChild(parent: CN, child: CN) {
        val completeChildren = listOf(child)
        this.setCompletedBy(parent, completeChildren, true)  //might it ever not be preferred!
    }

    fun setChildren(parent: CN, completeChildren: List<CN>, isAlternative: Boolean) {
        this.setCompletedBy(parent, completeChildren, isAlternative)
    }

    fun setSkipDataAfter(leafNodeIndex: CN, skipData: TreeDataComplete<CN>) {
        _skipDataAfter[leafNodeIndex] = skipData
    }

    fun setEmbeddedTreeFor(n: CN, treeData: TreeDataComplete<CN>) {
        _embeddedFor[n] = treeData
    }

    private fun setCompletedBy(parent: CN, children: List<CN>, isAlternative: Boolean) {
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

    fun traverseTreeDepthFirst(callback: SpptWalker, skipDataAsTree: Boolean) {
        val walker = TreeDataWalkerDepthFirst<CN>(this)
        walker.traverse(callback, skipDataAsTree)
    }

    fun matches(other: TreeDataComplete<CN>) = when {
        this.initialSkip != other.initialSkip -> false
        this._embeddedFor != other._embeddedFor -> false
        this._skipDataAfter != other._skipDataAfter -> false
        this._preferred != other._preferred -> false
        this._complete != other._complete -> false
        else -> true
    }

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeDataComplete<*> -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"
}