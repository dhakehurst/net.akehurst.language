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
        const val UNNAMED_ANY_PROPERTY_NAME = "\$value"
        const val UNNAMED_STRING_PROPERTY_NAME = "\$value"
        const val UNNAMED_LIST_PROPERTY_VALUE = "\$value"
        const val UNNAMED_GROUP_PROPERTY_NAME = "\$group"
    }

    private val _ruleToType = mutableMapOf<Rule, RuleType>()
    private val _typeModel = TypeModel()
    private val _typeForRuleItem = mutableMapOf<RuleItem, RuleType>()
    private val _uniquePropertyNames = mutableMapOf<Pair<ElementType, String>, Int>()

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
                is EmptyRule -> findOrCreateElementType(rule.name)
                is Choice -> {
                    // if all rhs gives ElementType then this ruleType is a super type of all rhs
                    // else rhs maps to properties
                    val choiceType = findOrCreateElementType(rule.name)
                    val subtypes = rhs.alternative.map { typeForRuleItem(it) }
                    when {
                        subtypes.all { it is ElementType } -> {
                            subtypes.forEach {
                                (it as ElementType).addSuperType(choiceType)
                            }
                            choiceType
                        }
                        subtypes.all { it === BuiltInType.STRING } -> {
                            createUniquePropertyDeclaration(choiceType, UNNAMED_STRING_PROPERTY_NAME, BuiltInType.STRING, false, 0)
                            choiceType
                        }
                        subtypes.all { it === BuiltInType.LIST } -> {
                            createUniquePropertyDeclaration(choiceType, UNNAMED_LIST_PROPERTY_VALUE, BuiltInType.LIST, false, 0)
                            choiceType
                        }
                        else -> BuiltInType.ANY
                    }
                }
                is Concatenation -> {
                    typeForConcatenation(rule, rhs.items)
                }
                else -> error("Internal error, unhandled subtype of rule.rhs")
            }
            _ruleToType[rule] = ruleType
            ruleType
        }
    }

    private fun typeForRuleItem(ruleItem: RuleItem): RuleType {
        val type = _typeForRuleItem[ruleItem]
        return if (null != type) {
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
                    ruleItem.embedded -> {
                            val embTmfg = TypeModelFromGrammar(ruleItem.owningGrammar)
                            val embTm = embTmfg.derive()
                            embTm.findType(ruleItem.name) ?: error("Should never happen")
                    }
                    ruleItem.referencedRule(this._grammar).isLeaf -> BuiltInType.STRING
                    ruleItem.referencedRule(this._grammar).rhs is EmptyRule -> BuiltInType.NOTHING
                    ruleItem.referencedRule(this._grammar).rhs is Choice -> typeForChoice("??", (ruleItem.referencedRule(this._grammar).rhs as Choice).alternative)
                    else -> typeForRhs(ruleItem.referencedRule(this._grammar))
                }
                is Concatenation -> when {
                    1 == ruleItem.items.size -> typeForRuleItem(ruleItem.items[0])
                    else -> TODO()
                }
                is Group -> when {
                    1 == ruleItem.choice.alternative.size -> {
                        val concat = ruleItem.choice.alternative[0]
                        val unnamedName = newUnnamed(ruleItem.owningRule)
                        val concatType = findOrCreateElementType(unnamedName)
                        this._typeForRuleItem[ruleItem] = concatType
                        concat.items.forEachIndexed { idx, it ->
                            createPropertyDeclaration(concatType, it, idx)
                        }
                         concatType
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

    private fun typeForConcatenation(rule: Rule, items: List<ConcatenationItem>): ElementType {
        val concatType = findOrCreateElementType(rule.name)
        this._ruleToType[rule] = concatType
        items.forEachIndexed { idx, it ->
            createPropertyDeclaration(concatType, it, idx)
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
                    (it as ElementType).addSuperType(choiceType)
                }
                choiceType
            }
            subtypes.all { it === net.akehurst.language.api.typeModel.BuiltInType.STRING } -> BuiltInType.STRING
            subtypes.all { it === net.akehurst.language.api.typeModel.BuiltInType.LIST } -> BuiltInType.LIST
            else -> {
                val choiceType = findOrCreateElementType(name)
                createUniquePropertyDeclaration(choiceType, UNNAMED_ANY_PROPERTY_NAME,BuiltInType.ANY, false,0)
                choiceType
            }
        }
    }

    private fun findOrCreateElementType(name: String): ElementType {
        val rt = _typeModel.findOrCreateType(name) as ElementType
        return rt
    }

    private fun createPropertyDeclaration(et: ElementType, ruleItem: ConcatenationItem, childIndex: Int): PropertyDeclaration? {
        return when (ruleItem) {
            is EmptyRule -> null
            is Terminal -> null //createUniquePropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is NonTerminal -> {
                    val rhs = if (ruleItem.embedded) {
                        ruleItem.referencedRule(ruleItem.owningGrammar).rhs
                    } else {
                        ruleItem.referencedRule(this._grammar).rhs
                    }
                    when (rhs) {
                        is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), BuiltInType.STRING, false, childIndex)
                        is Concatenation -> when {
                            1 == rhs.items.size -> when (rhs.items[0]) {
                                is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), BuiltInType.STRING, false, childIndex)
                                is ListOfItems -> {
                                    val propType = typeForRuleItem(rhs) //to get list type
                                    val isNullable = (rhs.items[0] as ListOfItems).min == 0 && (rhs.items[0] as ListOfItems).min == -1
                                    createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), propType, isNullable, childIndex)
                                }
                                else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), typeForRuleItem(ruleItem), false, childIndex)
                            }
                            else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), typeForRuleItem(ruleItem), false, childIndex)
                        }
                        is Choice -> {
                            val pName = propertyNameFor(et, ruleItem)
                            val choiceType = typeForChoice(pName, rhs.alternative)
                            createUniquePropertyDeclaration(et, pName, choiceType, false, childIndex)
                        }
                        else -> {
                            val propType = typeForRuleItem(ruleItem)
                            createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), propType, false, childIndex)
                        }
                    }
            }
            is SimpleList -> {
                val isNullable = ruleItem.min==0 && ruleItem.max==1
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item), typeForRuleItem(ruleItem), isNullable, childIndex)
            }
            is SeparatedList -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item), typeForRuleItem(ruleItem),false,  childIndex)
            is Group -> TODO()
            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun propertyNameFor(et: ElementType, ruleItem: SimpleItem): String = when (ruleItem) {
        is EmptyRule -> error("should not happen")
        is Terminal -> UNNAMED_STRING_PROPERTY_NAME
        is NonTerminal -> ruleItem.name
        is Group -> createUniquePropertyNameFor(et, UNNAMED_GROUP_PROPERTY_NAME)
        else -> error("Internal error, unhandled subtype of SimpleItem")
    }

    private fun createUniquePropertyDeclaration(et: ElementType, name: String, type: RuleType, isNullable:Boolean, childIndex: Int): PropertyDeclaration? {
        val uniqueName = createUniquePropertyNameFor(et,name)
        return PropertyDeclaration(et, uniqueName, type, isNullable, childIndex)
    }

    private fun createUniquePropertyNameFor(et: ElementType, name: String): String {
        val nameCount = this._uniquePropertyNames[Pair(et, name)]
        val uniqueName = if (null == nameCount) {
            this._uniquePropertyNames[Pair(et, name)] = 2
            name
        } else {
            this._uniquePropertyNames[Pair(et, name)] = nameCount + 1
            "$name$nameCount"
        }
        return uniqueName
    }

}