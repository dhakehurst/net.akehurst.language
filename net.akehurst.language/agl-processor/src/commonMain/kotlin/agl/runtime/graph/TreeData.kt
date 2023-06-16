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

import net.akehurst.language.agl.util.Debug

internal class TreeData<GN>(
    val forStateSetNumber: Int
) {

    val complete = TreeDataComplete(forStateSetNumber)

    val growingChildren: Map<GN, List<CompleteNodeIndex>> get() = this._growingChildren

    fun preferred(node: CompleteNodeIndex): CompleteNodeIndex? = this.complete.preferred(node)

    fun start(gni: GN, initialSkipData: TreeDataComplete?) {
        val growing = mutableListOf<CompleteNodeIndex>()
        this.setGrowingChildren(gni, growing)
        this.complete.start(initialSkipData)
    }

    /**
     * remove the (completed) tree and recursively all its child-nodes - unless they have other parents
     *
     */
    /*
    fun removeTree(node: GN, isGrowing: (GN) -> Boolean) {
        if (hasParents(node.complete) || isGrowing(node)) {
            // do not remove
        } else {
            if (node.isComplete) {
                removeTreeComplete(node.complete)
            } else {
                removeTreeGrowing(node)
            }
        }
    }
    */

    private fun removeTreeComplete(node: CompleteNodeIndex) {
        val childrenOfRemoved = this.complete.completeChildren[node]
        this.complete.remove(node)
        if (node.isEmbedded) {
            //nothing to remove, children stored in other TreeData
        } else {
            childrenOfRemoved?.entries?.forEach { (optionList, children) ->
                children.forEach { child -> decrementParents(child) }
            }
        }
    }

    /*
        private fun removeTreeGrowing(node: GN) {
            val childrenOfRemoved = this.removeGrowingNode(node)
            childrenOfRemoved?.forEach { child -> decrementParents(child) }
        }

        fun hasGrowingParent(node: GN): Boolean = (_numberOfParents[node.complete] ?: 0) > 0
    */
    fun setEmbeddedChild(parent: CompleteNodeIndex, child: CompleteNodeIndex) {
        val completeChildren = listOf(child)
        this.complete.setChildren(parent, completeChildren, true)  //might it ever not be preferred!
    }

    fun setFirstChildForComplete(parent: CompleteNodeIndex, child: CompleteNodeIndex, isAlternative: Boolean) {
        val completeChildren = listOf(child)
        this.complete.setChildren(parent, completeChildren, isAlternative)
        this.incrementParents(child)
    }

    fun setFirstChildForGrowing(parent: GN, child: CompleteNodeIndex) {
        var growing = this._growingChildren[parent]
        if (null == growing) {
            growing = mutableListOf(child)
            this.setGrowingChildren(parent, growing)
        } else {
            // replacing first child
            growing = mutableListOf(child)
            this.setGrowingChildren(parent, growing)
        }
        this.incrementParents(child)
    }

    fun setNextChildForCompleteParent(oldParent: GN, newParent: CompleteNodeIndex, nextChild: CompleteNodeIndex, isAlternative: Boolean) {
        val children = this._growingChildren[oldParent]!! //should never be null //TODO: remove it
        //val nextChildIndex = oldParent.numNonSkipChildren
        val nextChildIndex = children.size
        //clone children so the other can keep growing if need be
        //TODO: performance don't want to copy
        val cpy = children.toMutableList()
        when {
            cpy.size > nextChildIndex -> cpy[nextChildIndex] = nextChild
            cpy.size == nextChildIndex -> cpy.add(nextChild)
            else -> error("Internal error: should never happen")
        }
        this.complete.setChildren(newParent, cpy, isAlternative)
        cpy.forEach { this.incrementParents(it) }
    }

    fun setNextChildForGrowingParent(oldParent: GN, newParent: GN, nextChild: CompleteNodeIndex) {
        val children = this._growingChildren[oldParent]!! //should never be null //TODO: remove it
        //val nextChildIndex = newParent.numNonSkipChildren - 1
        val nextChildIndex = children.size
        //TODO: performance don't want to copy
        val cpy = children.toMutableList()
        when {
            cpy.size > nextChildIndex -> cpy[nextChildIndex] = nextChild
            cpy.size == nextChildIndex -> cpy.add(nextChild)
            else -> error("Internal error: should never happen")
        }
        this.setGrowingChildren(newParent, cpy)
        cpy.forEach { this.incrementParents(it) }
    }

    fun inGrowingParentChildAt(parent: GN, childIndex: Int): CompleteNodeIndex? {
        val children = this._growingChildren[parent]
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
        this.complete.setUserGoalChildrenAfterInitialSkip(nug, userGoalChildren)
    }

    fun setSkipDataAfter(leafNodeIndex: CompleteNodeIndex, skipData: TreeDataComplete) {
        this.complete.setSkipDataAfter(leafNodeIndex, skipData)
    }

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeData<*> -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"

    // --- Implementation ---

    private val _growingChildren = mutableMapOf<GN, List<CompleteNodeIndex>>()
    private val _numberOfParents = mutableMapOf<CompleteNodeIndex, Int>()

    private fun hasParents(child: CompleteNodeIndex) = (this._numberOfParents[child] ?: 0) != 0

    private fun incrementParents(child: CompleteNodeIndex) {
        val n = this._numberOfParents[child] ?: 0
        this._numberOfParents[child] = n + 1
    }

    private fun decrementParents(child: CompleteNodeIndex) {
        val n = this._numberOfParents[child]
        when (n) {
            null -> error("Internal Error: can't remove child with no recorded parents")
            1 -> {
                removeTreeComplete(child)
                this._numberOfParents.remove(child)
            }

            else -> this._numberOfParents[child] = n - 1
        }
    }

    private fun setGrowingChildren(parent: GN, children: List<CompleteNodeIndex>) {
        if (Debug.OUTPUT_TREE_DATA) Debug.debug(Debug.IndentDelta.NONE) { "Set growing children: $parent = $children" }
        this._growingChildren[parent] = children
    }

    private fun removeGrowingNode(gni: GN): List<CompleteNodeIndex>? {
        if (Debug.OUTPUT_TREE_DATA) Debug.debug(Debug.IndentDelta.NONE) { "Remove growing parent: $gni" }
        return _growingChildren.remove(gni)
    }

}