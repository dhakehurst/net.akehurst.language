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

package net.akehurst.language.api.processor

import net.akehurst.language.grammar.api.Concatenation
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarRule
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.TangibleItem

enum class CompletionItemKind {
    PATTERN,
    LITERAL,
    SEGMENT,
    REFERRED,
}

/**
 * @param label name shown to the user, e.g. the rule name (for a PATTERN the leaf rule name, its regex is in [description])
 * @param text the text inserted into the sentence
 */
data class CompletionItem(
    val kind: CompletionItemKind,
    val label: String,
    val text: String
) {
    /** index of the expansion this item comes from (see CompletionProviderOptions.path), null if not from an expansion */
    var id: Int? = null
    /** more information for the user, e.g. shown on hover or pre-selection in an editor. For a PATTERN it is the pattern (regex) */
    var description: String = ""
    /** the grammar item this completion comes from, if any (for use by code, not shown to the user) */
    var detail: RuleItem? = null

    /**
     * position in the sentence from which this item replaces the text (up to the requested position).
     * Items in one result can have different positions, e.g. completing a partially typed word (its start)
     * or inserting what is expected next (the requested position).
     * null if not set, in which case use [ExpectedAtResult.offeredPosition].
     */
    var offeredPosition: Int? = null
}

interface SpineNode {
    val rule: GrammarRule
    val nextChildNumber: Int
    val nextExpectedItems: Set<RuleItem>
    //val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem>
    val nextExpectedConcatenation: Set<Concatenation>
}

interface Spine {
    val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem>
    val expectedNextRuleItems: Set<RuleItem>
    val elements: List<SpineNode>
    val nextChildNumber: Int

    /**
     * the grammar that the sentence is parsed with.
     * Rule references are resolved in it, so that overridden rules are used.
     * null if not known, in which case a reference is resolved in the grammar that owns it.
     */
    val targetGrammar: Grammar? get() = null
}

interface CompletionProvider<AsmType : Any, ContextType : Any> {
    fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem>
}