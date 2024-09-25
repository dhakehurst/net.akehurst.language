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
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeNamespaceSimple
import net.akehurst.language.grammar.asm.GrammarReferenceDefault
import net.akehurst.language.grammar.asm.NonTerminalDefault
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.api.*
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

class CompletionProviderSimple(
    val targetGrammar: Grammar,
    val grammar2TypeModel: Grammar2TypeModelMapping,
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModel
) : CompletionProviderAbstract<Asm, ContextAsmSimple>() {

    val targetNamespace = typeModel.findNamespaceOrNull(targetGrammar.qualifiedName) as GrammarTypeNamespaceSimple?
        ?: error("Namespace not found for grammar '${targetGrammar.qualifiedName}'")

    override fun provide(nextExpected: Set<Spine>, context: ContextAsmSimple?, options: Map<String, Any>): List<CompletionItem> {
        return if (null == context || context.isEmpty || crossReferenceModel.isEmpty) {
            nextExpected.flatMap { sp -> provideTerminalsForSpine(sp) }.toSet().toList() //TODO: can we remove duplicates earlier!
        } else {
            val items = nextExpected.flatMap { sp ->
                val spri = sp.elements.firstOrNull()
                when (spri) {
                    null -> provideTerminalsForSpine(sp)
                    else -> {
                        val type = typeFor(spri)
                        when (type) {
                            null -> sp.expectedNextItems.flatMap { provideForRuleItem(it, 2, context) }
                            else -> provideForType(type, sp.nextChildNumber, spri, sp.expectedNextItems, context)
                        }
                    }
                }
            }
            items.toSet().toList() //TODO: can we remove duplicates earlier!
        }
    }

    fun typeFor(ruleItem: RuleItem): TypeInstance? {
        val rule = ruleItem.owningRule
        return targetNamespace.findTypeForRule(rule.name)
    }

    private fun provideForType(type: TypeInstance, nextChildNumber: Int, ri: RuleItem, expectedNextItems: Set<RuleItem>, context: ContextAsmSimple): List<CompletionItem> {
        val prop = type.declaration.getPropertyByIndexOrNull(nextChildNumber)
        val expectedPropName = expectedNextItems.map {
            val ri = when {
                it.owningRule.isLeaf -> NonTerminalDefault(GrammarReferenceDefault(targetGrammar.namespace, targetGrammar.name), it.owningRule.name)
                it is TangibleItem -> it
                else -> TODO()
            }
            //val tiType = targetNamespace.findTypeUsageForRule()
            val tiType = SimpleTypeModelStdLib.String.declaration
            grammar2TypeModel.propertyNameFor(targetGrammar, ri, tiType)
        }
        return when (prop) {
            null -> TODO()
            else -> {
                val refTypeNames = expectedPropName.flatMap {
                    crossReferenceModel.referenceForProperty(prop.typeInstance.qualifiedTypeName, it)
                }
                val refTypes = refTypeNames.mapNotNull { typeModel.findByQualifiedNameOrNull(it) }
                val items = refTypes.flatMap { refType ->
                    context.rootScope.findItemsConformingTo {
                        val itemType = typeModel.findFirstByPossiblyQualifiedOrNull(it) ?: SimpleTypeModelStdLib.NothingType.declaration
                        itemType.conformsTo(refType)
                    }
                }
                items.map {
                    CompletionItem(CompletionItemKind.REFERRED, it.referableName, it.qualifiedTypeName.last.value)
                }
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
}