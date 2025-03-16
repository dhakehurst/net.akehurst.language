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
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.GrammarReferenceDefault
import net.akehurst.language.grammar.asm.NonTerminalDefault
import net.akehurst.language.grammarTypemodel.asm.GrammarTypeNamespaceSimple
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.typemodel.api.PropertyDeclaration
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
) : CompletionProviderAbstract<Asm, ContextAsmSimple>() {

    val targetNamespace = typeModel.findNamespaceOrNull(targetGrammar.qualifiedName) as GrammarTypeNamespaceSimple?
        ?: error("Namespace not found for grammar '${targetGrammar.qualifiedName}'")

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextAsmSimple>): List<CompletionItem> {
        val depth = options.depth
        val context = options.context
        val result = if (null == context) {// || context.isEmpty || crossReferenceModel.isEmpty) {
            nextExpected.flatMap { sp -> provideDefault(depth,sp) }.toSet()
                .toList() //TODO: can we remove duplicates earlier!
        } else {
            val items = nextExpected.flatMap { sp ->
                val firstSpineNode = sp.elements.firstOrNull()
                when (firstSpineNode) {
                    null -> provideDefault(depth,sp)
                    else -> {
                        val type = typeFor(firstSpineNode.rule)
                        when (type) {
                            null -> provideDefault(depth,sp)
                            else -> provideForType(type, firstSpineNode, context) + provideDefault(depth,sp)
                        }
                    }
                }
            }
            items.toSet().toList() //TODO: can we remove duplicates earlier!
        }
        return defaultSortAndFilter(result)
    }

    fun typeFor(rule: GrammarRule): TypeInstance? = targetNamespace.findTypeForRule(rule.name)


    private fun provideForType(type: TypeInstance, firstSpineNode: SpineNode, context: ContextAsmSimple): List<CompletionItem> {
        val prop = type.resolvedDeclaration.getOwnedPropertyByIndexOrNull(firstSpineNode.nextChildNumber)
        //TODO: lists ?
        return when (prop) {
            null -> emptyList()
            else -> {
                var strProp: PropertyDeclaration = prop
                while(strProp.typeInstance != StdLibDefault.String) {
                    strProp = firstPropertyOf(strProp.typeInstance)
                }
                val refTypeNames = crossReferenceModel.referenceForProperty(strProp.typeInstance.qualifiedTypeName, strProp.name.value)
                val refTypes = refTypeNames.mapNotNull { typeModel.findByQualifiedNameOrNull(it) }
                val items = refTypes.flatMap { refType ->
                    context.rootScope.findItemsConformingTo {
                        val itemType = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDeclaration
                        itemType.conformsTo(refType)
                    }
                }
                items.map {
                    CompletionItem(CompletionItemKind.REFERRED, it.qualifiedTypeName.last.value, it.referableName)
                }

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
                        context.rootScope.findItemsConformingTo {
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

    private fun provideForType2(type: TypeInstance, firstSpineNode: SpineNode, context: ContextAsmSimple): List<CompletionItem> {
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
                        context.rootScope.findItemsConformingTo {
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

    private fun firstPropertyOf(type: TypeInstance): PropertyDeclaration {
        return type.allResolvedProperty.values.minBy { it.index }
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