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
import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract.Companion.defaultSortAndFilter
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.*
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.Terminal
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.asm.AglStyleModelDefault
import net.akehurst.language.typemodel.api.TypeInstance

class AglStyleCompletionProvider() : CompletionProvider<AglStyleModel, ContextWithScope<Any,Any>> {

    companion object {
        private val aglGrammarQualifiedName get() = Agl.registry.agl.grammar.processor!!.targetGrammar!!.qualifiedName
        private val aglGrammarTypeModel get() = Agl.registry.agl.grammar.processor!!.typesModel
        private val aglGrammarNamespace: GrammarTypeNamespace get() = aglGrammarTypeModel.findNamespaceOrNull(aglGrammarQualifiedName) as GrammarTypeNamespace? ?: error("Internal error")

        private val aglStyleQualifiedName get() = Agl.registry.agl.style.processor!!.targetGrammar!!.qualifiedName
        private val aglStyleTypeModel get() = Agl.registry.agl.style.processor!!.typesModel
        private val aglStyleNamespace: GrammarTypeNamespace get() = aglStyleTypeModel.findNamespaceOrNull(aglStyleQualifiedName) as GrammarTypeNamespace? ?: error("")

        private val aglBaseQualifiedName get() = Agl.registry.agl.base.processor!!.targetGrammar!!.qualifiedName
        //private val aglBaseTypeModel = Agl.registry.agl.base.processor!!.typeModel
        private val aglBaseNamespace: GrammarTypeNamespace get() = aglStyleTypeModel.findNamespaceOrNull(aglBaseQualifiedName) as GrammarTypeNamespace? ?: error("")

        //        private val terminal = aglGrammarNamespace.findTypeUsageForRule("terminal") ?: error("Internal error: type for 'terminal' not found")
        private val grammarRule = aglGrammarNamespace.findTypeForRule(GrammarRuleName("grammarRule")) ?: error("Internal error: type for 'grammarRule' not found")

//        private val LITERAL = aglStyleNamespace.findTypeUsageForRule("LITERAL") ?: error("Internal error: type for 'LITERAL' not found")
//        private val PATTERN = aglStyleNamespace.findTypeUsageForRule("PATTERN") ?: error("Internal error: type for 'PATTERN' not found")
//        private val IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("IDENTIFIER") ?: error("Internal error: type for 'IDENTIFIER' not found")
//        private val META_IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("META_IDENTIFIER") ?: error("Internal error: type for 'META_IDENTIFIER' not found")
//        private val STYLE_ID = aglStyleNamespace.findTypeUsageForRule("STYLE_ID") ?: error("Internal error: type for 'STYLE_ID' not found")
//        private val STYLE_VALUE = aglStyleNamespace.findTypeUsageForRule("STYLE_VALUE") ?: error("Internal error: type for 'STYLE_VALUE' not found")
    }

    override fun provide(spines: Set<Spine>, options: CompletionProviderOptions<ContextWithScope<Any,Any>>): List<CompletionItem> {
        val context = options.context
        return if (null == context) {
            emptyList()
        } else {
            val items = spines.flatMap { spine -> spine.expectedNextLeafNonTerminalOrTerminal.flatMap { provideForTerminalItem(options.depth, spine, it, context) } }
            defaultSortAndFilter(items)
        }
    }

    private fun provideForTerminalItem(depth: Int, spine: Spine, nextExpected: RuleItem, context: ContextWithScope<Any, Any>): List<CompletionItem> {
        val itemType = aglStyleNamespace.findTypeForRule(nextExpected.owningRule.name)
            ?: aglBaseNamespace.findTypeForRule(nextExpected.owningRule.name)
            ?: error("Should not be null")
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
            else -> CompletionProviderAbstract.provideDefaultForSpine(depth,spine)
        }
    }

    private fun LITERAL(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        val scopeItems = context.findItemsConformingTo { it.value == "LITERAL" }
        return scopeItems.map {
            CompletionItem(CompletionItemKind.REFERRED, "LITERAL", it.referableName).also {
                it.description = "Reference to a literal value used in the grammar. Literals are enclosed in single quotes or leaf rules."
            }
        }
    }

    private fun PATTERN(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        val scopeItems = context.findItemsConformingTo { it.value == "PATTERN" }
        return scopeItems.map {
            CompletionItem(CompletionItemKind.REFERRED, "PATTERN", it.referableName).also {
                it.description = "Reference to a pattern value (regular expression) used in the grammar. Patterns are enclosed in double quotes or leaf rules."
            }
        }
    }

    private fun IDENTIFIER(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        val scopeItems = context.findItemsConformingTo { it == grammarRule.resolvedDeclaration.qualifiedName }
        return scopeItems.map {
            CompletionItem(CompletionItemKind.REFERRED, grammarRule.resolvedDeclaration.name.value, it.referableName)
        }
    }

    private fun META_IDENTIFIER(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", AglStyleModelDefault.KEYWORD_STYLE_ID.value),
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", AglStyleModelDefault.NO_STYLE_ID.value)
        )
    }

    private fun STYLE_ID(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "foreground"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "background"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "font-style"),
            CompletionItem(CompletionItemKind.SEGMENT, "style", "<STYLE_ID>: <STYLE_VALUE>;"),
        )
    }

    private fun STYLE_VALUE(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "<colour>"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "bold"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "italic"),
        )
    }

    private fun selectorAndComposition(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "selectorAndComposition", ","),
        )
    }

    private fun rule(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.value) {
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

    private fun style(nextExpected: RuleItem, ti: TypeInstance, context: ContextWithScope<Any,Any>): List<CompletionItem> {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.value) {
                "':'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "style", ":"))
                "';'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "style", ";"))
                else -> error("Internal error: RuleItem $nextExpected not handled")
            }

            else -> error("Internal error: RuleItem $nextExpected not handled")
        }
    }
}