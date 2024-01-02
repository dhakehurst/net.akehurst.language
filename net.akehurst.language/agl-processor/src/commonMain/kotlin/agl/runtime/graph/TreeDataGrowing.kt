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

import net.akehurst.language.agl.sppt.TreeData
import net.akehurst.language.agl.sppt.treeData
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.sppt.SpptDataNode

internal class TreeDataGrowing<GN, CN : SpptDataNode>(
    val forStateSetNumber: Int
) {

    val isClean: Boolean get() = this._growingChildren.isEmpty() //&& this._numberOfGrowingParents.isEmpty()

    val isEmpty: Boolean get() = complete.isEmpty && this.isClean

    val complete = treeData(forStateSetNumber)

    val growingChildren: Map<GN, List<CN>> get() = this._growingChildren

    fun preferred(node: CN): CN? = (this.complete.preferred(node) as CN?)

    fun initialise(gni: GN, initialSkipData: TreeData?) {
        val growing = mutableListOf<CN>()
        this.setGrowingChildren(gni, growing)
        this.complete.start(initialSkipData)
    }

    /**
     * remove the (completed) tree and recursively all its child-nodes - unless they have other parents
     */
//    fun removeTree(node: GN) { //, isGrowing: (GN) -> Boolean) {
//        if (hasParents(node.complete) || isGrowing(node)) {
//            // do not remove
//        } else {
//            if (node.isComplete) {
//                removeTreeComplete(node.complete)
//            } else {
//        removeTreeGrowing(node)
//            }
//        }
    //   }

    fun removeTreeComplete(node: CN) {
        //val childrenOfRemoved = this.complete.completeChildren[node]
        this.complete.remove(node)
        // if (node.isEmbedded) {
        //     // children stored in other TreeData ? TODO
        // } else {
        // childrenOfRemoved?.entries?.forEach { (optionList, children) ->
        //     children.forEach { child -> decrementParents(child) }
        // }
        //}
    }

    fun removeTreeGrowing(node: GN) {
        val childrenOfRemoved = this.removeGrowingNode(node)
        //childrenOfRemoved?.forEach { child -> decrementGrowingParents(child) }
    }

    fun setEmbeddedChild(parent: SpptDataNode, child: SpptDataNode, embeddedTreeData: TreeData) {
        val completeChildren = listOf(child)
        this.complete.setChildren(parent, completeChildren, true)  //might it ever not be preferred!
        this.complete.setEmbeddedTreeFor(parent, embeddedTreeData)
    }

    fun setFirstChildForComplete(parent: CN, child: CN, isAlternative: Boolean) {
        val completeChildren = listOf(child)
        this.complete.setChildren(parent, completeChildren, isAlternative)
        //this.incrementParents(child)
    }

    fun setFirstChildForGrowing(parent: GN, child: CN) {
        val growing = this._growingChildren[parent]
        if (null == growing) {
            this.setGrowingChildren(parent, mutableListOf(child))
        } else {
            // replacing first child
            // decrementGrowingParents(growing[0])
            this.setGrowingChildren(parent, mutableListOf(child))
        }
        //this.incrementGrowingParents(child)
    }

    fun setNextChildForCompleteParent(oldParent: GN, newParent: CN, nextChild: CN, isAlternative: Boolean) {
        val children = this._growingChildren[oldParent]!! //should never be null //TODO: remove it
        //val children = this._growingChildren.remove(oldParent)!!
        //val nextChildIndex = oldParent.numNonSkipChildren
        val nextChildIndex = children.size
        //clone children so the other can keep growing if need be
        //TODO: performance don't want to copy
        val cpy = children.toMutableList()
        when {
            cpy.size > nextChildIndex -> {
                cpy[nextChildIndex] = nextChild
            }

            cpy.size == nextChildIndex -> cpy.add(nextChild)
            else -> error("Internal error: should never happen")
        }
        this.complete.setChildren(newParent, cpy, isAlternative)
        //cpy.forEach { this.incrementParents(it) }
    }

    fun setNextChildForGrowingParent(oldParent: GN, newParent: GN, nextChild: CN) {
        val children = this._growingChildren[oldParent]!! //should never be null //TODO: remove it
        //val nextChildIndex = newParent.numNonSkipChildren - 1
        val nextChildIndex = children.size
        //TODO: performance don't want to copy
        val cpy = children.toMutableList()
        when {
            cpy.size > nextChildIndex -> {
                cpy[nextChildIndex] = nextChild
            }

            cpy.size == nextChildIndex -> cpy.add(nextChild)
            else -> error("Internal error: should never happen")
        }
        this.setGrowingChildren(newParent, cpy)
        if (null == this._growingChildren[newParent]) {
            //cpy.forEach { this.incrementGrowingParents(it) }
        } else {
            //replacing an existing parents children, num of parents does not change
        }
    }

    fun inGrowingParentChildAt(parent: GN, childIndex: Int): CN? {
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

    fun setUserGoalChildrenAfterInitialSkip(nug: CN, userGoalChildren: List<CN>) {
        this.complete.setUserGoalChildrenAfterInitialSkip(nug, userGoalChildren)
    }

    fun setSkipDataAfter(leafNodeIndex: CN, skipData: TreeData) {
        this.complete.setSkipDataAfter(leafNodeIndex, skipData)
    }

    override fun hashCode(): Int = this.forStateSetNumber
    override fun equals(other: Any?): Boolean = when {
        other !is TreeDataGrowing<*, *> -> false
        else -> other.forStateSetNumber == this.forStateSetNumber
    }

    override fun toString(): String = "TreeData{${forStateSetNumber}}"

    // --- Implementation ---

    private val _growingChildren = hashMapOf<GN, List<CN>>()
    // private val _numberOfGrowingParents = hashMapOf<CN, Int>()

    // private fun hasParents(child: CN) = (this._numberOfGrowingParents[child] ?: 0) != 0
    /*
        private fun incrementGrowingParents(child: CN) {
            val n = this._numberOfGrowingParents[child] ?: 0
            this._numberOfGrowingParents[child] = n + 1
        }

        private fun decrementGrowingParents(child: CN) {
            val n = this._numberOfGrowingParents[child]
            when (n) {
                null -> error("Internal Error: can't remove child with no recorded parents")
                1 -> {
                    // removeTreeComplete(child)
                    this._numberOfGrowingParents.remove(child)
                }

                else -> this._numberOfGrowingParents[child] = n - 1
            }
        }
    */
    private fun setGrowingChildren(parent: GN, children: List<CN>) {
        if (Debug.OUTPUT_TREE_DATA) Debug.debug(Debug.IndentDelta.NONE) { "Set growing children: $parent = $children" }
        this._growingChildren[parent] = children
    }

    private fun removeGrowingNode(gni: GN): List<CN>? {
        if (Debug.OUTPUT_TREE_DATA) Debug.debug(Debug.IndentDelta.NONE) { "Remove growing parent: $gni" }
        return _growingChildren.remove(gni)
    }

}