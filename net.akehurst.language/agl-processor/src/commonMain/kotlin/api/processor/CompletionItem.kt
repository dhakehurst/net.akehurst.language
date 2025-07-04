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
import net.akehurst.language.grammar.api.GrammarRule
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.TangibleItem

enum class CompletionItemKind {
    PATTERN,
    LITERAL,
    SEGMENT,
    REFERRED,
}

data class CompletionItem(
    val kind: CompletionItemKind,
    val label: String,
    val text: String
) {
    var id: Int? = 0
    var description: String = ""
}

interface SpineNode {
    val rule: GrammarRule
    val nextChildNumber: Int
    val nextExpectedItems: Set<RuleItem>
    val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem>
    val nextExpectedConcatenation: Set<Concatenation>
}

interface Spine {
    val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem>
    val expectedNextRuleItems: Set<RuleItem>
    val elements: List<SpineNode>
    val nextChildNumber: Int
}

interface CompletionProvider<AsmType : Any, ContextType : Any> {
    fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem>
}