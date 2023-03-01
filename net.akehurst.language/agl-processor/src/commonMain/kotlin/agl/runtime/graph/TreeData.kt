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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.RuntimeRule

internal class TreeData(
    val forStateSetNumber: Int
) {

    private val _growing = mutableMapOf<GrowingNodeIndex, MutableList<CompleteNodeIndex>>()

    // index --> mpa-of-alternatives (optionList,lists-of-children)
    //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = mutableMapOf<CompleteNodeIndex, MutableMap<List<Int>, List<CompleteNodeIndex>>>()
    private val _numberOfParents = mutableMapOf<CompleteNodeIndex, Int>()
    private val _preferred = mutableMapOf<PreferredChildIndex, CompleteNodeIndex>()

    private val _skipDataAfter = hashMapOf<CompleteNodeIndex, TreeData>()

    val completeChildren: Map<CompleteNodeIndex, Map<List<Int>, List<CompleteNodeIndex>>> = this._complete

    //val completedBy: Map<CompleteNodeIndex, CompleteNodeIndex> = _completedBy
    val growing: Map<GrowingNodeIndex, List<CompleteNodeIndex>> = _growing
    var root: CompleteNodeIndex? = null; private set
    var initialSkip: TreeData? = null; private set

    // needed when parsing embedded sentences and skip
    val startPosition: Int? get() = root?.startPosition
    val nextInputPosition: Int? get() = root?.nextInputPosition

    fun createGrowingNodeIndex(
        state: ParserState,
        runtimeLookaheadSet: Set<LookaheadSet>,
        startPosition: Int,
        nextInputPosition: Int,
        nextInputPositionAfterSkip: Int,
        numNonSkipChildren: Int
    ): GrowingNodeIndex {
        val listSize = GrowingNodeIndex.listSize(state.runtimeRules.first(), numNonSkipChildren)
        return GrowingNodeIndex(this, RuntimeState(state, runtimeLookaheadSet), startPosition, nextInputPosition, nextInputPositionAfterSkip, listSize)
    }

    fun childrenFor(runtimeRule: RuntimeRule, startPosition: Int, nextInputPosition: Int): List<Pair<List<Int>, List<CompleteNodeIndex>>> {
        val keys = this._complete.keys.filter {
            it.startPosition == startPosition
                    && it.nextInputPosition == nextInputPosition
                    && it.runtimeRulesSet.contains(runtimeRule)
        }
        return when (keys.size) {
            0 -> emptyList()
            1 -> this._complete[keys[0]]!!.entries.map { Pair(it.key, it.value) }
            else -> if (_preferred.containsKey(keys[0].preferred)) {
                this._complete[_preferred[keys[0].preferred]]!!.entries.map { Pair(it.key, it.value) }
            } else {
                keys.flatMap {
                    this._complete[it]!!.entries.map { Pair(it.key, it.value) }
                }
            }
        }
    }

    fun preferred(node: CompleteNodeIndex): CompleteNodeIndex? = this._preferred[node.preferred]

    fun setRoot(root: GrowingNodeIndex) {
        this.root = root.complete
    }

    fun start(key: GrowingNodeIndex, initialSkipData: TreeData?) {
        val growing = mutableListOf<CompleteNodeIndex>()
        this._growing[key] = growing
        this.initialSkip = initialSkipData
    }

    private fun remove(node: CompleteNodeIndex) {
        this._complete.remove(node)
        this._preferred.remove(node.preferred)
        this._skipDataAfter.remove(node)
    }

    /**
     * remove the (completed) tree and recursively all its child-nodes - unless they have other parents
     *
     */
    fun removeTree(node: GrowingNodeIndex) {
        if (node.runtimeState.isAtEnd) {
            val n = this._numberOfParents[node.complete] ?: 0
            if (0==n) removeTreeComplete(node.complete)
        } else {
            removeTreeGrowing(node)
        }
    }
    private fun removeTreeGrowing(node:GrowingNodeIndex) {
        val childrenOfRemoved = this._growing.remove(node)
        childrenOfRemoved?.forEach { child ->
                val n = this._numberOfParents[child] ?: error("Internal Error: can't remove child with no recorded parents")
                this._numberOfParents[child] = n - 1
                if (1 == n) removeTreeComplete(child)
        }
    }
    private fun removeTreeComplete(node:CompleteNodeIndex) {
        val childrenOfRemoved = this._complete[node]
        this.remove(node)
        if (node.isEmbedded) {
            //nothing to remove, children stored in other TreeData
        } else {
            childrenOfRemoved?.entries?.forEach { (optionList, children) ->
                children.forEach { child ->
                    val n = this._numberOfParents[child] ?: error("Internal Error: can't remove child with no recorded parents")
                    this._numberOfParents[child] = n - 1
                    if (1 == n) removeTreeComplete(child)
                }
            }
        }
    }

    fun setEmbeddedChild(parent: GrowingNodeIndex, child: CompleteNodeIndex) {
        if (parent.runtimeState.isAtEnd) {
            val completeChildren = listOf(child)
            this.setCompletedBy(parent, completeChildren, true)  //might it ever not be preferred!
        } else {
            error("Internal error: should not happen")
        }
    }

    fun setFirstChild(parent: GrowingNodeIndex, child: GrowingNodeIndex, isAlternative: Boolean) {
        if (parent.runtimeState.isAtEnd) {
            val completeChildren = listOf(child.complete)
            this.setCompletedBy(parent, completeChildren, isAlternative)
        } else {
            var growing = this._growing[parent]
            if (null == growing) {
                growing = mutableListOf(child.complete)
                this._growing[parent] = growing
            } else {
                // replacing first child
                growing = mutableListOf(child.complete)
                this._growing[parent] = growing
            }
        }
        this.incrementParents(child.complete)
    }

    /*
    fun appendChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, nextChild: GrowingNodeIndex) {
        val children = this._growing[oldParent]!! //should never be null
        if (newParent.state.isAtEnd) {
            //clone children so the other can keep growing if need be
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            cpy.add(nextChild.complete)
            this.setCompletedBy(newParent, cpy)
        }
        if (newParent.state.isNotAtEnd) {
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            cpy.add(nextChild.complete)
            this._growing[newParent] = cpy
        }
    }
    */

    /**
     * if this completes the parent, record complete parent
     */
    fun setInGrowingParentChildAt(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, nextChild: GrowingNodeIndex, isAlternative: Boolean) {
        val nextChildIndex = newParent.numNonSkipChildren - 1
        val children = this._growing[oldParent]!! //should never be null //TODO: remove it
        if (newParent.runtimeState.isAtEnd) {
            //clone children so the other can keep growing if need be
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            when {
                cpy.size > nextChildIndex -> cpy[nextChildIndex] = nextChild.complete
                cpy.size == nextChildIndex -> cpy.add(nextChild.complete)
                else -> error("Internal error: should never happen")
            }
            this.setCompletedBy(newParent, cpy, isAlternative)
            cpy.forEach { ch -> incrementParents(ch) }
        } else {
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            when {
                cpy.size > nextChildIndex -> cpy[nextChildIndex] = nextChild.complete
                cpy.size == nextChildIndex -> cpy.add(nextChild.complete)
                else -> error("Internal error: should never happen")
            }
            this._growing[newParent] = cpy
            cpy.forEach { ch -> incrementParents(ch) }
        }

    }

    fun inGrowingParentChildAt(parent: GrowingNodeIndex, childIndex: Int): CompleteNodeIndex? {
        val children = this._growing[parent]
        return if (null == children) {
            null
        } else {
            if (childIndex < children.size) {
                children[childIndex]
            } else {
                null
            }
        }
    }

    fun setUserGoalChildrenAfterInitialSkip(nug: CompleteNodeIndex, userGoalChildren: List<CompleteNodeIndex>) {
        this._complete[nug] = mutableMapOf(listOf(0) to userGoalChildren.toMutableList())
    }

    fun setSkipDataAfter(leafNodeIndex: CompleteNodeIndex, skipData: TreeData) {
        _skipDataAfter[leafNodeIndex] = skipData
    }

    fun skipChildrenAfter(nodeIndex: CompleteNodeIndex) = this._skipDataAfter[nodeIndex]

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeData -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"

    private fun setCompletedBy(parent: GrowingNodeIndex, children: List<CompleteNodeIndex>, isAlternative: Boolean) {
        var alternatives = this._complete[parent.complete]
        if (null == alternatives) {
            alternatives = mutableMapOf(parent.runtimeState.optionList to children)
            this._complete[parent.complete] = alternatives
            if (isAlternative) {
                //ensure other is not preferred
                this._preferred.remove(parent.complete.preferred)
            } else {
                this._preferred[parent.complete.preferred] = parent.complete
            }
        } else {
            if (isAlternative) {
                //ensure other is not preferred
                this._preferred.remove(parent.complete.preferred)
            } else {
                this._preferred[parent.complete.preferred] = parent.complete
                alternatives.clear()
            }
            alternatives[parent.runtimeState.optionList] = children
        }
    }

    private fun incrementParents(child: CompleteNodeIndex) {
        val n = this._numberOfParents[child] ?: 0
        this._numberOfParents[child] = n + 1
    }
}