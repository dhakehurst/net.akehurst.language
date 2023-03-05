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

internal class TreeData(
    val forStateSetNumber: Int
) {

    val complete = TreeDataComplete(forStateSetNumber)

    val growingChildren: Map<GrowingNodeIndex, List<CompleteNodeIndex>> get() = this._growingChildren

    private val _growingChildren = mutableMapOf<GrowingNodeIndex, MutableList<CompleteNodeIndex>>()
    private val _growingCount = mutableMapOf<GrowingNodeIndex, Int>()
    private val _numberOfParents = mutableMapOf<CompleteNodeIndex, Int>()

    fun preferred(node: CompleteNodeIndex): CompleteNodeIndex? = this.complete.preferred(node)

    fun start(gni: GrowingNodeIndex, initialSkipData: TreeDataComplete?) {
        val growing = mutableListOf<CompleteNodeIndex>()
        this._growingChildren[gni] = growing
        this.complete.start(initialSkipData)
    }

    /**
     * remove the (completed) tree and recursively all its child-nodes - unless they have other parents
     *
     */
    fun removeTree(node: GrowingNodeIndex, isGrowing:(GrowingNodeIndex)->Boolean) {
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

    private fun removeTreeGrowing(node: GrowingNodeIndex) {
        val childrenOfRemoved = this._growingChildren.remove(node)
        childrenOfRemoved?.forEach { child -> decrementParents(child) }
    }

    fun hasGrowingParent(node: GrowingNodeIndex): Boolean = (_numberOfParents[node.complete] ?: 0) > 0

    /*
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
    */
    fun setEmbeddedChild(parent: GrowingNodeIndex, child: CompleteNodeIndex) {
        if (parent.runtimeState.isAtEnd) {
            val completeChildren = listOf(child)
            this.complete.setChildren(parent.complete, completeChildren, true)  //might it ever not be preferred!
        } else {
            error("Internal error: should not happen")
        }
    }

    fun setFirstChild(parent: GrowingNodeIndex, child: GrowingNodeIndex, isAlternative: Boolean) {
        if (parent.runtimeState.isAtEnd) {
            val completeChildren = listOf(child.complete)
            this.complete.setChildren(parent.complete, completeChildren, isAlternative)
        } else {
            var growing = this._growingChildren[parent]
            if (null == growing) {
                growing = mutableListOf(child.complete)
                this._growingChildren[parent] = growing
            } else {
                // replacing first child
                growing = mutableListOf(child.complete)
                this._growingChildren[parent] = growing
            }
        }
        this.incrementParents(child.complete)
    }

    /**
     * if this completes the parent, record complete parent
     */
    fun setInGrowingParentChildAt(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, nextChild: GrowingNodeIndex, isAlternative: Boolean) {
        val nextChildIndex = newParent.numNonSkipChildren - 1
        val children = this._growingChildren[oldParent]!! //should never be null //TODO: remove it
        if (newParent.runtimeState.isAtEnd) {
            //clone children so the other can keep growing if need be
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            when {
                cpy.size > nextChildIndex -> cpy[nextChildIndex] = nextChild.complete
                cpy.size == nextChildIndex -> cpy.add(nextChild.complete)
                else -> error("Internal error: should never happen")
            }
            this.complete.setChildren(newParent.complete, cpy, isAlternative)
            cpy.forEach {   this.incrementParents(it) }
        } else {
            //TODO: performance don't want to copy
            val cpy = children.toMutableList()
            when {
                cpy.size > nextChildIndex -> cpy[nextChildIndex] = nextChild.complete
                cpy.size == nextChildIndex -> cpy.add(nextChild.complete)
                else -> error("Internal error: should never happen")
            }
            this._growingChildren[newParent] = cpy
            cpy.forEach {   this.incrementParents(it) }
        }
    }

    fun inGrowingParentChildAt(parent: GrowingNodeIndex, childIndex: Int): CompleteNodeIndex? {
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
        other !is TreeData -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"

    private fun hasParents(child: CompleteNodeIndex) = (this._numberOfParents[child] ?: 0) != 0

    /*
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
    */
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

    fun incrementGrowing(gni: GrowingNodeIndex) {
        val count = _growingCount[gni]
        when (count) {
            null -> _growingCount[gni] = 1
            else -> _growingCount[gni] = count + 1
        }
    }

    fun decrementGrowing(gni: GrowingNodeIndex) {
        val count = _growingCount[gni]
        when (count) {
            null -> error("Internal Error: should never happen")
            1 -> removeGrowingNode(gni)
            else -> _growingCount[gni] = count - 1
        }
    }

    private fun removeGrowingNode(gni: GrowingNodeIndex) {
        _growingCount.remove(gni)
        _growingChildren.remove(gni)
    }

}