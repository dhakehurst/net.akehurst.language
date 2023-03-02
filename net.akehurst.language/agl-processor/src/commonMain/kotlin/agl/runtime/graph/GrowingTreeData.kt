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

internal class GrowingTreeData(
    val forStateSetNumber: Int
) {

    val completeChildren: Map<CompleteNodeIndex, Map<List<Int>, List<CompleteNodeIndex>>> get() = this._complete
    val growing: Map<GrowingNodeIndex, List<CompleteNodeIndex>> get() = this._growing
    var root: CompleteNodeIndex? = null; private set
    var initialSkip: GrowingTreeData? = null; private set

    // needed when parsing embedded sentences and skip
    val startPosition: Int? get() = root?.startPosition
    val nextInputPosition: Int? get() = root?.nextInputPosition


    private val _growing = mutableMapOf<GrowingNodeIndex, MutableList<CompleteNodeIndex>>()
    private val _growingCount = mutableMapOf<GrowingNodeIndex, Int>()

    // index --> map-of-alternatives (optionList,lists-of-children)
    //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = mutableMapOf<CompleteNodeIndex, MutableMap<List<Int>, List<CompleteNodeIndex>>>()
    private val _numberOfParents = mutableMapOf<CompleteNodeIndex, Int>()
    private val _preferred = mutableMapOf<PreferredChildIndex, CompleteNodeIndex>()

    private val _skipDataAfter = hashMapOf<CompleteNodeIndex, GrowingTreeData>()

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

    fun start(key: GrowingNodeIndex, initialSkipData: GrowingTreeData?) {
        val growing = mutableListOf<CompleteNodeIndex>()
        this._growing[key] = growing
        this.initialSkip = initialSkipData
    }

    /**
     * remove the (completed) tree and recursively all its child-nodes - unless they have other parents
     *
     */
    fun removeTree(node: GrowingNodeIndex) {
        if (node.runtimeState.isAtEnd) {
            val n = this._numberOfParents[node.complete] ?: 0
            if (0 == n) removeTreeComplete(node.complete)
        } else {
            removeTreeGrowing(node)
        }
    }

    private fun removeTreeGrowing(node: GrowingNodeIndex) {
        val childrenOfRemoved = this._growing.remove(node)
        childrenOfRemoved?.forEach { child ->
            val n = this._numberOfParents[child] ?: error("Internal Error: can't remove child with no recorded parents")
            this._numberOfParents[child] = n - 1
            if (1 == n) removeTreeComplete(child)
        }
    }

    private fun removeTreeComplete(node: CompleteNodeIndex) {
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

    fun setSkipDataAfter(leafNodeIndex: CompleteNodeIndex, skipData: GrowingTreeData) {
        _skipDataAfter[leafNodeIndex] = skipData
    }

    fun skipChildrenAfter(nodeIndex: CompleteNodeIndex) = this._skipDataAfter[nodeIndex]

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is GrowingTreeData -> false
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

    private fun incrementGrowing(gni:GrowingNodeIndex) {
        val count = _growingCount[gni]
        when {
            null==count -> _growingCount[gni] = 1
            else -> _growingCount[gni] = count+1
        }
    }
    private fun decrementGrowing(gni:GrowingNodeIndex) {
        val count = _growingCount[gni]
        when {
            null==count -> error("Internal Error: should never happen")
            1 == count -> {
                _growingCount.remove(gni)
                _growing.remove(gni)
            }
            else -> _growingCount[gni] = count-1
        }
    }
}