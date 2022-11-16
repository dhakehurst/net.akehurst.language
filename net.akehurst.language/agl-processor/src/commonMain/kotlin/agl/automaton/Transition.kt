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

import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.api.runtime.RulePosition
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.runtime.structure.RuntimeRuleListKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsList

//internal typealias RuntimeGuard = Transition.(GrowingNodeIndex, ParserState?) -> Boolean

internal interface RuntimeGuard {
    fun invoke(numNonSkipChildren: Int): Boolean
}

internal class Transition(
    val from: ParserState,
    val to: ParserState,
    val action: ParseAction,
    val lookahead: Set<Lookahead>
) {

    companion object {

        class ToEndMultiGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.to.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun invoke(numNonSkipChildren: Int): Boolean = this.min <= numNonSkipChildren + 1 && (-1 == max || numNonSkipChildren + 1 <= max)

            override fun toString(): String = "ToEndMulti{$min <= n}"
        }

        class ToItemMultiGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.to.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun invoke(numNonSkipChildren: Int): Boolean = -1 == max || numNonSkipChildren + 1 < max

            override fun toString(): String = "ToItemMulti{n <= $max}"
        }

        class ToEndSListGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.to.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun invoke(numNonSkipChildren: Int): Boolean = this.min <= (numNonSkipChildren / 2) + 1 && (-1 == max || (numNonSkipChildren / 2) + 1 <= max)

            override fun toString(): String = "ToEndSList{$min <= n}"
        }

        class ToItemSListGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.to.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun invoke(numNonSkipChildren: Int): Boolean = -1 == max || (numNonSkipChildren / 2) + 1 < max

            override fun toString(): String = "ToItemSList{n <= $max}"
        }

        class ToSeparatorSListGraftRuntimeGuard(val trans: Transition) : RuntimeGuard {
            val rhs = trans.to.firstRule.rhs as RuntimeRuleRhsList
            val min = rhs.min
            val max = rhs.max

            override fun invoke(numNonSkipChildren: Int): Boolean = -1 == max || (numNonSkipChildren / 2) + 1 < max

            override fun toString(): String = "ToSeparatorSList{n <= $max}"
        }

        object DefaultRuntimeGuard : RuntimeGuard {
            override fun invoke(numNonSkipChildren: Int): Boolean = true
            override fun toString(): String = "DefaultRuntimeGuard{true}"
        }

        fun runtimeGuardFor(trans: Transition): RuntimeGuard {
            val target = trans.to.firstRule
            val targetRp = trans.to.rulePositions[0]
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

                    else -> TODO()
                }

                else -> DefaultRuntimeGuard
            }
        }
    }

    val context by lazy {
        this.from.outTransitions.previousFor(this).toSet()
    }

    val runtimeGuard: RuntimeGuard = runtimeGuardFor(this)

    private val hashCode_cache: Int by lazy {
        arrayListOf(from, to, action, lookahead).hashCode()
    }

    override fun hashCode(): Int = this.hashCode_cache

    override fun equals(other: Any?): Boolean {
        when (other) {
            is Transition -> {
                if (this.from != other.from) return false
                if (this.to != other.to) return false
                if (this.action != other.action) return false
                if (this.lookahead != other.lookahead) return false
//                if (this.graftPrevGuard != other.graftPrevGuard) return false
                return true
            }

            else -> return false
        }
    }

    override fun toString(): String {
        val ctx = this.context.joinToString { it.rulePositions.toString() }
        val lhsStr = this.lookahead.joinToString(separator = "|") { "[${it.guard.fullContent.joinToString { it.tag }}](${it.up.fullContent.joinToString { it.tag }})" }
        return "Transition[$ctx] { $from -- $action${lhsStr} --> $to }"
        //return "Transition[$ctx] { $from -- $action${lhsStr} --> $to }${graftPrevGuard?:"[]"}"
    }
}