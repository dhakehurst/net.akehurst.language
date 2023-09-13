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
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.grammar.Terminal
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.typemodel.api.TypeInstance

class AglStyleCompletionProvider() : CompletionProvider<AglStyleModel, SentenceContext<String>> {

    companion object {
        private val aglGrammarQualifiedName = Agl.registry.agl.grammar.processor!!.grammar!!.qualifiedName
        private val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
        private val aglGrammarNamespace: GrammarTypeNamespace
            get() = aglGrammarTypeModel.namespace[aglGrammarQualifiedName] as GrammarTypeNamespace? ?: error("Internal error")

        private val aglStyleQualifiedName = Agl.registry.agl.style.processor!!.grammar!!.qualifiedName
        private val aglStyleTypeModel = Agl.registry.agl.style.processor!!.typeModel
        private val aglStyleNamespace: GrammarTypeNamespace
            get() = aglStyleTypeModel.namespace[aglStyleQualifiedName] as GrammarTypeNamespace? ?: error("")


        //        private val terminal = aglGrammarNamespace.findTypeUsageForRule("terminal") ?: error("Internal error: type for 'terminal' not found")
        private val grammarRule = aglGrammarNamespace.findTypeUsageForRule("grammarRule") ?: error("Internal error: type for 'grammarRule' not found")

//        private val LITERAL = aglStyleNamespace.findTypeUsageForRule("LITERAL") ?: error("Internal error: type for 'LITERAL' not found")
//        private val PATTERN = aglStyleNamespace.findTypeUsageForRule("PATTERN") ?: error("Internal error: type for 'PATTERN' not found")
//        private val IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("IDENTIFIER") ?: error("Internal error: type for 'IDENTIFIER' not found")
//        private val META_IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("META_IDENTIFIER") ?: error("Internal error: type for 'META_IDENTIFIER' not found")
//        private val STYLE_ID = aglStyleNamespace.findTypeUsageForRule("STYLE_ID") ?: error("Internal error: type for 'STYLE_ID' not found")
//        private val STYLE_VALUE = aglStyleNamespace.findTypeUsageForRule("STYLE_VALUE") ?: error("Internal error: type for 'STYLE_VALUE' not found")
    }

    override fun provide(nextExpected: Set<RuleItem>, context: SentenceContext<String>?, options: Map<String, Any>): List<CompletionItem> {
        return if (null == context) {
            emptyList()
        } else {
            val items = nextExpected.flatMap { provideForTerminalItem(it, context) }
            items
        }
    }

    private fun provideForTerminalItem(nextExpected: RuleItem, context: SentenceContext<String>): List<CompletionItem> {
        val itemType = aglStyleNamespace.findTypeUsageForRule(nextExpected.owningRule.name) ?: error("Should not be null")
        return when (nextExpected.owningRule.name) {
            "LITERAL" -> LITERAL(nextExpected, itemType, context)
            "PATTERN" -> PATTERN(nextExpected, itemType, context)
            "IDENTIFIER" -> IDENTIFIER(nextExpected, itemType, context)
            "META_IDENTIFIER" -> META_IDENTIFIER(nextExpected, itemType, context)
            "STYLE_ID" -> STYLE_ID(nextExpected, itemType, context)
            "STYLE_VALUE" -> STYLE_VALUE(nextExpected, itemType, context)
            "selectorAndComposition" -> selectorAndComposition(nextExpected, itemType, context)
            "rule" -> rule(nextExpected, itemType, context)
            "style" -> style(nextExpected, itemType, context)
            else -> emptyList()
        }
    }

    private fun LITERAL(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        val scopeItems = context.rootScope.items["LITERAL"]?.values ?: emptyList()
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", it)
        }
    }

    private fun PATTERN(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        val scopeItems = context.rootScope.items["PATTERN"]?.values ?: emptyList()
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, "PATTERN", it)
        }
    }

    private fun IDENTIFIER(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        val scopeItems = context.rootScope.items[grammarRule.type.qualifiedName]?.values ?: emptyList()
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, grammarRule.type.name, it)
        }
    }

    private fun META_IDENTIFIER(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        return listOf(CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", "\$keyword"))
    }

    private fun STYLE_ID(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "foreground"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "background"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "font-style"),
            CompletionItem(CompletionItemKind.SEGMENT, "style", "<STYLE_ID>: <STYLE_VALUE>;"),
        )
    }

    private fun STYLE_VALUE(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "<colour>"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "bold"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "italic"),
        )
    }

    private fun selectorAndComposition(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "selectorAndComposition", ","),
        )
    }

    private fun rule(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.name) {
                "'{'" -> listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "rule", "{"),
                    CompletionItem(CompletionItemKind.SEGMENT, "rule", "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}"),
                )

                "'}'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "rule", "}"))
                else -> error("Internal error: RuleItem $nextExpected not handled")
            }

            else -> error("Internal error: RuleItem $nextExpected not handled")
        }
    }

    private fun style(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext<String>): List<CompletionItem> {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.name) {
                "':'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "style", ":"))
                "';'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "style", ";"))
                else -> error("Internal error: RuleItem $nextExpected not handled")
            }

            else -> error("Internal error: RuleItem $nextExpected not handled")
        }
    }
}