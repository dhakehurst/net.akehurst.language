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

import net.akehurst.language.agl.automaton.LookaheadSet
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.sppt.SPPTBranchFromTreeData
import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.agl.sppt.ToStringVisitor

internal class GrowingNode(
    val graph: ParseGraph,
    val index: GrowingNodeIndex
) {

    //FIXME: shouldn't really do this, shouldn't store these in sets!!
    private val hashCode_cache = arrayOf(this.currentState, this.startPosition).contentHashCode()

    val runtimeState:RuntimeState get() = index.runtimeState
    val currentState: ParserState get() = index.runtimeState.state
    val runtimeLookahead: Set<LookaheadSet> get() = index.runtimeState.runtimeLookaheadSet
    val startPosition: Int get() = index.startPosition
    val nextInputPosition: Int get() = index.nextInputPosition
    val nextInputPositionAfterSkip: Int get() = index.nextInputPositionAfterSkip

    val matchedTextLength: Int = this.nextInputPosition - this.startPosition
    val runtimeRules = currentState.runtimeRulesSet

    var previous: MutableMap<GrowingNodeIndex, PreviousInfo> = mutableMapOf()
    val next: MutableSet<GrowingNode> = mutableSetOf() //TODO: do we actually need this?
    val isLeaf: Boolean get() = this.runtimeRules.first().kind == RuntimeRuleKind.TERMINAL
    val isEmptyMatch: Boolean get() = this.currentState.isAtEnd && this.startPosition == this.nextInputPosition

    fun addNext(value: GrowingNode) {
        this.next.add(value)
    }
/*
    private fun toStringNode(withPrevious: Boolean): String {
        var r = "$currentState,$startPosition,$nextInputPosition,"
        r += if (this.currentState.isAtEnd) "C" else this.currentState.rulePositions.first().position
        val ct = this.runtimeLookahead.map {
            val cont = mutableSetOf<RuntimeRule>()
            if (it.includesRT) cont += RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD
            if (it.includesEOT) cont += RuntimeRuleSet.END_OF_TEXT
            if (it.matchANY) cont += RuntimeRuleSet.ANY_LOOKAHEAD
            cont += it.content
            cont
        }
        r += ct.joinToString(prefix = "[", postfix = "]", separator = ",") { it.joinToString(separator = "|") { it.tag } }
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
                s = " --> " + prev.toStringNode(false) + prev.toStringPrevious(visited)
            } else {
                val sz = this.previous.size
                s = " -" + sz + "> " + prev.toStringNode(false) + prev.toStringPrevious(visited)
            }

        }
        return s
    }
*/
    //useful during debug
    fun toStringTree(): String {
        val nodes = when {
            this.isLeaf -> this.runtimeRules.map { rr ->
                SPPTLeafFromInput(this.graph.input, rr, this.startPosition, this.nextInputPosition, 0)
            }
            else -> this.runtimeRules.map { rr ->
                SPPTBranchFromTreeData(this.graph.treeData, this.graph.input, rr, 0, this.startPosition, this.nextInputPosition, 0)
            }
        }
        val v = ToStringVisitor("\n", "  ")
        return nodes.joinToString(separator = "\n") { n -> v.visitNode(n, "  ").joinToString(separator = "\n") }
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
        return this.index.toString()
        //return this.toStringNode(true)
    }

}
