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
import net.akehurst.language.agl.util.Debug

internal class TreeDataComplete(
    val forStateSetNumber: Int
) {

    // --- used to create SPPT ---

    val completeChildren: Map<CompleteNodeIndex, Map<List<Int>, List<CompleteNodeIndex>>> get() = this._complete
    var root: CompleteNodeIndex? = null; private set
    var initialSkip: TreeDataComplete? = null; private set

    // needed when parsing embedded sentences and skip
    val startPosition: Int? get() = root?.startPosition
    val nextInputPosition: Int? get() = root?.nextInputPosition

    fun createCompleteNodeIndex(
        state: ParserState,
        startPosition: Int,
        nextInputPosition: Int,
        nextInputPositionAfterSkip: Int,
        growingNodeIndex: GrowingNodeIndex?,
        childrenPriorities:List<List<Int>>?
    ): CompleteNodeIndex {
        return CompleteNodeIndex(this, state, startPosition, nextInputPosition, nextInputPositionAfterSkip, growingNodeIndex,childrenPriorities)
    }

    fun setUserGoalChildrenAfterInitialSkip(nug: CompleteNodeIndex, userGoalChildren: List<CompleteNodeIndex>) {
        this._complete[nug] = mutableMapOf(listOf(0) to userGoalChildren.toMutableList())
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
            else -> {
                val preferred = keys[0].preferred
                if (Debug.CHECK) check( keys.all { it.preferred == preferred } )
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

    fun skipChildrenAfter(nodeIndex: CompleteNodeIndex) = this._skipDataAfter[nodeIndex]

    // --- private implementation ---

    // index --> map-of-alternatives (optionList,lists-of-children)
    //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = mutableMapOf<CompleteNodeIndex, MutableMap<List<Int>, List<CompleteNodeIndex>>>()
    private val _preferred = mutableMapOf<PreferredChildIndex, CompleteNodeIndex>()
    private val _skipDataAfter = hashMapOf<CompleteNodeIndex, TreeDataComplete>()

    // --- used only by TreeData ---
    fun reset() {
        this.root = null
        this.initialSkip = null
        this._complete.clear()
        this._preferred.clear()
        this._skipDataAfter.clear()
    }

    fun preferred(node: CompleteNodeIndex): CompleteNodeIndex? = this._preferred[node.preferred]

    fun setRoot(root: CompleteNodeIndex) {
        this.root = root
    }

    fun start(initialSkipData: TreeDataComplete?) {
        this.initialSkip = initialSkipData
    }

    fun remove(node: CompleteNodeIndex) {
        this._complete.remove(node)
        this._preferred.remove(node.preferred)
        this._skipDataAfter.remove(node)
    }

    private fun setEmbeddedChild(parent: CompleteNodeIndex, child: CompleteNodeIndex) {
        val completeChildren = listOf(child)
        this.setCompletedBy(parent, completeChildren, true)  //might it ever not be preferred!
    }

    fun setChildren(parent: CompleteNodeIndex, completeChildren: List<CompleteNodeIndex>, isAlternative: Boolean) {
        this.setCompletedBy(parent, completeChildren, isAlternative)
    }

    fun setSkipDataAfter(leafNodeIndex: CompleteNodeIndex, skipData: TreeDataComplete) {
        _skipDataAfter[leafNodeIndex] = skipData
    }

    private fun setCompletedBy(parent: CompleteNodeIndex, children: List<CompleteNodeIndex>, isAlternative: Boolean) {
        var alternatives = this._complete[parent]
        if (null == alternatives) {
            alternatives = mutableMapOf(parent.optionList to children)
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
            alternatives[parent.optionList] = children
        }
    }

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeDataComplete -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"
}