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

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.sppt.SPPTBranchFromInputAndGrownChildren
import net.akehurst.language.api.sppt.SPPTNode

internal class GrowingNode(
    val graph: ParseGraph,
    val currentState: ParserState,
    val runtimeLookahead: LookaheadSet,
    val children: GrowingChildren
) {
    companion object {
        fun indexForGrowingNode(gn:GrowingNode) = indexFromGrowingChildren(gn.currentState, gn.runtimeLookahead, gn.children)

        fun indexFromGrowingChildren(state: ParserState, lhs: LookaheadSet, growingChildren: GrowingChildren): GrowingNodeIndex {
            val listSize = listSize(state.runtimeRules.first(), growingChildren.numberNonSkip)
            return GrowingNodeIndex(state.runtimeRules,state.positions, lhs.number, growingChildren.startPosition, growingChildren.nextInputPosition, listSize)
        }

        // used for start and leaf
        fun index(state: ParserState, lhs: LookaheadSet, startPosition: Int, nextInputPosition: Int, listSize: Int): GrowingNodeIndex {
            return GrowingNodeIndex(state.runtimeRules,state.positions, lhs.number, startPosition, nextInputPosition, listSize)
        }

        // used to augment the GrowingNodeIndex (GSS node identity) for MULTI and SEPARATED_LIST
        // needed because the 'RulePosition' does not capture the 'position' in the list
        fun listSize(runtimeRule: RuntimeRule, childrenSize: Int): Int = when (runtimeRule.kind) {
            RuntimeRuleKind.NON_TERMINAL -> when (runtimeRule.rhs.itemsKind) {
                RuntimeRuleRhsItemsKind.EMPTY -> -1
                RuntimeRuleRhsItemsKind.CONCATENATION -> -1
                RuntimeRuleRhsItemsKind.CHOICE -> -1
                RuntimeRuleRhsItemsKind.LIST -> when (runtimeRule.rhs.listKind) {
                    RuntimeRuleListKind.MULTI -> childrenSize
                    RuntimeRuleListKind.SEPARATED_LIST -> childrenSize
                    else -> TODO()
                }
            }
            else -> -1
        }
    }

    //FIXME: shouldn't really do this, shouldn't store these in sets!!
    private val hashCode_cache = arrayOf(this.currentState, this.startPosition).contentHashCode()

    val nextInputPosition: Int get() = children.nextInputPosition
    val numNonSkipChildren: Int get() = children.numberNonSkip

    //val location: InputLocation get() = children.location
    val startPosition get() = children.startPosition
    val matchedTextLength: Int = this.nextInputPosition - this.startPosition
    val runtimeRules = currentState.runtimeRulesSet
    val terminalRule = runtimeRules.first()

    var skipNodes: List<SPPTNode>? = null
/*
val lastLocation
    get() = when (this.runtimeRules.first().kind) {
        RuntimeRuleKind.TERMINAL -> if (this.skipNodes.isEmpty()) this.location else this.skipNodes.last().location
        RuntimeRuleKind.GOAL,
        RuntimeRuleKind.NON_TERMINAL -> if (children.isEmpty()) this.location else children.last().location
        RuntimeRuleKind.EMBEDDED -> if (children.isEmpty()) this.location else children.last().location
    }
*/

    var previous: MutableMap<GrowingNodeIndex, PreviousInfo> = mutableMapOf()
    val next: MutableSet<GrowingNode> = mutableSetOf() //TODO: do we actually need this?
    val isLeaf: Boolean
        get() {
            return this.runtimeRules.first().kind == RuntimeRuleKind.TERMINAL
        }
    val isEmptyMatch: Boolean
        get() {
            // match empty if start and next-input positions are the same and children are complete
            return this.currentState.isAtEnd && this.startPosition == this.nextInputPosition
        }

    private var _asCompletedNodes: MutableList<SPPTNode>? = null
    val asCompletedNodes1: List<SPPTNode> get() {
         if (null==_asCompletedNodes && this.isLeaf) {
             this._asCompletedNodes = mutableListOf(
                 this.graph.input.leaves[this.runtimeRules.first(), startPosition] ?: error("Internal Error: leaf node not found!")
             )
        }
        return _asCompletedNodes ?: error("Internal Error: completedNodes not set")
    }

    val asCompletedNodes2: List<SPPTNode> by lazy {
        if (this.isLeaf) {
            listOf(
                this.graph.input.leaves[this.runtimeRules.first(), startPosition] ?: error("Internal Error: leaf node not found!")
            )
        } else {
                this.currentState.rulePositions.mapNotNull {
                    if (it.isAtEnd) {
                        val input = this.graph.input
                        val rr = it.runtimeRule
                        val option = it.option
                        val priority = it.priority
                        val cn = SPPTBranchFromInputAndGrownChildren(input, rr, option, startPosition, nextInputPosition, priority)
                        cn.grownChildrenAlternatives[option] = this.children
                        cn
                    } else {
                        null
                    }
                }
        }
    }

    val asCompletedNodes: List<SPPTNode> get() = this.asCompletedNodes2

    fun newPrevious() {
        this.previous = mutableMapOf()
    }

    fun addPrevious(info: PreviousInfo) {
        val gn = info.node
        val gi = GrowingNode.indexFromGrowingChildren(gn.currentState, gn.runtimeLookahead, gn.children)
        this.previous.put(gi, info)
        info.node.addNext(this)
    }

    fun addPrevious(previousNode: GrowingNode) {
        val info = PreviousInfo(previousNode)
        val gi = GrowingNode.indexFromGrowingChildren(previousNode.currentState, previousNode.runtimeLookahead, previousNode.children)
        this.previous.put(gi, info)
        previousNode.addNext(this)
    }

    fun addNext(value: GrowingNode) {
        this.next.add(value)
    }

    fun removeNext(value: GrowingNode) {
        this.next.remove(value)
    }

    fun addCompleted(runtimeRule: RuntimeRule, completedNode:SPPTNode) {
        if (null==this._asCompletedNodes) this._asCompletedNodes = mutableListOf()
        this._asCompletedNodes?.add(completedNode)
    }

    fun toStringTree(withChildren: Boolean, withPrevious: Boolean): String {
        var r = "$currentState,$startPosition,$nextInputPosition,"
        r += if (this.currentState.isAtEnd) "C" else this.currentState.rulePositions.first().position
        r+= this.runtimeLookahead.content.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }
        //val name = this.currentState.runtimeRules.joinToString(prefix = "[", separator = ",", postfix = "]") { "${it.tag}(${it.number})" }
        //r += ":" + name
/*
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
*/
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
