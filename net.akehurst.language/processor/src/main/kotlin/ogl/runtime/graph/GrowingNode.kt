/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.ogl.runtime.graph

import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.parser.sppt.SPPTNodeDefault

class GrowingNode(
        val runtimeRule: RuntimeRule,
        val startPosition: Int,
        val nextInputPosition: Int,
        val nextItemIndex: Int,
        val priority: Int,
        val children: List<SPPTNodeDefault>,
        val numNonSkipChildren: Int
) {

    private val hashCode_cache = intArrayOf(this.runtimeRule.number, this.startPosition, this.nextInputPosition, this.nextItemIndex).contentHashCode()

    var previous: MutableSet<PreviousInfo> = mutableSetOf()
    val next: MutableList<GrowingNode> = mutableListOf()
    val hasCompleteChildren: Boolean = this.runtimeRule.isCompleteChildren(nextItemIndex, numNonSkipChildren, children)
    val isLeaf: Boolean
        get() {
            return this.runtimeRule.isTerminal
        }
    val isBranch: Boolean
        get() {
            return this.runtimeRule.isNonTerminal
        }
    val isEmptyMatch: Boolean
        get() {
            // match empty if start and next-input positions are the same and children are complete
            return this.hasCompleteChildren && this.startPosition == this.nextInputPosition
        }
    val isSkip: Boolean
        get() {
            return this.runtimeRule.isSkip
        }
    val canGrowWidthWithSkip: Boolean
        get() {
            return !this.runtimeRule.isEmptyRule && this.isBranch
        }

    val canGrowWidth: Boolean by lazy {
        // not sure we need the test for isEmpty, because if it is empty it should be complete or NOT!???
        if (this.isLeaf or this.isEmptyMatch or hasCompleteChildren) {
            false
        } else {
            this.runtimeRule.canGrowWidth(this.nextItemIndex, this.numNonSkipChildren)
        }
    }


    val hasNextExpectedItem: Boolean
        get() {
            return this.runtimeRule.findHasNextExpectedItem(this.nextItemIndex)
        }

    val nextExpectedItems: Set<RuntimeRule> by lazy {
        this.runtimeRule.findNextExpectedItems(this.nextItemIndex, this.numNonSkipChildren)
    }

    val incrementedNextItemIndex: Int get(){
        return this.runtimeRule.incrementNextItemIndex(this.nextItemIndex)
    }

    fun expectsItemAt(runtimeRule: RuntimeRule, atPosition: Int): Boolean {
        return this.runtimeRule.couldHaveChild(runtimeRule, atPosition)
    }

    fun newPrevious() {
        this.previous = mutableSetOf()
    }

    fun addPrevious(previousNode: GrowingNode, atPosition: Int) {
        val info = PreviousInfo(previousNode, atPosition)
        this.previous.add(info)
        previousNode.addNext(this)
    }

    fun addNext(value: GrowingNode) {
        this.next.add(value)
    }

    fun removeNext(value: GrowingNode) {
        this.next.remove(value)
    }

    fun canGraftBack(info: PreviousInfo): Boolean {
        // TODO: should return false if this is emptyRule and previous' children is not empty
        // if (this.previous.isEmpty() || !this.getHasCompleteChildren()) {
        if (!this.hasCompleteChildren) {
            return false
        }
        var b = false
        // for (final PreviousInfo info : previous) {
        val x = this.isEmptyMatch && (info.node.runtimeRule.rhs.kind == RuntimeRuleItemKind.MULTI || info.node.runtimeRule.rhs.kind == RuntimeRuleItemKind.SEPARATED_LIST) && info.node.numNonSkipChildren != 0

        b = b or (info.node.expectsItemAt(this.runtimeRule, info.atPosition) && !x)
        // }
        return b// && this.getHasCompleteChildren();// && this.getIsStacked();
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.hashCode_cache
    }

    override fun equals(obj: Any?): Boolean {
        // assume obj is also a GrowingNode, should never be compared otherwise
        val other = obj as GrowingNode?
        if (null == other) {
            return false
        } else {
            return this.runtimeRule.number == other.runtimeRule.number
                    && this.startPosition == other.startPosition
                    && this.nextInputPosition == other.nextInputPosition
                    && this.nextItemIndex == other.nextItemIndex
        }
    }

    //TODO: toString
}