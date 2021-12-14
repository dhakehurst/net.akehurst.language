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

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RuntimeRule

internal class TreeData(
    val forStateSetNumber:Int
) {

    // child --> parent
    private val _parent = mutableMapOf<GrowingNodeIndex, GrowingNodeIndex>()

    private val _growing = mutableMapOf<GrowingNodeIndex, MutableList<CompleteNodeIndex>>()

    // (state,startPosition) --> listOf<child> //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = mutableMapOf<CompleteNodeIndex, MutableList<CompleteNodeIndex>>()
    private val _completedBy = mutableMapOf<CompleteNodeIndex, CompleteNodeIndex>()

    val completeChildren: Map<CompleteNodeIndex, List<CompleteNodeIndex>> = this._complete
    val completedBy: Map<CompleteNodeIndex, CompleteNodeIndex> = _completedBy
    val growing:Map<GrowingNodeIndex, List<CompleteNodeIndex>> = _growing
    var root: CompleteNodeIndex? = null; private set
    var initialSkip: TreeData? = null; private set

    // needed when parsing embedded sentences and skip
    val startPosition: Int? get() = root?.startPosition
    val nextInputPosition: Int? get() = root?.nextInputPosition

    fun createGrowingNodeIndex(state: ParserState, lhs: LookaheadSet, startPosition: Int, nextInputPosition: Int, numNonSkipChildren: Int): GrowingNodeIndex {
        val listSize = GrowingNodeIndex.listSize(state.runtimeRules.first(), numNonSkipChildren)
        return GrowingNodeIndex(this, state, lhs, startPosition, nextInputPosition, listSize)
    }

    fun childrenFor(runtimeRule: RuntimeRule, option: Int, startPosition: Int, nextInputPosition: Int): Set<List<CompleteNodeIndex>> {
        val keys = this._complete.keys.filter {
            it.startPosition == startPosition
                    && it.nextInputPosition == nextInputPosition
                    //&& it.state.options.contains(option)
                    && it.runtimeRules.contains(runtimeRule)
        }
        return when (keys.size) {
            0 -> emptySet()
            1 -> setOf(this._complete[keys[0]]!!)
            else -> error("should not happen")
        }
    }

    fun hasComplete(node: GrowingNodeIndex): Boolean = this._complete.containsKey(node.complete)

    fun setRoot(root: GrowingNodeIndex) {
        this.root = root.complete
    }

    fun start(key: GrowingNodeIndex, initialSkipData: TreeData?) {
        val growing = mutableListOf<CompleteNodeIndex>()
        this._growing[key] = growing
        this.initialSkip = initialSkipData
    }

    fun setEmbeddedChild(parent: GrowingNodeIndex, child: CompleteNodeIndex) {
        if (parent.state.isAtEnd) {
            var completeChildren = this._complete[parent.complete]
            if (null == completeChildren) { //TODO: handle childrenAlternatives ?
                completeChildren = mutableListOf(child)
                this._complete[parent.complete] = completeChildren
                this.setCompletedBy(parent)
            } else {
                // replacing first child
                completeChildren = mutableListOf(child)
                this._complete[parent.complete] = completeChildren
                this.setCompletedBy(parent)
            }
        } else {
            error("Internal error: should not happen")
        }
    }

    fun setFirstChild(parent: GrowingNodeIndex, child: GrowingNodeIndex, skipData: TreeData?) {
        val skipChildren = skipData?.let {
            val sr = skipData.completeChildren[skipData.root]!!.get(0)
            val c = skipData.completeChildren[sr]!!.map {
                skipData.completeChildren[it]!!.get(0)
            }
            c
        }
        if (parent.state.isAtEnd) {
            var completeChildren = this._complete[parent.complete]
            if (null == completeChildren) { //TODO: handle childrenAlternatives ?
                completeChildren = mutableListOf(child.complete)
                if (null != skipChildren) completeChildren.addAll(skipChildren)
                this._complete[parent.complete] = completeChildren
                this.setCompletedBy(parent)
            } else {
                // replacing first child
                completeChildren = mutableListOf(child.complete)
                if (null != skipChildren) completeChildren.addAll(skipChildren)
                this._complete[parent.complete] = completeChildren
                this.setCompletedBy(parent)
            }
        }
        // due to States containing multiple RPs...a state could mark the end and not the end for different RPs
        if (parent.state.isNotAtEnd) {
            var growing = this._growing[parent]
            if (null == growing) {
                growing = mutableListOf(child.complete)
                if (null != skipChildren) growing.addAll(skipChildren)
                this._growing[parent] = growing
            } else {
                // replacing first child
                growing = mutableListOf(child.complete)
                if (null != skipChildren) growing.addAll(skipChildren)
                this._growing[parent] = growing
            }
        }
    }

    fun appendChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, nextChild: GrowingNodeIndex, skipData: TreeData?) {
        val skipChildren = skipData?.let {
            val sr = skipData.completeChildren[skipData.root]!!.get(0)
            val c = skipData.completeChildren[sr]!!.map {
                skipData.completeChildren[it]!!.get(0)
            }
            c
        }
        val children = this._growing[oldParent]!! //should never be null
        if (newParent.state.isAtEnd) {
            //clone children so the other can keep growing if need be
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            cpy.add(nextChild.complete)
            if (null != skipChildren) cpy.addAll(skipChildren)
            this._complete[newParent.complete] = cpy
            this.setCompletedBy(newParent)
        }
        if (newParent.state.isNotAtEnd) {
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            cpy.add(nextChild.complete)
            if (null != skipChildren) cpy.addAll(skipChildren)
            this._growing[newParent] = cpy
        }
    }

    fun setUserGoalChildrentAfterInitialSkip(nug: CompleteNodeIndex, userGoalChildren: List<CompleteNodeIndex>) {
        this._complete[nug] = userGoalChildren.toMutableList()
    }

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeData -> false
        else -> other.forStateSetNumber==this.forStateSetNumber
    }
    override fun toString(): String = "TreeData{${forStateSetNumber}}"

    private fun setCompletedBy(parent: GrowingNodeIndex) {
        this._completedBy[parent.complete] = parent.complete
    }
}