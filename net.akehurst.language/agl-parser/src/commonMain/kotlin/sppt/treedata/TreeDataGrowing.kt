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
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.TreeData

/**
 * Append-only ("snoc") singly-linked chain of children for a growing parent.
 *
 * Storing the children of every growing parent as a [ChildChain] instead of
 * a [List] gives us:
 *  - O(1) [append]: a new chain shares its entire prefix with the receiver
 *    (no copy);
 *  - structural sharing across alternative parents that diverge from a
 *    common prefix (the very situation the previous `toMutableList()` was
 *    defending against by full copy).
 *
 * The trade-off: random index access is O(n - i). That's fine here --
 * reading is only required when materialising the final list to hand to
 * `complete.setChildren(...)`, which happens once per completion.
 *
 * The chain stores the *last* element at the head; [toList] walks
 * backwards then fills an array in reverse, costing one allocation of
 * size [size].
 */
internal class ChildChain<CN> private constructor(
    /** The most recently appended child (last element of the sequence). */
    val head: CN,
    /** The chain holding everything before [head]; `null` iff [size] == 1. */
    val prev: ChildChain<CN>?,
    /** Total number of children in this chain (>= 1). */
    val size: Int,
) {

    fun append(child: CN): ChildChain<CN> = ChildChain(child, this, size + 1)

    /**
     * Materialise the chain into a [List] in insertion order
     * (oldest first, most-recent last). Single allocation, O(size).
     */
    fun toList(): List<CN> {
        val out = ArrayList<CN>(size)
        var node: ChildChain<CN>? = this
        while (node != null) {
            out.add(node.head)
            node = node.prev
        }
        // We walked from newest -> oldest; reverse in place to get
        // insertion order. ArrayList.reverse is O(n) and in-place.
        out.reverse()
        return out
    }

    override fun toString(): String = toList().toString()

    companion object {
        fun <CN> of(child: CN): ChildChain<CN> = ChildChain(child, null, 1)
    }
}

class TreeDataGrowing<GN, CN : SpptDataNode>(
    val forStateSetNumber: Int
) {

    val isClean: Boolean get() = this._growingChildren.isEmpty() //&& this._numberOfGrowingParents.isEmpty()

    val isEmpty: Boolean get() = complete.isEmpty && this.isClean

    val complete = treeData(forStateSetNumber)

    /**
     * Snapshot view: materialises EVERY chain into a [List] on each call.
     * Cost is O(total children across all growing parents) per access plus
     * one [List] allocation per parent. **Do not call from a hot path** --
     * it is intended for tests and diagnostics only.
     */
    val growingChildren: Map<GN, List<CN>>
        get() = this._growingChildren.mapValues { (_, chain) -> chain?.toList() ?: emptyList() }

    fun preferred(node: CN): CN? = (this.complete.preferred(node) as CN?)

    fun initialise(gni: GN, initialSkipData: TreeData?) {
        // null chain == empty initial sequence (no children yet).
        this.setGrowingChildren(gni, null)
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
/*
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
*/
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
        // Either no entry yet, or replacing the first (and only) child --
        // both cases collapse to "store a fresh single-element chain".
        // decrementGrowingParents(growing[0])  // (legacy ref-counting -- disabled)
        this.setGrowingChildren(parent, ChildChain.of(child))
        //this.incrementGrowingParents(child)
    }

    fun setNextChildForCompleteParent(oldParent: GN, newParent: CN, nextChild: CN, isAlternative: Boolean) {
        //TODO: want to not build tree is some situations, however tree is used for resolving ambiguities!
        //old parent should normally by still growing,
        //however, when grammar is ambiguous, the oldParent could have been completed by some other option, thus no longer growing
        if (this._growingChildren.containsKey(oldParent)) {
            val chain = this._growingChildren[oldParent]
            // O(1) append; old parent's chain is unchanged (structurally shared).
            val extended = chain?.append(nextChild) ?: ChildChain.of(nextChild)
            // O(n) materialisation, once, when promoting to `complete`.
            this.complete.setChildren(newParent, extended.toList(), isAlternative)
            //extended.toList().forEach { this.incrementParents(it) }
        } else {
            //must have already been completed!
            TODO("Need to figure out what to do here ! - write suitable test")
        }
    }

    fun setNextChildForGrowingParent(oldParent: GN, newParent: GN, nextChild: CN) {
        if (Debug.CHECK) check(this._growingChildren.containsKey(oldParent)) { "no growing entry for $oldParent" }
        val chain = this._growingChildren[oldParent]
        // O(1) append; old parent's chain is unchanged (structurally shared).
        val extended = chain?.append(nextChild) ?: ChildChain.of(nextChild)
        this.setGrowingChildren(newParent, extended)
    }
/*
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
*/
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

    private val _growingChildren = hashMapOf<GN, ChildChain<CN>?>()
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
    private fun setGrowingChildren(parent: GN, children: ChildChain<CN>?) {
        if (Debug.OUTPUT_TREE_DATA) Debug.debug(Debug.IndentDelta.NONE) { "Set growing children: $parent = $children" }
        this._growingChildren[parent] = children
    }

    private fun removeGrowingNode(gni: GN): ChildChain<CN>? {
        if (Debug.OUTPUT_TREE_DATA) Debug.debug(Debug.IndentDelta.NONE) { "Remove growing parent: $gni" }
        return _growingChildren.remove(gni)
    }

}