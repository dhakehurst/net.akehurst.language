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

package net.akehurst.language.agl.grammar.grammar.asm

import net.akehurst.language.api.grammar.*

data class OverrideRuleDefault(
    override val grammar: Grammar,
    override val name: String,
    override val isSkip: Boolean,
    override val isLeaf: Boolean,
    override val overrideKind: OverrideKind
) : GrammarRuleAbstract(), OverrideRule {

    override val isOverride: Boolean = true

    private var _overridenRhs: RuleItem? = null
    var overridenRhs: RuleItem
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
            OverrideKind.APPEND_ALTERNATIVE -> {
                val or = this.grammar.findAllSuperNonTerminalRule(this.name).first()
                when (or.rhs) {
                    is ChoiceLongest -> {
                        val appendedAlternatives = (or.rhs as ChoiceLongest).alternative + overridenRhs
                        val ac = ChoiceLongestDefault(appendedAlternatives)
                        val indices = (or.rhs as ChoiceLongestDefault).index!!
                        val ni = indices.dropLast(1) + indices.last() + 1
                        ac.setOwningRule(this, ni)
                        ac
                    }

                    else -> error("Cannot append choice to a non-longest-choice rule")
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
