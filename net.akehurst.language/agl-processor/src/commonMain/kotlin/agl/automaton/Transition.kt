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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleListKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind

internal typealias RuntimeGuard = Transition.(GrowingNodeIndex, ParserState?) -> Boolean

internal class Transition(
    val from: ParserState,
    val to: ParserState,
    val action: ParseAction,
    val lookahead: Set<Lookahead>,
    val graftPrevGuard: Set<RulePosition>?,
    val runtimeGuard: RuntimeGuard
) {

    companion object {
        val multiRuntimeGuard: Transition.(GrowingNodeIndex) -> Boolean = { gn: GrowingNodeIndex ->
            val previousRp = gn.runtimeState.state.rulePositions.first() //FIXME: first rule may not be correct
            val runtimeRule = gn.runtimeState.state.firstRule //FIXME:
            when {
                previousRp.isAtEnd -> gn.numNonSkipChildren + 1 >= runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.POSITION_MULIT_ITEM -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = gn.numNonSkipChildren + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || gn.numNonSkipChildren + 1 <= runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || gn.numNonSkipChildren + 1 <= runtimeRule.rhs.multiMax
                    }

                }
                else -> true
            }
        }
        val sListRuntimeGuard: Transition.(GrowingNodeIndex) -> Boolean = { gn: GrowingNodeIndex ->
            val previousRp = gn.runtimeState.state.rulePositions.first() //FIXME: first rule may not be correct
            val runtimeRule = gn.runtimeState.state.firstRule //FIXME:
            when {
                previousRp.isAtEnd -> (gn.numNonSkipChildren / 2) + 1 >= runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.POSITION_SLIST_ITEM -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = (gn.numNonSkipChildren / 2) + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 <= runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 <= runtimeRule.rhs.multiMax
                    }
                }
                previousRp.position == RulePosition.POSITION_SLIST_SEPARATOR -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = (gn.numNonSkipChildren / 2) + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 < runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 < runtimeRule.rhs.multiMax
                    }
                }
                else -> true
            }
        }

        val graftRuntimeGuard: RuntimeGuard = { gn, previous ->
            if (null == previous) {
                true
            } else {
                val rr = previous.firstRule //FIXME: possibly more than one!!
                when (rr.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.LIST -> when (rr.rhs.listKind) {
                        RuntimeRuleListKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                        RuntimeRuleListKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                        else -> TODO()
                    }
                    else -> true
                }
            }
        }
        val defaultRuntimeGuard: RuntimeGuard = { _, _ -> true }
        fun runtimeGuardFor(action: Transition.ParseAction): RuntimeGuard = when (action) {
            Transition.ParseAction.GRAFT -> graftRuntimeGuard
            else -> defaultRuntimeGuard
        }
    }

    internal enum class ParseAction {
        HEIGHT, // reduce first
        GRAFT,  // reduce other
        WIDTH,  // shift
        GOAL,    // goal
        EMBED,
//        GRAFT_OR_HEIGHT // try graft if fails do height -- reduces ambiguity on recursive rules
    }

    val context by lazy {
        this.from.outTransitions.previousFor(this).toSet()
    }

    private val hashCode_cache: Int by lazy {
        arrayListOf(from, to, action, lookahead, graftPrevGuard).hashCode()
    }

    override fun hashCode(): Int = this.hashCode_cache

    override fun equals(other: Any?): Boolean {
        when (other) {
            is Transition -> {
                if (this.from != other.from) return false
                if (this.to != other.to) return false
                if (this.action != other.action) return false
                if (this.lookahead != other.lookahead) return false
                if (this.graftPrevGuard != other.graftPrevGuard) return false
                return true
            }
            else -> return false
        }
    }

    override fun toString(): String {
        val ctx = this.context.joinToString { it.rulePositions.toString() }
        val lhsStr = this.lookahead.joinToString(separator = "|") { "[${it.guard.fullContent.joinToString {  it.tag }}](${it.up.fullContent.joinToString {  it.tag }})" }
        return "Transition[$ctx] { $from -- $action${lhsStr} --> $to }${graftPrevGuard?:"[]"}"
    }
}