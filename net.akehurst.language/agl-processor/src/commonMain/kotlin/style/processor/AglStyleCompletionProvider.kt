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

package net.akehurst.language.style.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.grammar.processor.ContextFromGrammar
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.Terminal
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.asm.AglStyleModelDefault
import net.akehurst.language.typemodel.api.TypeInstance

class AglStyleCompletionProvider() : CompletionProvider<AglStyleModel, ContextFromGrammar> {

    companion object {
        private val aglGrammarQualifiedName = Agl.registry.agl.grammar.processor!!.targetGrammar!!.qualifiedName
        private val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
        private val aglGrammarNamespace: GrammarTypeNamespace
            get() = aglGrammarTypeModel.findNamespaceOrNull(aglGrammarQualifiedName) as GrammarTypeNamespace? ?: error("Internal error")

        private val aglStyleQualifiedName = Agl.registry.agl.style.processor!!.targetGrammar!!.qualifiedName
        private val aglStyleTypeModel = Agl.registry.agl.style.processor!!.typeModel
        private val aglStyleNamespace: GrammarTypeNamespace
            get() = aglStyleTypeModel.findNamespaceOrNull(aglStyleQualifiedName) as GrammarTypeNamespace? ?: error("")


        //        private val terminal = aglGrammarNamespace.findTypeUsageForRule("terminal") ?: error("Internal error: type for 'terminal' not found")
        private val grammarRule = aglGrammarNamespace.findTypeForRule(GrammarRuleName("grammarRule")) ?: error("Internal error: type for 'grammarRule' not found")

//        private val LITERAL = aglStyleNamespace.findTypeUsageForRule("LITERAL") ?: error("Internal error: type for 'LITERAL' not found")
//        private val PATTERN = aglStyleNamespace.findTypeUsageForRule("PATTERN") ?: error("Internal error: type for 'PATTERN' not found")
//        private val IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("IDENTIFIER") ?: error("Internal error: type for 'IDENTIFIER' not found")
//        private val META_IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("META_IDENTIFIER") ?: error("Internal error: type for 'META_IDENTIFIER' not found")
//        private val STYLE_ID = aglStyleNamespace.findTypeUsageForRule("STYLE_ID") ?: error("Internal error: type for 'STYLE_ID' not found")
//        private val STYLE_VALUE = aglStyleNamespace.findTypeUsageForRule("STYLE_VALUE") ?: error("Internal error: type for 'STYLE_VALUE' not found")
    }

    override fun provide(nextExpected: Set<Spine>, context: ContextFromGrammar?, options: Map<String, Any>): List<CompletionItem> {
        return if (null == context) {
            emptyList()
        } else {
            val items = nextExpected.flatMap { it.expectedNextItems.flatMap { provideForTerminalItem(it, context) } }
            items
        }
    }

    private fun provideForTerminalItem(nextExpected: RuleItem, context: ContextFromGrammar): List<CompletionItem> {
        val itemType = aglStyleNamespace.findTypeForRule(nextExpected.owningRule.name) ?: error("Should not be null")
        return when (nextExpected.owningRule.name) {
            GrammarRuleName("LITERAL") -> LITERAL(nextExpected, itemType, context)
            GrammarRuleName("PATTERN") -> PATTERN(nextExpected, itemType, context)
            GrammarRuleName("IDENTIFIER") -> IDENTIFIER(nextExpected, itemType, context)
            GrammarRuleName("META_IDENTIFIER") -> META_IDENTIFIER(nextExpected, itemType, context)
            GrammarRuleName("STYLE_ID") -> STYLE_ID(nextExpected, itemType, context)
            GrammarRuleName("STYLE_VALUE") -> STYLE_VALUE(nextExpected, itemType, context)
            GrammarRuleName("selectorAndComposition") -> selectorAndComposition(nextExpected, itemType, context)
            GrammarRuleName("rule") -> rule(nextExpected, itemType, context)
            GrammarRuleName("style") -> style(nextExpected, itemType, context)
            else -> emptyList()
        }
    }

    private fun LITERAL(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        val scopeItems = context.rootScope.findItemsConformingTo { it.value == "LITERAL" }
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, it.referableName, "LITERAL").also {
                it.description = "Reference to a literal value used in the grammar. Literals are enclosed in single quotes or leaf rules."
            }
        }
    }

    private fun PATTERN(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        val scopeItems = context.rootScope.findItemsConformingTo { it.value == "PATTERN" }
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, it.referableName, "PATTERN").also {
                it.description = "Reference to a pattern value (regular expression) used in the grammar. Patterns are enclosed in double quotes or leaf rules."
            }
        }
    }

    private fun IDENTIFIER(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        val scopeItems = context.rootScope.findItemsConformingTo { it == grammarRule.declaration.qualifiedName }
        return scopeItems.map {
            CompletionItem(CompletionItemKind.LITERAL, it.referableName, grammarRule.declaration.name.value)
        }
    }

    private fun META_IDENTIFIER(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, AglStyleModelDefault.KEYWORD_STYLE_ID, "META_IDENTIFIER"),
            CompletionItem(CompletionItemKind.LITERAL, AglStyleModelDefault.NO_STYLE_ID, "META_IDENTIFIER")
        )
    }

    private fun STYLE_ID(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "foreground", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "background", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "font-style", "STYLE_ID"),
            CompletionItem(CompletionItemKind.SEGMENT, "<STYLE_ID>: <STYLE_VALUE>;", "style"),
        )
    }

    private fun STYLE_VALUE(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "<colour>", "STYLE_VALUE"),
            CompletionItem(CompletionItemKind.LITERAL, "bold", "STYLE_VALUE"),
            CompletionItem(CompletionItemKind.LITERAL, "italic", "STYLE_VALUE"),
        )
    }

    private fun selectorAndComposition(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, ",", "selectorAndComposition"),
        )
    }

    private fun rule(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.value) {
                "'{'" -> listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "{", "rule"),
                    CompletionItem(CompletionItemKind.SEGMENT, "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}", "rule"),
                )

                "'}'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "}", "rule"))
                else -> error("Internal error: RuleItem $nextExpected not handled")
            }

            else -> error("Internal error: RuleItem $nextExpected not handled")
        }
    }

    private fun style(nextExpected: RuleItem, ti: TypeInstance, context: ContextFromGrammar): List<CompletionItem> {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.value) {
                "':'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, ":", "style"))
                "';'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, ";", "style"))
                else -> error("Internal error: RuleItem $nextExpected not handled")
            }

            else -> error("Internal error: RuleItem $nextExpected not handled")
        }
    }
}