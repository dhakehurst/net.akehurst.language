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

import net.akehurst.language.api.grammar.RuleItem

enum class CompletionItemKind {
    LITERAL,
    PATTERN,
    SEGMENT
}

data class CompletionItem(
    val kind: CompletionItemKind,
    val text: String,
    val name: String
) {
    var description: String = ""
}

interface CompletionProvider<in AsmType, in ContextType> {
    fun provide(nextExpected: Set<RuleItem>, context: ContextType?, options: Map<String, Any>): List<CompletionItem>
}