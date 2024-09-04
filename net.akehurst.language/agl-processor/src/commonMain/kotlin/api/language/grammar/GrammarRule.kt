/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.api.language.grammar

import net.akehurst.language.api.language.base.Formatable
import kotlin.jvm.JvmInline

@JvmInline
value class GrammarRuleName(val value: String) {
    override fun toString(): String = value
}

interface PreferenceRule : GrammarItem {
    val forItem: SimpleItem
    val optionList: List<PreferenceOption>
}

interface PreferenceOption : Formatable {

    enum class Associativity { LEFT, RIGHT }

    val item: NonTerminal
    val choiceNumber: Int
    val onTerminals: List<SimpleItem>
    val associativity: Associativity
}

interface GrammarRule : GrammarItem {
    val name: GrammarRuleName
    val isOverride: Boolean
    val isSkip: Boolean
    val isLeaf: Boolean
    val isOneEmbedded: Boolean
    val rhs: RuleItem

    val compressedLeaf: Terminal
}

interface NormalRule : GrammarRule {
}

enum class OverrideKind { REPLACE, APPEND_ALTERNATIVE, SUBSTITUTION }

interface OverrideRule : GrammarRule {
    val overrideKind: OverrideKind
    val overridenRhs: RuleItem
}