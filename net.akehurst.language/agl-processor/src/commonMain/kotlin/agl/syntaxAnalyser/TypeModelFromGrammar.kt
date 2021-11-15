/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.typeModel.*

internal class TypeModelFromGrammar(
    private val _grammar: Grammar
) {

    companion object {
        const val UNNAMED_STRING_PROPERTY_NAME = "\$value"
        const val UNNAMED_LIST_VALUE = "\$value"
    }

    private val _ruleToType = mutableMapOf<Rule, RuleType>()
    private val _typeModel = TypeModel()
    private val _typeForRuleItem = mutableMapOf<RuleItem, RuleType>()

    internal fun derive(): TypeModel {
        for (rule in _grammar.allRule) {
            if (rule.isSkip.not() && rule.isLeaf.not()) {
                typeForRhs(rule)
            }
        }
        return _typeModel
    }

    private fun typeForRhs(rule: Rule): RuleType {
        val type = _ruleToType[rule]
        return if (null != type) {
            type // return the type if it exists, also stops infinite recursion
        } else {
            val rhs = rule.rhs
            val ruleType = when (rhs) {
                // rhs's are only ever these things (currently)
                is EmptyRule -> BuiltInType.NOTHING
                is Choice -> {
                    // if all rhs gives ElementType then this ruleType is a super type of all rhs
                    // else rhs maps to properties
                    val choiceType = findOrCreateElementType(rule.name)
                    val subtypes = rhs.alternative.map { typeForRuleItem(it) }
                    when {
                        subtypes.all { it is ElementType } -> {
                            subtypes.forEach {
                                (it as ElementType).superType.add(choiceType)
                            }
                            choiceType
                        }
                        subtypes.all { it === BuiltInType.STRING } -> {
                            PropertyDeclaration(choiceType, UNNAMED_STRING_PROPERTY_NAME, BuiltInType.STRING)
                            choiceType
                        }
                        subtypes.all { it === BuiltInType.LIST } -> BuiltInType.LIST
                        else -> TODO()
                    }
                }
                is Concatenation -> typeForConcatenation(rule.name, rhs.items)
                else -> error("Internal error, unhandled subtype of rule.rhs")
            }
            _ruleToType[rule] = ruleType
            ruleType
        }
    }

    private fun typeForRuleItem(ruleItem: RuleItem): RuleType {
        val type = _typeForRuleItem[ruleItem]
        return if (null!=type) {
            type
        } else {
            val ruleType = when (ruleItem) {
                is EmptyRule -> BuiltInType.NOTHING
                is Terminal -> BuiltInType.STRING
                is SimpleList -> when (ruleItem.max) {
                    1 -> typeForRuleItem(ruleItem.item) //TODO: nullable
                    else -> BuiltInType.LIST //TODO: add list type
                }
                is SeparatedList -> when (ruleItem.max) {
                    1 -> typeForRuleItem(ruleItem.item) //TODO: nullable //unlikely!
                    else -> BuiltInType.LIST //TODO: add list type
                }
                is NonTerminal -> when {
                    ruleItem.referencedRule.isLeaf -> BuiltInType.STRING
                    ruleItem.referencedRule.rhs is EmptyRule -> BuiltInType.NOTHING
                    ruleItem.referencedRule.rhs is Choice -> typeForChoice("??", (ruleItem.referencedRule.rhs as Choice).alternative)
                    else -> typeForRhs(ruleItem.referencedRule)
                }
                is Concatenation -> when {
                    1 == ruleItem.items.size -> typeForRuleItem(ruleItem.items[0])
                    else -> TODO()
                }
                is Group -> when {
                    1 == ruleItem.choice.alternative.size -> {
                        val concat = ruleItem.choice.alternative[0]
                        val unnamedName = newUnnamed(ruleItem.owningRule)
                        typeForConcatenation(unnamedName, concat.items)
                    }
                    else -> {
                        TODO()
                    }
                }
                else -> error("Internal error, unhandled subtype of RuleItem")
            }
            _typeForRuleItem[ruleItem] = ruleType
            ruleType
        }
    }

    private fun newUnnamed(owningRule: Rule): String {
        TODO()
    }

    private fun typeForConcatenation(name: String, items: List<ConcatenationItem>): ElementType {
        val concatType = findOrCreateElementType(name)
        items.forEach {
            createPropertyDeclaration(concatType, it)
        }
        return concatType
    }

    private fun typeForChoice(name: String, alternative: List<Concatenation>): RuleType {
        // if all rhs gives ElementType then this ruleType is a super type of all rhs
        // else rhs maps to properties
        val subtypes = alternative.map { typeForRuleItem(it) }
        return when {
            subtypes.all { it is ElementType } -> {
                val choiceType = findOrCreateElementType(name)
                subtypes.forEach {
                    (it as ElementType).superType.add(choiceType)
                }
                choiceType
            }
            subtypes.all { it === net.akehurst.language.api.typeModel.BuiltInType.STRING } -> BuiltInType.STRING
            subtypes.all { it === net.akehurst.language.api.typeModel.BuiltInType.LIST } -> BuiltInType.LIST
            else -> TODO()
        }
    }

    private fun findOrCreateElementType(name: String): ElementType {
        val rt = _typeModel.findOrCreateType(name) as ElementType
        return rt
    }

    private fun createPropertyDeclaration(et: ElementType, ruleItem: ConcatenationItem): PropertyDeclaration? {
        val propType = typeForRuleItem(ruleItem)
        return when (ruleItem) {
            is EmptyRule -> null
            is Terminal -> null //PropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is NonTerminal -> PropertyDeclaration(et, ruleItem.name, propType)
            is SimpleList -> PropertyDeclaration(et, propertyNameFor(ruleItem.item), propType)
            is SeparatedList -> PropertyDeclaration(et, propertyNameFor(ruleItem.item), propType)
            is Group -> TODO()
            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun propertyNameFor(ruleItem: SimpleItem): String = when (ruleItem) {
        is EmptyRule -> error("should not happen")
        is Terminal -> UNNAMED_STRING_PROPERTY_NAME
        is NonTerminal -> ruleItem.name
        is Group -> TODO()
        else -> error("Internal error, unhandled subtype of SimpleItem")
    }
}