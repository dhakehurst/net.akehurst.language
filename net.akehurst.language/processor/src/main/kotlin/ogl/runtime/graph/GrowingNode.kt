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

import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleKind

class GrowingNode(
        val runtimeRule: RuntimeRule,
        val startPosition: Int,
        val nextInputPosition: Int,
        val nextItemIndex: Int,
        val priority: Int,
        val children: List<SPPTNode>,
        val numNonSkipChildren: Int
) {

    private val hashCode_cache = intArrayOf(this.runtimeRule.number, this.startPosition, this.nextInputPosition, this.nextItemIndex).contentHashCode()

    val previous: MutableList<PreviousInfo> = mutableListOf()
    val next: MutableList<GrowingNode> = mutableListOf()
    val hasCompleteChildren : Boolean = this.runtimeRule.isCompleteChildren(nextItemIndex, numNonSkipChildren, children)
    val isLeaf : Boolean get() { return this.runtimeRule.kind == RuntimeRuleKind.TERMINAL } //TODO: cache or calculate? does it matter?
    val isEmptyRuleMatch: Boolean get() {
        // children must be complete or we would not have created the node
        // therefore must match empty if start and next-input positions are the same
        return this.startPosition == this.nextInputPosition
    }

    fun addPrevious(previousNode: GrowingNode, atPosition: Int) {
        val info = PreviousInfo(previousNode, atPosition)
        this.previous.add(info)
        previousNode.addNext(this)
    }

    fun addNext(value: GrowingNode) {
        this.next.add(value)
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