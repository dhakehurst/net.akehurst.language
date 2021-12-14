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

internal class GrowingNode(
    val graph: ParseGraph,
    val index:GrowingNodeIndex
) {

    //FIXME: shouldn't really do this, shouldn't store these in sets!!
    private val hashCode_cache = arrayOf(this.currentState, this.startPosition).contentHashCode()

    val currentState: ParserState get() = index.state
    val runtimeLookahead: LookaheadSet get() = index.runtimeLookaheadSet
    val startPosition:Int get() = index.startPosition
    val nextInputPosition: Int get() = index.nextInputPosition
    val nextInputPositionAfterSkip: Int get() = index.nextInputPositionAfterSkip
    val numNonSkipChildren:Int get() = index.numNonSkipChildren

    //val location: InputLocation get() = children.location
    val matchedTextLength: Int = this.nextInputPosition - this.startPosition
    val runtimeRules = currentState.runtimeRulesSet
    val terminalRule = runtimeRules.first()

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
    val isLeaf: Boolean get()= this.runtimeRules.first().kind == RuntimeRuleKind.TERMINAL
    val isEmptyMatch: Boolean get() = this.currentState.isAtEnd && this.startPosition == this.nextInputPosition

    /*
    val asCompletedNodes: List<SPPTNode> by lazy {
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
*/

    fun newPrevious() {
        this.previous = mutableMapOf()
    }

    fun addPrevious(info: PreviousInfo) {
        val gn = info.node
        val gi = gn.index
        this.previous.put(gi, info)
        info.node.addNext(this)
    }

    fun addPrevious(gn: GrowingNode) {
        val info = PreviousInfo(gn)
        val gi = gn.index
        val existing = this.previous[gi]
        if (null==existing) {
            this.previous.put(gi, info)
            gn.addNext(this)
        } else {
            //merge previouses
            for (p in gn.previous) {
                existing.node.addPrevious(p.value)
            }
        }
    }

    fun addNext(value: GrowingNode) {
        this.next.add(value)
    }

    fun removeNext(value: GrowingNode) {
        this.next.remove(value)
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
