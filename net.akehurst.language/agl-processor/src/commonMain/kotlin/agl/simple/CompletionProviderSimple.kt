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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.kotlinx.collections.transitiveClosure
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammarTypemodel.asm.GrammarTypesNamespaceSimple
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.Throwable
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.all
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.getOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.minByOrNull
import kotlin.collections.minOf
import kotlin.collections.plus
import kotlin.collections.setOf
import kotlin.collections.toList
import kotlin.collections.toSet
import kotlin.error
import kotlin.printStackTrace
import kotlin.ranges.until

class CompletionProviderSimple(
    val targetGrammar: Grammar,
    val grammar2TypesDomain: Grammar2TypesDomainMapping,
    val typesDomain: TypesDomain,
    val crossReferenceDomain: CrossReferenceDomain,
    /** the asm-transform rules of [targetGrammar], used to find the property a leaf gives a value to (see [provideReferencesForSpine]) */
    val transformRuleSet: AsmTransformRuleSet? = null
) : CompletionProviderAbstract<Asm, SentenceContext>() {

    val targetNamespace = typesDomain.findNamespaceOrNull(targetGrammar.qualifiedName) as GrammarTypesNamespaceSimple?
        ?: error("Namespace not found for grammar '${targetGrammar.qualifiedName}'")

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<SentenceContext>): List<CompletionItem> {
        val sentenceContext = options.sentenceContext
        // the rule items of all spines, without duplicates, so that each is expanded once
        // (the same list is used with a path, so that the ids of the items stay the same)
        val allRuleItems = nextExpected.flatMap { it.expectedNextRuleItems }.toSet()
        val allTangibles = nextExpected.flatMap { it.expectedNextLeafNonTerminalOrTerminal }.toSet()

        return when {
            options.path.isEmpty() -> {
                val defaultItems = expansionToCompletionItem(provideForRuleItems(allRuleItems, options, targetGrammar)) +
                        provideForTangibles(allTangibles, options, targetGrammar)
                val result = when (sentenceContext) {
                    null -> defaultItems
                    else -> {
                        val referred = nextExpected.flatMap { sp -> provideReferredFor(sp, sentenceContext) }
                        referred + defaultItems
                    }
                }
                defaultSortAndFilter(result.toSet().toList())
            }

            else -> {
                val path = options.path
                val (pthDepth0, pthExpIdx0) = path.first()
                val initExpansions = provideExpansionsForRuleItem(pthDepth0, allRuleItems, options, targetGrammar)
                val expansions = when (path.size) {
                    1 -> initExpansions
                    else -> {
                        var lastItems = initExpansions
                        // an index that is not in the list ends the path, as for the later path elements
                        var pthItem = lastItems.getOrNull(pthExpIdx0)?.ruleItem
                        for (i in 1 until path.size) {
                            if (null == pthItem) break
                            val (pthDepth, pthExpIdx) = path[i]
                            lastItems = expand(pthDepth, pthItem, options, targetGrammar)
                            pthItem = lastItems.getOrNull(pthExpIdx)?.ruleItem
                        }
                        lastItems
                    }
                }
                val cis = expansionToCompletionItem(expansions)
                defaultSortAndFilter(cis)
            }
        }
    }

    /**
     * the items of [sentenceContext] that can be referred to at the end of [spine].
     * Uses the grammar walk of [provideReferencesForSpine] when there is a [transformRuleSet],
     * otherwise the properties of the type of the rule of the spine ([provideForType]).
     */
    private fun provideReferredFor(spine: Spine, sentenceContext: SentenceContext): List<CompletionItem> {
        val trs = transformRuleSet
        return when {
            null != trs -> provideReferencesForSpine(spine, sentenceContext, trs, typesDomain, crossReferenceDomain).values.flatten()
            else -> {
                val firstSpineNode = spine.elements.firstOrNull()
                val type = firstSpineNode?.let { typeFor(it.rule) }
                when {
                    null == firstSpineNode || null == type -> emptyList()
                    else -> provideForType(type, firstSpineNode, sentenceContext)
                }
            }
        }
    }

    fun typeFor(rule: GrammarRule): TypeInstance? = targetNamespace.findTypeForRule(rule.name)


    private fun provideForType(type: TypeInstance, firstSpineNode: SpineNode, sentenceContext: SentenceContext): List<CompletionItem> {
        try {
            val prop = type.resolvedDefinition.getOwnedPropertyByIndexOrNull(firstSpineNode.nextChildNumber)
            //TODO: lists ?
            return when (prop) {
                null -> emptyList()
                else -> {
                    var strProps: Set<PropertyDeclaration> = setOf(prop)
                    // properties already looked at, so that a type whose first property is (or leads back to) itself does not loop for ever
                    var visited: Set<PropertyDeclaration> = strProps
                    while (strProps.isNotEmpty() && strProps.all { it.typeInstance != StdLibDefault.String }) {
                        val next = strProps.filter { it.typeInstance == StdLibDefault.String }.toSet() +
                                strProps.filter { it.typeInstance != StdLibDefault.String }.flatMap { prp ->
                                    val type = prp.typeInstance
                                    val td = type.resolvedDefinition
                                    when (td) {
                                        is CollectionType -> firstPropertyOf(type.typeArguments[0].type)
                                        is StructuredType -> firstPropertyOf(type)
                                        else -> emptyList()
                                    }
                                }.toSet()
                        strProps = next - visited
                        visited = visited + next
                    }
                    strProps.flatMap { prp ->
                        val refTypeNames = crossReferenceDomain.referenceForProperty(prp.owner.qualifiedName, prp.name.value)
                        val refTypes = refTypeNames.mapNotNull { typesDomain.findByQualifiedNameOrNull(it) }
                        val items = refTypes.flatMap { refType ->
                            sentenceContext.findItemsConformingTo {
                                val itemType = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDefinition
                                itemType.conformsTo(refType)
                            }
                        }
                        items.map {
                            CompletionItem(CompletionItemKind.REFERRED, it.qualifiedTypeName.last.value, it.referableName)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            return emptyList()
        }
    }
/*
    private fun provideForType1(type: TypeInstance, firstSpineNode: SpineNode, context: SentenceContext): List<CompletionItem> {
        val prop = type.resolvedDeclaration.getOwnedPropertyByIndexOrNull(firstSpineNode.nextChildNumber)
        //TODO: lists ?
        return when (prop) {
            null -> emptyList()
            else -> {
                val firstTangibles = firstSpineNode.expectedNextLeafNonTerminalOrTerminal
                val compItems = firstTangibles.mapNotNull { ti ->
                    val ni2 = when {
                        ti.owningRule.isLeaf -> NonTerminalDefault(GrammarReferenceDefault(targetGrammar.namespace, targetGrammar.name), ti.owningRule.name)
                        else -> ti
                    }
                    val tiType = StdLibDefault.String.resolvedDeclaration
                    val pn = grammar2TypeModel.propertyNameFor(targetGrammar, ni2, tiType)
                    val refTypeNames = crossReferenceModel.referenceForProperty(prop.typeInstance.qualifiedTypeName, pn.value)
                    val refTypes = refTypeNames.mapNotNull { typeModel.findByQualifiedNameOrNull(it) }
                    val items = refTypes.flatMap { refType ->
                        context.findItemsConformingTo {
                            val itemType = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDeclaration
                            itemType.conformsTo(refType)
                        }
                    }
                    items.map {
                        CompletionItem(CompletionItemKind.REFERRED, it.qualifiedTypeName.last.value, it.referableName)
                    }
                }
                compItems.flatten()
            }
        }
    }
*/
    private fun firstPropertyOf(type: TypeInstance): List<PropertyDeclaration> {
        val typesClosure = setOf(type).transitiveClosure { it.resolvedDefinition.subtypes.toSet() }
        val minProps = typesClosure.mapNotNull { it.allResolvedProperty.values.minByOrNull { it.index } }
        val minOfMin = minProps.minOf { it.index }
        return minProps.filter { minOfMin == it.index }
    }

    /*
        private fun provideForTerminal(item: Terminal, desiredDepth: Int, context: ContextAsmSimple?): List<CompletionItem> {
            val owningRule = item.owningRule
            return when {
                owningRule.isLeaf -> listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<${owningRule.name}>", owningRule.compressedLeaf.value)
                )

                else -> when {
                    item.isPattern -> listOf(CompletionItem(CompletionItemKind.PATTERN, "<${item.value}>", item.value))
                    else -> listOf(CompletionItem(CompletionItemKind.LITERAL, item.value, item.value))
                }
            }
        }

        private fun provideForRuleItem(item: RuleItem, desiredDepth: Int, context: ContextAsmSimple?): List<CompletionItem> {
            val rule = item.owningRule
            return when {
                rule.isLeaf -> listOf(
                    CompletionItem(CompletionItemKind.PATTERN, "<${rule.name}>", rule.compressedLeaf.value)
                )

                else -> {
                    val cis = getItems(item, desiredDepth, context, emptySet())
                    cis.mapNotNull { it }.toSet().toList()
                }
            }
        }

        // uses null to indicate that there is an empty item
        private fun getItems(item: RuleItem, desiredDepth: Int, context: ContextAsmSimple?, done: Set<RuleItem>): List<CompletionItem?> {
            //TODO: use scope to add real items to this list - maybe in a subclass
            return when {
                done.contains(item) -> emptyList()
                else -> when (item) {
                    is EmptyRule -> listOf(null)
                    is Choice -> item.alternative.flatMap { getItems(it, desiredDepth, context, done + item) }
                    is Concatenation -> {
                        var items = getItems(item.items[0], desiredDepth, context, done + item)
                        var index = 1
                        while (index < item.items.size && items.any { it == null }) {
                            items = items.mapNotNull { it } + getItems(item.items[index], desiredDepth, context, done + item)
                            index++
                        }
                        items
                    }

                    is Terminal -> provideForTerminal(item)

                    is NonTerminal -> {
                        //TODO: handle overridden vs embedded rules!
                        val refRule = item.referencedRuleOrNull(this.targetGrammar)
                        when (refRule) {
                            null -> emptyList()
                            else -> getItems(refRule.rhs, desiredDepth - 1, context, done + item)
                        }
                    }

                    is SeparatedList -> {
                        val items = getItems(item.item, desiredDepth, context, done + item)
                        if (item.min == 0) {
                            items + listOf(null)
                        } else {
                            items + emptyList<CompletionItem>()
                        }
                    }

                    is SimpleList -> {
                        val items = getItems(item.item, desiredDepth, context, done + item)
                        if (item.min == 0) {
                            items + listOf(null)
                        } else {
                            items + emptyList<CompletionItem>()
                        }
                    }

                    is Group -> getItems(item.groupedContent, desiredDepth, context, done + item)
                    else -> error("not yet supported!")
                }
            }
        }
     */
}