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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SPPTNode

class GrowingNode(
        val isSkipGrowth : Boolean,
        val currentState : ParserState, // current rp of this node, it is growing, this changes (for new node) when children are added
        val location: InputLocation,
        val nextInputPosition: Int,
        val priority: Int,
        val children: List<SPPTNode>,
        val numNonSkipChildren: Int
) {
    //FIXME: shouldn't really do this, shouldn't store these in sets!!
    private val hashCode_cache = arrayOf(this.currentState, this.location.position).contentHashCode()

    val startPosition = location.position
    val matchedTextLength:Int = this.nextInputPosition - this.startPosition
    val runtimeRule get() = currentState.runtimeRule
    var skipNodes = mutableListOf<SPPTNode>()
    val lastLocation get() = if(this.runtimeRule.isTerminal) {
        if (this.skipNodes.isEmpty()) this.location else this.skipNodes.last().location
    } else {
        if (children.isEmpty()) this.location else children.last().location
    }

    var previous: MutableMap<GrowingNodeIndex, PreviousInfo> = mutableMapOf()
    val next: MutableList<GrowingNode> = mutableListOf()
    val hasCompleteChildren: Boolean = this.currentState.isAtEnd //this.runtimeRule.isCompleteChildren(currentState.position, numNonSkipChildren, children)
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
            return this.currentState.isAtEnd && this.startPosition == this.nextInputPosition
        }

    val canGrowWidth: Boolean by lazy {
        // not sure we need the test for isEmpty, because if it is empty it should be complete or NOT!???
        if (this.isLeaf or this.isEmptyMatch ) {//or this.hasCompleteChildren) {
            false
        } else {
            this.runtimeRule.canGrowWidth(this.currentState.position)
        }
    }


    val hasNextExpectedItem: Boolean
        get() {
            return this.runtimeRule.findHasNextExpectedItem(this.currentState.position)
        }

    val nextExpectedItems: Set<RuntimeRule> by lazy {
        this.runtimeRule.findNextExpectedItems(this.currentState.position)
    }

    val incrementedNextItemIndex: Int get(){
        return this.runtimeRule.incrementNextItemIndex(this.currentState.position)
    }


    fun expectsItemAt(runtimeRule: RuntimeRule, atPosition: Int): Boolean {
        return this.runtimeRule.couldHaveChild(runtimeRule, atPosition)
    }

    fun newPrevious() {
        this.previous = mutableMapOf()
    }
    fun addPrevious(info: PreviousInfo) {
        val gi = GrowingNodeIndex(info.node.currentState, info.node.startPosition, info.node.nextInputPosition)
        this.previous.put(gi,info)
        info.node.addNext(this)
    }
    fun addPrevious(previousNode: GrowingNode) {
        val info = PreviousInfo(previousNode)
        val gi = GrowingNodeIndex(previousNode.currentState, previousNode.startPosition, previousNode.nextInputPosition)
        this.previous.put(gi,info)
        previousNode.addNext(this)
    }

    fun addNext(value: GrowingNode) {
        this.next.add(value)
    }

    fun removeNext(value: GrowingNode) {
        this.next.remove(value)
    }

    fun toStringTree(withChildren: Boolean, withPrevious: Boolean): String {
        var r = "$currentState,$startPosition,$nextInputPosition,"
        r += if (this.hasCompleteChildren) "C" else this.currentState.position
        val name = if (this.runtimeRule.isTerminal) "'${this.runtimeRule.patternText}'" else this.runtimeRule.name
        r += ":" + name + "(" + this.runtimeRule.number + ")"

        if (withChildren) {
            if (this.isLeaf) {
                // no children
            } else {
                r += "{"
                for (c in this.children) {
    //TODO:                r += c.accept(ParseTreeToSingleLineTreeString(), null)
                }
                if (this.hasCompleteChildren) {
                    r += "}"
                } else {
                    r += "..."
                }
            }
        }

        if (withPrevious) {
            val visited = HashSet<GrowingNode>()
            r += this.toStringPrevious(visited)
        }

        return r
    }

    private fun toStringPrevious(visited: MutableSet<GrowingNode>): String {
        visited.add(this)
        var s = ""
        if (this.previous.isEmpty()) {
            //
        } else {
            val prev = this.previous.values.iterator().next().node
            if (visited.contains(prev)) {
                s = "--> ..."
            } else if (this.previous.size == 1) {
                s = " --> " + prev.toStringTree(false, false) + prev.toStringPrevious(visited)
            } else {
                val sz = this.previous.size
                s = " -" + sz + "> " + prev.toStringTree(false, false) + prev.toStringPrevious(visited)
            }

        }
        return s
    }

    // --- Any ---

    override fun hashCode(): Int {
        return this.hashCode_cache
    }

    override fun equals(other: Any?): Boolean {
        // assume obj is also a GrowingNode, should never be compared otherwise
        if (other is GrowingNode) {
            return this.currentState == other.currentState
                    && this.startPosition == other.startPosition
                    && this.nextInputPosition == other.nextInputPosition
        } else {
            return false
        }
    }

    override fun toString(): String {
        return this.toStringTree(false, true)
    }
}
