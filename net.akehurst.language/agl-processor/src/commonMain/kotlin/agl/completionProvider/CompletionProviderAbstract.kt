/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.completionProvider

import net.akehurst.language.api.grammar.Terminal
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionProvider

abstract class CompletionProviderAbstract<in AsmType, in ContextType> : CompletionProvider<AsmType, ContextType> {

    fun provideForTerminal(terminalItem: Terminal, context: ContextType?): List<CompletionItem> {
        val name = when {
            terminalItem.owningRule.isLeaf -> terminalItem.owningRule.name
            else -> terminalItem.name
        }
        val ci = when {
            terminalItem.isPattern -> CompletionItem(CompletionItemKind.PATTERN, "<$name>", terminalItem.value)
            else -> CompletionItem(CompletionItemKind.LITERAL, terminalItem.value, name)
        }
        return listOf(ci)
    }

}