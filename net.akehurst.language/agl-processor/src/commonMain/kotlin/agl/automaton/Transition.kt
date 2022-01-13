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
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

internal class Transition(
        val from: ParserState,
        val to: ParserState,
        val action: ParseAction,
        val lookaheadGuard: LookaheadSet,
        val upLookahead: LookaheadSet,
        val prevGuard: List<RulePosition>?,
        val runtimeGuard: Transition.(current:GrowingNodeIndex, previous:List<RulePosition>?)->Boolean
) {

    internal  enum class ParseAction {
        HEIGHT, // reduce first
        GRAFT,  // reduce other
        WIDTH,  // shift
        GOAL,    // goal
        EMBED,
//        GRAFT_OR_HEIGHT // try graft if fails do height -- reduces ambiguity on recursive rules
    }

    private val hashCode_cache:Int by lazy {
        arrayListOf(from, to, action, lookaheadGuard,upLookahead, prevGuard).hashCode()
    }


    override fun hashCode(): Int {
        return this.hashCode_cache
    }

    override fun equals(other: Any?): Boolean {
        when(other) {
            is Transition -> {
                if (this.from!=other.from) return false
                if (this.to!=other.to) return false
                if (this.action!=other.action) return false
                if (this.lookaheadGuard!=other.lookaheadGuard) return false
                if (this.upLookahead!=other.upLookahead) return false
                if (this.prevGuard!=other.prevGuard) return false
                return true
            }
            else -> return false
        }
    }

    override fun toString(): String {
        //val lh = " "+this.lookaheadGuard.number.toString()+":"+this.lookaheadGuard.content.map { it.tag }
        val cont1 = mutableSetOf<RuntimeRule>()
        if (lookaheadGuard.includesUP) cont1.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        if (lookaheadGuard.includesEOT) cont1.add(RuntimeRuleSet.END_OF_TEXT)
        if (lookaheadGuard.matchANY) cont1.add(RuntimeRuleSet.ANY_LOOKAHEAD)
        cont1.addAll(lookaheadGuard.content)
        val cont2 = mutableSetOf<RuntimeRule>()
        if (upLookahead.includesUP) cont2.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        if (upLookahead.includesEOT) cont2.add(RuntimeRuleSet.END_OF_TEXT)
        if (upLookahead.matchANY) cont2.add(RuntimeRuleSet.ANY_LOOKAHEAD)
        cont2.addAll(upLookahead.content)
        val lh = cont1.joinToString(separator = ",") {it.tag}
        val ulh = cont2.joinToString(separator = ","){it.tag}
        return "Transition { $from -- $action [$lh|$ulh] --> $to }"
    }
}