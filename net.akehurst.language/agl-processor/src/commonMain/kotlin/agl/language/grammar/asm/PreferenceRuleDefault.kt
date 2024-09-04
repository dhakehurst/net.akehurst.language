/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.api.language.base.Indent
import net.akehurst.language.api.language.grammar.*

data class PreferenceRuleDefault(
    override val grammar: Grammar,
    override val forItem: SimpleItem,
    override val optionList: List<PreferenceOption>
) : GrammarItemAbstract(), PreferenceRule {

    override fun asString(indent: Indent): String {
        val ni = indent.inc
        val optStr = optionList.joinToString(separator = "\n") { it.asString(ni) }
        return "preference $forItem {\n${optStr}\n$indent}"
    }

    override fun toString(): String = "preference $forItem { ... }"
}

data class PreferenceOptionDefault(
    override val item: NonTerminal,
    override val choiceNumber: Int,
    override val onTerminals: List<SimpleItem>,
    override val associativity: PreferenceOption.Associativity
) : PreferenceOption {

    override fun asString(indent: Indent): String {
        return "$item $choiceNumber 'on' ${onTerminals.joinToString(separator = ", ")} $associativity ;"
    }

}