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

package net.akehurst.language.agl.language.grammar.asm

import net.akehurst.language.api.language.grammar.*

data class OverrideRuleDefault(
    override val grammar: Grammar,
    override val name: GrammarRuleName,
    override val isSkip: Boolean,
    override val isLeaf: Boolean,
    override val overrideKind: OverrideKind
) : GrammarRuleAbstract(), OverrideRule {

    override val isOverride: Boolean = true

    private var _overridenRhs: RuleItem? = null
    override var overridenRhs: RuleItem
        get() {
            return this._overridenRhs ?: throw GrammarExeception("overridenRhs of rule must be set", null)
        }
        set(value) {
            value.setOwningRule(this, listOf(0))
            this._overridenRhs = value
        }

    override val rhs: RuleItem
        get() = when (overrideKind) {
            OverrideKind.REPLACE -> overridenRhs
            OverrideKind.SUBSTITUTION -> {
                when (overridenRhs) {
                    is NonTerminal -> {
                        val tg = (overridenRhs as NonTerminal).targetGrammar?.resolved
                        when {
                            tg == null -> error("Override rule using substitution must contain a \"qualified\" non-terminal, there is no reference to an extended grammar.")
                            this.grammar.allExtendsResolved.any { eg -> tg == eg } -> {
                                val or = tg.findAllResolvedGrammarRule(this.name)
                                when {
                                    null == or -> error("Cannot find rule '${(overridenRhs as NonTerminal).ruleReference}' in grammar '${tg.name}' for override substitution.")
                                    else -> {
                                        or.rhs
                                    }
                                }
                            }

                            else -> error("Grammar '${tg.name}' is not extended by this grammar, cannot substitute a rule if it is not inherited.")
                        }
                    }

                    else -> {
                        error("Override rule using inherited rule must contain a single qualified non-terminal.")
                    }
                }
            }

            OverrideKind.APPEND_ALTERNATIVE -> {
                val or = this.grammar.findAllSuperGrammarRule(this.name).firstOrNull()
                when {
                    null == or -> {
                        error("Rule ${this.name} is marked as override, but there is no super rule with that name to override.")
                    }

                    else -> when (or.rhs) {
                        is ChoiceLongest -> {
                            val appendedAlternatives = (or.rhs as ChoiceLongest).alternative + overridenRhs
                            val ac = ChoiceLongestDefault(appendedAlternatives)
                            val indices = (or.rhs as ChoiceLongestDefault).index!!
                            val ni = indices.dropLast(1) + indices.last() + 1
                            ac.setOwningRule(this, ni)
                            ac
                        }

                        is NonTerminal -> {
                            val appendedAlternatives = listOf(or.rhs, overridenRhs)
                            val ac = ChoiceLongestDefault(appendedAlternatives)
                            val ni = listOf(0, 1)
                            ac.setOwningRule(this, ni)
                            ac
                        }

                        is Terminal -> {
                            val appendedAlternatives = listOf(or.rhs, overridenRhs)
                            val ac = ChoiceLongestDefault(appendedAlternatives)
                            val ni = listOf(0, 1)
                            ac.setOwningRule(this, ni)
                            ac
                        }

                        else -> error("Cannot append choice overriden rule is not a choice or single NonTerminal")
                    }
                }
            }
        }

    override fun hashCode(): Int = listOf(name, grammar).hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is OverrideRule -> false
        else -> this.name == other.name && other.grammar == this.grammar
    }

    override fun toString(): String {
        var f = ""
        if (isSkip) f += "skip "
        if (isLeaf) f += "leaf "
        return "override $f$name = $rhs ;"
    }

}
