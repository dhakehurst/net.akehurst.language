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

import net.akehurst.language.grammar.api.RuleItem

enum class CompletionItemKind {
    LITERAL,
    PATTERN,
    REFERRED,
    SEGMENT
}

data class CompletionItem(
    val kind: CompletionItemKind,
    val text: String,
    val label: String
) {
    var description: String = ""
}

interface Spine {
    val expectedNextItems: Set<RuleItem>
    val elements: List<RuleItem>
    val nextChildNumber: Int
}

interface CompletionProvider<in AsmType, in ContextType> {
    fun provide(nextExpected: Set<Spine>, context: ContextType?, options: Map<String, Any>): List<CompletionItem>
}