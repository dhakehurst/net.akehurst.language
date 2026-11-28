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

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract.Companion.defaultSortAndFilter
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.processor.*
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.Terminal
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.asm.AglStyleDomainDefault
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypeInstance

class AglStyleCompletionProvider() : CompletionProvider<AglStyleDomain, SentenceContext> {

    companion object {
        // private val aglGrammarQualifiedName get() = Agl.registry.agl.grammar.processor!!.targetGrammar!!.qualifiedName
        // private val aglGrammarTypeModel get() = Agl.registry.agl.grammar.processor!!.typesModel
        //private val aglGrammarNamespace: GrammarTypeNamespace get() = aglGrammarTypeModel.findNamespaceOrNull(aglGrammarQualifiedName) as GrammarTypeNamespace? ?: error("Internal error")

        //private val aglStyleQualifiedName get() = Agl.registry.agl.style.processor!!.targetGrammar!!.qualifiedName
        //private val aglStyleTypeModel get() = Agl.registry.agl.style.processor!!.typesModel
        //private val aglStyleNamespace: GrammarTypeNamespace get() = AglStyle.typesModel.findNamespaceOrNull(QualifiedName("net.akehurst.language.style.api")) as GrammarTypeNamespace? ?: error("Internal error: aglStyleNamespace not found")

        private val styleTransformRuleSet =
            AglStyle.asmTransformDomain.findDefinitionByQualifiedNameOrNull(AglStyle.defaultTargetGrammar.qualifiedName) ?: error("Internal error: styleTransformRuleSet not found")

        //private val aglBaseQualifiedName get() = Agl.registry.agl.base.processor!!.targetGrammar!!.qualifiedName
        //private val aglBaseTypeModel = Agl.registry.agl.base.processor!!.typeModel
        // private val aglBaseNamespace: GrammarTypeNamespace get() = AglBase.typesModel.findNamespaceOrNull(QualifiedName("net.akehurst.language.base.api")) as GrammarTypeNamespace? ?: error("")

        //        private val terminal = aglGrammarNamespace.findTypeUsageForRule("terminal") ?: error("Internal error: type for 'terminal' not found")
        //private val grammarRule = aglGrammarNamespace.findTypeForRule(GrammarRuleName("grammarRule")) ?: error("Internal error: type for 'grammarRule' not found")
        private val grammarRuleTypeDefinition =
            AglGrammar.typesDomain.findByQualifiedNameOrNull(QualifiedName("net.akehurst.language.grammar.api.GrammarRule")) ?: error("Internal error: type for 'grammarRule' not found")

//        private val LITERAL = aglStyleNamespace.findTypeUsageForRule("LITERAL") ?: error("Internal error: type for 'LITERAL' not found")
//        private val PATTERN = aglStyleNamespace.findTypeUsageForRule("PATTERN") ?: error("Internal error: type for 'PATTERN' not found")
//        private val IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("IDENTIFIER") ?: error("Internal error: type for 'IDENTIFIER' not found")
//        private val META_IDENTIFIER = aglStyleNamespace.findTypeUsageForRule("META_IDENTIFIER") ?: error("Internal error: type for 'META_IDENTIFIER' not found")
//        private val STYLE_ID = aglStyleNamespace.findTypeUsageForRule("STYLE_ID") ?: error("Internal error: type for 'STYLE_ID' not found")
//        private val STYLE_VALUE = aglStyleNamespace.findTypeUsageForRule("STYLE_VALUE") ?: error("Internal error: type for 'STYLE_VALUE' not found")
    }

    override fun provide(spines: Set<Spine>, options: CompletionProviderOptions<SentenceContext>): List<CompletionItem> {
        val context = options.sentenceContext
        return if (null == context) {
            emptyList()
        } else {
            val items = spines.flatMap { spine ->
                when {
                    // the type name of a semantic selector ('type T', 'identify T', 'refers-to T'): the types of the context (SD6)
                    isTypeNameOfSemanticSelector(spine) -> typeNameItems(context, options.sentenceAsm)
                    // the value of a declaration: the values that fit its name (SD7)
                    expectsStyleValue(spine) -> styleValueItems(styleNameBeforePosition(options))
                    else -> {
                        // items referring to the grammar being styled, from the references definition of AglStyle
                        val refs = CompletionProviderAbstract.provideReferencesForSpine(spine, context, styleTransformRuleSet, AglStyle.typesDomain, AglStyle.crossReferenceDomain)
                        refs.values.flatten() + spine.expectedNextLeafNonTerminalOrTerminal
                            .filter { CompletionProviderAbstract.referenceKeyFor(it) !in refs.keys }
                            .flatMap { provideForTerminalItem(spine, it, options) }
                    }
                }
            }
            defaultSortAndFilter(items)
        }
    }

    /** true if the spine expects the value of a declaration, i.e. child 2 of 'style = STYLE_ID ':' styleValue ';'' */
    private fun expectsStyleValue(spine: Spine): Boolean =
        spine.expectedNextLeafNonTerminalOrTerminal.any { it.owningRule.isLeaf && it.owningRule.name.value == "STYLE_VALUE" }

    /**
     * the name of the declaration whose value is completed: the last declaration of the sentence (completed up to the position),
     * null if not known (no sentence ASM)
     */
    private fun styleNameBeforePosition(options: CompletionProviderOptions<SentenceContext>): String? {
        val asm = options.sentenceAsm as? AglStyleDomain ?: return null
        val locationMap = options.sentenceLocationMap ?: return null
        return asm.allDefinitions.flatMap { it.rules }.flatMap { it.declaration.values }
            .maxByOrNull { locationMap[it]?.position ?: -1 }?.name
    }

    /** the values offered for a declaration named [name] (any of them when the name is not known) */
    private fun styleValueItems(name: String?): List<CompletionItem> {
        val values = when (name) {
            "foreground", "background" -> listOf("<colour>")
            "font-style" -> listOf("italic", "normal")
            "font-weight" -> listOf("bold", "normal")
            else -> listOf("<colour>", "bold", "italic")
        }
        return values.map { CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", it) }
    }

    /** true if the spine expects the type name of a semantic selector, i.e. child 1 of 'semanticSelector = semanticKind possiblyQualifiedName' */
    private fun isTypeNameOfSemanticSelector(spine: Spine): Boolean =
        spine.elements.any { it.rule.name.value == "semanticSelector" && it.nextChildNumber >= 1 }

    /**
     * The types (TypeDefinition items) of the context, except those of 'std'.
     * The name is written as a simple name when the style set finds it that way (the types namespace with the qualified name
     * of the style set, or an 'import-types' namespace, see AglStyleSemanticAnalyser.resolveSemanticSelector),
     * otherwise qualified (CR1); the qualified name is the description.
     * The style set is the last one of the sentence before the position.
     */
    private fun typeNameItems(context: SentenceContext, sentenceAsm: Any?): List<CompletionItem> {
        val styleSet = (sentenceAsm as? AglStyleDomain)?.allDefinitions?.lastOrNull()
        val visible = styleSet?.let {
            CompletionProviderAbstract.Companion.VisibleNamespaces(it.qualifiedName.value, it.importTypes.map { imp -> imp.value }, null)
        }
        val found = CompletionProviderAbstract.allItemsConformingTo(context) { it.value == TypeDefinition::class.simpleName }
            .filter { (path, _) -> path.isNotEmpty() && path != listOf("std") }
        return found.map { (path, item) -> CompletionProviderAbstract.completionItemForReferred(path, item, found, visible) }
    }

    private fun provideForTerminalItem(spine: Spine, nextExpected: RuleItem, options: CompletionProviderOptions<SentenceContext>): List<CompletionItem> {
//        val itemType = aglStyleNamespace.findTypeForRule(nextExpected.owningRule.name)
//            ?: aglBaseNamespace.findTypeForRule(nextExpected.owningRule.name)
//            ?: error("Should not be null")
        // A literal terminal (e.g. ':') is shared by all rules that use it, so its owningRule can be any of them (even one in AglBase).
        // Use the rule of the spine, which is the rule that expects it here.
        val ruleName = when {
            nextExpected is Terminal && nextExpected.isLiteral -> spine.elements.firstOrNull()?.rule?.name ?: nextExpected.owningRule.name
            else -> nextExpected.owningRule.name
        }
        val itemType = styleTransformRuleSet.findAllTrRuleForGrammarRuleNamedOrNull(ruleName)?.resolvedType
            // e.g. a rule from AglBase that has no transform rule in the style rule set
            ?: return CompletionProviderAbstract.provideDefaultForSpine(spine, options)
        // the literals of selectorAndComposition, rule (tagRule, metaRule) and style, with their labels and the '{' snippet,
        // are now in the AglStyle completion definition (completionString), so the functions for them below are not used
        return when (ruleName) {
            else -> CompletionProviderAbstract.provideDefaultForSpine(spine, options)
        }
    }

    private fun selectorAndComposition(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext): List<CompletionItem> {
        return listOf(
            CompletionItem(CompletionItemKind.LITERAL, "selectorAndComposition", ","),
        )
    }

    private fun rule(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext): List<CompletionItem>? {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.id) {
                "'{'" -> listOf(
                    CompletionItem(CompletionItemKind.LITERAL, "rule", "{"),
                    CompletionItem(CompletionItemKind.SEGMENT, "rule", "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}"),
                )

                "'}'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "rule", "}"))
                else -> null // not handled here, the default is used
            }

            else -> null // not handled here, the default is used
        }
    }

    private fun style(nextExpected: RuleItem, ti: TypeInstance, context: SentenceContext): List<CompletionItem>? {
        return when (nextExpected) {
            is Terminal -> when (nextExpected.id) {
                "':'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "style", ":"))
                "';'" -> listOf(CompletionItem(CompletionItemKind.LITERAL, "style", ";"))
                else -> null // not handled here, the default is used
            }

            else -> null // not handled here, the default is used
        }
    }
}