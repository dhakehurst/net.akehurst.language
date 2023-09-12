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

package net.akehurst.language.agl.agl.grammar.style

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyleModel

class AglStyleCompletionProvider() : CompletionProvider<AglStyleModel, SentenceContext<String>> {

    private val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
    private val namespace: GrammarTypeNamespace
        get() =
            aglGrammarTypeModel.namespace[Agl.registry.agl.grammar.processor!!.grammar!!.qualifiedName] as GrammarTypeNamespace? ?: error("")

    override fun provide(terminalItems: List<CompletionItem>, context: SentenceContext<String>?, options: Map<String, Any>): List<CompletionItem> {
        return if (null == context) {
            terminalItems
        } else {
            val items = terminalItems.flatMap { provideForTerminalItem(it, context) }
            items
        }
    }

    private fun provideForTerminalItem(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        val itemType = namespace.findTypeUsageForRule(termItem.ruleName)
        return when (termItem.ruleName) {
            "LITERAL" -> LITERAL(termItem, context)
            "PATTERN" -> PATTERN(termItem, context)
            "IDENTIFIER" -> IDENTIFIER(termItem, context)
            "META_IDENTIFIER" -> META_IDENTIFIER(termItem, context)
            "STYLE_ID" -> STYLE_ID(termItem, context)
            "STYLE_VALUE" -> STYLE_VALUE(termItem, context)
            else -> emptyList()
        }
    }

    private fun LITERAL(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        val scopeItems = context.rootScope.items["LITERAL"]?.values ?: emptyList()
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", it)
        }
    }

    private fun PATTERN(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        val scopeItems = context.rootScope.items["PATTERN"]?.values ?: emptyList()
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, "PATTERN", it)
        }
    }

    private fun IDENTIFIER(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        val rType = namespace.findTypeUsageForRule("grammarRule") ?: error("Type not found for rule 'grammarRule'")
        val scopeItems = context.rootScope.items[rType.type.qualifiedName]?.values ?: emptyList()
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, rType.type.name, it)
        }
    }

    private fun META_IDENTIFIER(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        return emptyList()
    }

    private fun STYLE_ID(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        return emptyList()
    }

    private fun STYLE_VALUE(termItem: CompletionItem, context: SentenceContext<String>): List<CompletionItem> {
        return emptyList()
    }

}