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
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.collections.transitiveClosure
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.GrammarReferenceDefault
import net.akehurst.language.grammar.asm.NonTerminalDefault
import net.akehurst.language.grammarTypemodel.asm.GrammarTypeNamespaceSimple
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.typemodel.api.CollectionType
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.StructuredType
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.firstOrNull
import kotlin.collections.flatMap
import kotlin.collections.flatten
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import kotlin.collections.toList
import kotlin.collections.toSet
import kotlin.error

class CompletionProviderSimple(
    val targetGrammar: Grammar,
    val grammar2TypeModel: Grammar2TypeModelMapping,
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModel
) : CompletionProviderAbstract<Asm, ContextWithScope<Any, Any>>() {

    val targetNamespace = typeModel.findNamespaceOrNull(targetGrammar.qualifiedName) as GrammarTypeNamespaceSimple?
        ?: error("Namespace not found for grammar '${targetGrammar.qualifiedName}'")

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextWithScope<Any, Any>>): List<CompletionItem> {
        val context = options.context
        val result = if (null == context) {// || context.isEmpty || crossReferenceModel.isEmpty) {
            nextExpected.flatMap { sp -> provideDefaultForSpine(sp,options) }.toSet()
                .toList() //TODO: can we remove duplicates earlier!
        } else {
            val items = nextExpected.flatMap { sp ->
                val firstSpineNode = sp.elements.firstOrNull()
                when (firstSpineNode) {
                    null -> provideDefaultForSpine(sp,options)
                    else -> {
                        val type = typeFor(firstSpineNode.rule)
                        when (type) {
                            null -> provideDefaultForSpine(sp,options)
                            else -> provideForType(type, firstSpineNode, context) + provideDefaultForSpine(sp,options)
                        }
                    }
                }
            }
            items.toSet().toList() //TODO: can we remove duplicates earlier!
        }
        return defaultSortAndFilter(result)
    }

    fun typeFor(rule: GrammarRule): TypeInstance? = targetNamespace.findTypeForRule(rule.name)


    private fun provideForType(type: TypeInstance, firstSpineNode: SpineNode, context: ContextWithScope<Any, Any>): List<CompletionItem> {
        val prop = type.resolvedDeclaration.getOwnedPropertyByIndexOrNull(firstSpineNode.nextChildNumber)
        //TODO: lists ?
        return when (prop) {
            null -> emptyList()
            else -> {
                var strProps: Set<PropertyDeclaration> = setOf(prop)
                while (strProps.isNotEmpty() && strProps.all { it.typeInstance != StdLibDefault.String }) {
                    strProps = strProps.filter { it.typeInstance == StdLibDefault.String }.toSet() +
                            strProps.filter { it.typeInstance != StdLibDefault.String }.flatMap { prp ->
                                val type = prp.typeInstance
                                val td = type.resolvedDeclaration
                                when (td) {
                                    is CollectionType -> firstPropertyOf(type.typeArguments[0].type)
                                    is StructuredType -> firstPropertyOf(type)
                                    else -> emptyList()
                                }
                            }.toSet()
                }
                strProps.flatMap { prp ->
                    val refTypeNames = crossReferenceModel.referenceForProperty(prp.owner.qualifiedName, prp.name.value)
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
            }
        }
    }

    private fun provideForType1(type: TypeInstance, firstSpineNode: SpineNode, context: ContextWithScope<Any, Any>): List<CompletionItem> {
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

    private fun firstPropertyOf(type: TypeInstance): List<PropertyDeclaration> {
        val typesClosure = setOf(type).transitiveClosure { it.resolvedDeclaration.subtypes.toSet() }
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