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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.structure.RulePosition

internal class Transition(
    val from: ParserState,
    val to: ParserState,
    val action: ParseAction,
    val lookahead: Set<Lookahead>,
    val prevGuard: List<RulePosition>?,
    val runtimeGuard: Transition.(current: GrowingNodeIndex, previous: List<RulePosition>?) -> Boolean
) {

    internal enum class ParseAction {
        HEIGHT, // reduce first
        GRAFT,  // reduce other
        WIDTH,  // shift
        GOAL,    // goal
        EMBED,
//        GRAFT_OR_HEIGHT // try graft if fails do height -- reduces ambiguity on recursive rules
    }

    private val hashCode_cache: Int by lazy {
        arrayListOf(from, to, action, lookahead, prevGuard).hashCode()
    }

    override fun hashCode(): Int = this.hashCode_cache

    override fun equals(other: Any?): Boolean {
        when (other) {
            is Transition -> {
                if (this.from != other.from) return false
                if (this.to != other.to) return false
                if (this.action != other.action) return false
                if (this.lookahead != other.lookahead) return false
                if (this.prevGuard != other.prevGuard) return false
                return true
            }
            else -> return false
        }
    }

    override fun toString(): String {
        val lhsStr = this.lookahead.joinToString(separator = "|") { "[${it.guard.fullContent.joinToString {  it.tag }}](${it.up.fullContent.joinToString {  it.tag }})" }
        return "Transition { $from -- $action${lhsStr} --> $to }"
    }
}