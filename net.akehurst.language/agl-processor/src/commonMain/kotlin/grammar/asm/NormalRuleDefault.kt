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

package net.akehurst.language.grammar.asm

import net.akehurst.language.base.api.Indent
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.api.NormalRule
import net.akehurst.language.grammar.api.RuleItem

class NormalRuleDefault(
    override val grammar: Grammar,
    override val name: GrammarRuleName,
    override val isSkip: Boolean,
    override val isLeaf: Boolean
) : GrammarRuleAbstract(), NormalRule {

    override val isOverride: Boolean = false

    private var _rhs: RuleItem? = null
    override var rhs: RuleItem
        get() {
            return this._rhs ?: error("rhs of rule must be set")
        }
        set(value) {
            value.setOwningRule(this, listOf(0))
            this._rhs = value
        }

    override fun asString(indent: Indent): String {
        var f = ""
        if (isSkip) f += "skip "
        if (isLeaf) f += "leaf "
        val rhsStr = rhs.toString() //TODO: rhs.asString(indent)
        return "$f$name = $rhsStr ;"
    }

    override fun hashCode(): Int = listOf(name, grammar).hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is NormalRule -> false
        else -> this.name == other.name && other.grammar == this.grammar
    }

    override fun toString(): String {
        var f = ""
        if (isSkip) f += "skip "
        if (isLeaf) f += "leaf "
        return "$f$name = $rhs ;"
    }
}