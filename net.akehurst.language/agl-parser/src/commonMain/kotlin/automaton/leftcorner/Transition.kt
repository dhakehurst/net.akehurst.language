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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsList
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSeparated
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsListSimple
import net.akehurst.language.automaton.api.AutomatonState
import net.akehurst.language.automaton.api.AutomatonTransition
import net.akehurst.language.automaton.api.LookaheadGuard
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.automaton.api.TransitionContext
import net.akehurst.language.parser.api.RulePosition

//internal typealias RuntimeGuard = Transition.(GrowingNodeIndex, ParserState?) -> Boolean

interface RuntimeGuard {
    fun execute(numNonSkipChildren: Int): Boolean
    fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule>
}

class Transition(
    override val source: ParserState,
    override val target: ParserState,
    override val action: ParseAction,
    val lookaheadGuard: Set<Lookahead>
) : AutomatonTransition {

    companion object {

        class ToEndMultiGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.target.firstRule.rhs as RuntimeRuleRhsListSimple
            val min = rhs.min
            val max = rhs.max

            override fun execute(numNonSkipChildren: Int): Boolean = this.min <= numNonSkipChildren + 1 && (-1 == max || numNonSkipChildren + 1 <= max)

            override fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule> = when {
                this.min > numNonSkipChildren -> setOf(rhs.repeatedRhsItem)
                (-1 != max && max < numNonSkipChildren + 1) -> trans.lookaheadGuard.map { it.guardLookaheadSet }.fold(LookaheadSetPart.EMPTY) { acc, it -> acc.union(it.part) }.fullContent
                else -> emptySet()
            }

            override fun toString(): String = "ToEndMulti{$min <= n}"
        }

        class ToItemMultiGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.target.firstRule.rhs as RuntimeRuleRhsListSimple
            val min = rhs.min
            val max = rhs.max

            override fun execute(numNonSkipChildren: Int): Boolean = -1 == max || numNonSkipChildren + 1 < max
            override fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule> = when {
                (-1 == max || numNonSkipChildren + 1 < max) -> emptySet()
                else -> emptySet()
            }


            override fun toString(): String = "ToItemMulti{n <= $max}"
        }

        class ToEndSListGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.target.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun execute(numNonSkipChildren: Int): Boolean = this.min <= (numNonSkipChildren / 2) + 1 && (-1 == max || (numNonSkipChildren / 2) + 1 <= max)

            override fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule> = when {
                this.min <= (numNonSkipChildren / 2) + 1 -> emptySet()
                (-1 == max || (numNonSkipChildren / 2) + 1 <= max) -> emptySet()
                else -> emptySet()
            }

            override fun toString(): String = "ToEndSList{$min <= n}"
        }

        class ToItemSListGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.target.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun execute(numNonSkipChildren: Int): Boolean = -1 == max || (numNonSkipChildren / 2) + 1 < max

            override fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule> = when {
                -1 == max || (numNonSkipChildren / 2) + 1 < max -> emptySet()
                else -> emptySet()
            }


            override fun toString(): String = "ToItemSList{n <= $max}"
        }

        class ToSeparatorSListGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.target.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun execute(numNonSkipChildren: Int): Boolean = -1 == max || (numNonSkipChildren / 2) + 1 < max

            override fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule> = when {
                -1 == max || (numNonSkipChildren / 2) + 1 < max -> emptySet()
                else -> emptySet()
            }

            override fun toString(): String = "ToSeparatorSList{n <= $max}"
        }

        object DefaultRuntimeGuard : RuntimeGuard {
            override fun execute(numNonSkipChildren: Int): Boolean = true
            override fun expectedWhenFailed(numNonSkipChildren: Int): Set<RuntimeRule> = error("Internal Error: should never happen")
            override fun toString(): String = "DefaultRuntimeGuard{true}"
        }

        fun runtimeGuardFor(trans: Transition): RuntimeGuard {
            val target = trans.target.firstRule
            val targetRp = trans.target.rulePosition[0]
            val tgtRhs = target.rhs
            return when (trans.action) {
                ParseAction.GRAFT -> when (tgtRhs) {
                    is RuntimeRuleRhsListSimple -> when {
                        targetRp.isAtEnd -> ToEndMultiGraftRuntimeGuard(trans)
                        targetRp.position == RulePosition.POSITION_MULIT_ITEM -> ToItemMultiGraftRuntimeGuard(trans)
                        else -> TODO()
                    }

                    is RuntimeRuleRhsListSeparated -> when {
                        targetRp.isAtEnd -> ToEndSListGraftRuntimeGuard(trans)
                        targetRp.position == RulePosition.POSITION_SLIST_ITEM -> ToItemSListGraftRuntimeGuard(trans)
                        targetRp.position == RulePosition.POSITION_SLIST_SEPARATOR -> ToSeparatorSListGraftRuntimeGuard(trans)
                        else -> TODO()
                    }

                    else -> DefaultRuntimeGuard
                }

                else -> DefaultRuntimeGuard
            }
        }
    }

    val context by lazy {
        this.source.outTransitions.previousFor(this).toSet()
    }

    val runtimeGuard: RuntimeGuard = runtimeGuardFor(this)

    private val hashCode_cache: Int by lazy {
        arrayListOf(source, target, action, lookaheadGuard).hashCode()
    }

    // --- AutomatonTransition ---
    override val lookahead: Set<LookaheadGuard> get() = lookaheadGuard

    override val prev: Set<AutomatonState> get() = context.map { it.previous }.toSet()

    override val prevPrev: Set<AutomatonState> get() = context.filterIsInstance<CompleteKey>().map { it.prevPrev }.toSet()

    /**
     * Atomic (prevPrev, prev) pairs derived directly from the underlying [CompleteKey]s.
     * Returns the empty set for incomplete transitions (WIDTH/EMBED) — see API kdoc.
     */
    override val transContext: Set<TransitionContext> get() = context.filterIsInstance<CompleteKey>().toSet()

    // --- Any ---
    override fun hashCode(): Int = this.hashCode_cache

    override fun equals(other: Any?): Boolean {
        when (other) {
            is Transition -> {
                if (this.source != other.source) return false
                if (this.target != other.target) return false
                if (this.action != other.action) return false
                if (this.lookaheadGuard != other.lookaheadGuard) return false
//                if (this.graftPrevGuard != other.graftPrevGuard) return false
                return true
            }

            else -> return false
        }
    }

    override fun toString(): String {
        val ctx = this.context.joinToString { it.toString() }
        val lhsStr = this.lookaheadGuard.joinToString(separator = "|") { "[${it.guardLookaheadSet.fullContent.joinToString { it.tag }}](${it.upLookaheadSet.fullContent.joinToString { it.tag }})" }
        return "Transition: $source -- $action${lhsStr} --> $target {$ctx}"
        //return "Transition[$ctx] { $from -- $action${lhsStr} --> $to }${graftPrevGuard?:"[]"}"
    }
}