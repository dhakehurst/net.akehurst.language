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

import net.akehurst.language.agl.agl.grammar.grammar.PseudoRuleNames
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.typeModel.*

class TypeModelFromGrammar(
    val grammar: Grammar,
    val configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration
) : TypeModel {

    companion object {
        const val UNNAMED_PRIMITIVE_PROPERTY_NAME = "\$value"
        const val UNNAMED_LIST_PROPERTY_NAME = "\$list"
        const val UNNAMED_TUPLE_PROPERTY_NAME = "\$tuple"
        const val UNNAMED_GROUP_PROPERTY_NAME = "\$group"

        fun configure(init: TypeModelFromGrammarConfigurationBuilder.() -> Unit): TypeModelFromGrammarConfiguration {
            val b = TypeModelFromGrammarConfigurationBuilder()
            b.init()
            return b.build()
        }

        fun String.lower()=when {
            this.uppercase()==this -> this.lowercase()
            else -> this.replaceFirstChar { it.lowercase() }
        }

        val defaultConfiguration = configure {
            typeNameFor { rule -> rule.name.replaceFirstChar { it.titlecase() } }
            propertyNameFor { ruleItem, ruleItemType ->
                val baseName = when (ruleItem) {
                    is Terminal -> when (ruleItemType) {
                        is StringType -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                        is ListSimpleType -> UNNAMED_LIST_PROPERTY_NAME
                        is ListSeparatedType -> UNNAMED_LIST_PROPERTY_NAME
                        is TupleType -> UNNAMED_TUPLE_PROPERTY_NAME
                        else -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                    }

                    is Embedded -> ruleItem.embeddedGoalName.lower()
                    is NonTerminal -> ruleItem.name.lower()
                    is Group -> UNNAMED_GROUP_PROPERTY_NAME
                    else -> error("Internal error, unhandled subtype of SimpleItem")
                }.replaceFirstChar { it.lowercase() }
                when (ruleItemType) {
                    is NothingType -> baseName
                    is AnyType -> baseName
                    is StringType -> baseName
                    is UnnamedSuperTypeType -> baseName
                    is ListSimpleType -> when(ruleItem) {
                        is NonTerminal -> ruleItem.name.lower()
                        is Terminal -> UNNAMED_LIST_PROPERTY_NAME
                        is Group -> UNNAMED_LIST_PROPERTY_NAME
                        else -> "${baseName}List"
                    }
                    is ListSeparatedType -> when(ruleItem) {
                        is NonTerminal -> ruleItem.name.lower()
                        is Terminal -> UNNAMED_LIST_PROPERTY_NAME
                        else -> "${baseName}List"
                    }
                    is TupleType -> baseName
                    is ElementType -> baseName
                }
            }
        }
    }

    // GrammarRule.name -> ElementType
    private val _ruleToType = mutableMapOf<String, RuleType>()
    private val _typeForRuleItem = mutableMapOf<RuleItem, RuleType>()
    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredRuleType, String>, Int>()
    private val _pseudoRuleNameGenerator = PseudoRuleNames(grammar)

    override val namespace: String get() = grammar.namespace.qualifiedName
    override val name: String get() = grammar.name

    override val types: Map<String, RuleType> by lazy {
        grammar.allResolvedGrammarRule
            .filter { it.isLeaf.not() && it.isSkip.not() }
            .associateBy({ configuration?.typeNameFor?.invoke(it)?:it.name }) {
                typeForRhs(it) as RuleType
            }
    }

    override fun findType(name: String): RuleType {
        return when (val type = _ruleToType[name]) {
            null -> {
                when (val rule = this.grammar.findNonTerminalRule(name)) {
                    null -> {
                        //Maybe a pseudoRule
                        val pseudoRuleItem = _pseudoRuleNameGenerator.itemForPseudoRuleName(name)
                        typeForRuleItem(pseudoRuleItem)
                    }

                    else -> typeForRhs(rule)
                }
            }

            else -> type
        }
    }

    private fun stringTypeForRuleName(name: String): StringType {
        val existing = _ruleToType[name]
        return if (null == existing) {
            val type = StringType
            _ruleToType[name] = type //halt recursion
            type
        } else {
            existing as StringType
        }
    }

    private fun findOrCreateElementType(rule: GrammarRule, ifCreate:(ElementType)->Unit): ElementType {
        val ruleName = rule.name
        val existing = _ruleToType[ruleName]
        return if (null == existing) {
            val elTypeName = configuration?.typeNameFor?.invoke(rule) ?: ruleName
            val type = ElementType(this, elTypeName)
            _ruleToType[ruleName] = type //halt recursion
            ifCreate.invoke(type)
            type
        } else {
            existing as ElementType
        }
    }

    /*
        private fun createElementType(name: String): ElementType {
            val existing = _ruleToType[name]
            return if (null == existing) {
                val type = ElementType(this, name)
                _ruleToType[name] = type //halt recursion
                type
            } else {
                error("Internal Error: created duplicate ElementType for '$name'")
            }
        }
    */
    private fun findElementType(rule: GrammarRule): ElementType? = _ruleToType[name] as ElementType?

    private fun typeForRhs(rule: GrammarRule): RuleType {
        val type = _ruleToType[rule.name]
        return if (null != type) {
            type // return the type if it exists, also stops infinite recursion
        } else {
            val rhs = rule.rhs
            val ruleType = when (rhs) {
                // rhs's are only ever these things (currently)
                is EmptyRule -> findOrCreateElementType(rule) {}
                is Choice -> typeForChoiceRule(rule)
                is Concatenation -> typeForConcatenation(rule, rhs.items)
                is SimpleList -> when (rhs.max) {
                    1 -> typeForRuleItem(rhs.item) //no need for nullable, when min is 0 we get empty list
                    else -> ListSimpleType(typeForRuleItem(rhs.item)) //PrimitiveType.LIST //TODO: add list type
                }

                is SeparatedList -> when (rhs.max) {
                    1 -> typeForRuleItem(rhs.item) //no need for nullable, when min is 0 we get empty list
                    else -> {
                        val itemType = typeForRuleItem(rhs.item)
                        val sepType = typeForRuleItem(rhs.separator)
                        ListSeparatedType(itemType, sepType)
                    }
                }

                else -> error("Internal error, unhandled subtype of rule '${rule.name}'.rhs '${rhs::class.simpleName}' when getting TypeModel from grammar '${this.grammar.qualifiedName}'")
            }
            if (ruleType is ElementType) {
                _ruleToType[rule.name] = ruleType
            }
            ruleType
        }
    }

    private fun typeForRuleItem(ruleItem: RuleItem): RuleType {
        val type = _typeForRuleItem[ruleItem]
        return if (null != type) {
            type
        } else {
            val ruleType = when (ruleItem) {
                is EmptyRule -> NothingType
                is Terminal -> StringType
                is SimpleList -> when (ruleItem.max) {
                    1 -> typeForRuleItem(ruleItem.item) //no need for nullable, when min is 0 we get empty list
                    else -> ListSimpleType(typeForRuleItem(ruleItem.item)) //PrimitiveType.LIST //TODO: add list type
                }

                is SeparatedList -> when (ruleItem.max) {
                    1 -> typeForRuleItem(ruleItem.item) //no need for nullable, when min is 0 we get empty list
                    else -> {
                        val itemType = typeForRuleItem(ruleItem.item)
                        val sepType = typeForRuleItem(ruleItem.separator)
                        ListSeparatedType(itemType, sepType)
                    }
                }

                is Embedded -> {
                    val embTmfg = TypeModelFromGrammar(ruleItem.embeddedGrammarReference.resolved!!) //TODO: check for null
                    val embTm = embTmfg
                    embTm.findType(ruleItem.name) ?: error("Should never happen")
                }

                is NonTerminal -> when {
                    ruleItem.referencedRule(this.grammar).isLeaf -> StringType
                    ruleItem.referencedRule(this.grammar).rhs is EmptyRule -> NothingType
                    ruleItem.referencedRule(this.grammar).rhs is Choice -> {
                        val r = ruleItem.referencedRule(this.grammar)
                        typeForChoiceRule(r) //r.name, (r.rhs as Choice).alternative)
                    }

                    else -> typeForRhs(ruleItem.referencedRule(this.grammar))
                }

                is Concatenation -> when {
                    1 == ruleItem.items.size -> typeForRuleItem(ruleItem.items[0])
                    else -> {
                        val concatType = TupleType()
                        this._typeForRuleItem[ruleItem] = concatType
                        ruleItem.items.forEachIndexed { idx, it ->
                            createPropertyDeclaration(concatType, it, idx)
                        }
                        when {
                            concatType.property.isEmpty() -> NothingType
                            else -> concatType
                        }
                    }
                }

                is Group -> populateTypeForChoice(ruleItem.choice, null)

                else -> error("Internal error, unhandled subtype of RuleItem")
            }
            _typeForRuleItem[ruleItem] = ruleType
            ruleType
        }
    }

    private fun typeForConcatenation(rule: GrammarRule, items: List<ConcatenationItem>): StructuredRuleType {
        val concatType = findOrCreateElementType(rule) {newType ->
            items.forEachIndexed { idx, it -> createPropertyDeclaration(newType, it, idx) }
        }
        return concatType
    }

    private fun typeForChoiceRule(choiceRule: GrammarRule): RuleType { //name: String, alternative: List<Concatenation>): RuleType {
        val t = populateTypeForChoice(choiceRule.rhs as Choice, choiceRule)
        return t
    }

    private fun populateTypeForChoice(choice: Choice, choiceRule: GrammarRule?): RuleType {
        // if all choice gives ElementType then this type is a super type of all choices
        // else choices maps to properties
        val subtypes = choice.alternative.map { typeForRuleItem(it) }
        return when {
            subtypes.all { it is NothingType } -> NothingType
            subtypes.all { it is StringType } -> StringType

            subtypes.all { it is ElementType } -> when {
                null == choiceRule -> UnnamedSuperTypeType(subtypes)
                else -> {
                    val choiceType = findOrCreateElementType(choiceRule) {newType ->
                        subtypes.forEach {
                            (it as ElementType).addSuperType(newType)
                        }
                    }
                    choiceType
                }
            }

            subtypes.all { it is ListSimpleType } -> { //=== PrimitiveType.LIST } -> {
                val choiceType = ListSimpleType(AnyType) //TODO: elementTypes ?
                choiceType
            }

            subtypes.all { it is TupleType } -> when {
                1 == subtypes.map { (it as TupleType).property.values.map { Pair(it.name, it.type) }.toSet() }.toSet().size -> subtypes.first()
                else -> UnnamedSuperTypeType(subtypes)
            }

            else -> UnnamedSuperTypeType(subtypes)
        }
    }

    private fun createPropertyDeclaration(et: StructuredRuleType, ruleItem: ConcatenationItem, childIndex: Int) {
        when (ruleItem) {
            is EmptyRule -> Unit
            is Terminal -> Unit //createUniquePropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is Embedded -> {
                val refRule = ruleItem.referencedRule(ruleItem.embeddedGrammarReference.resolved!!) //TODO: check for null
                createPropertyDeclarationForReferencedRule(refRule, et, ruleItem, childIndex)
            }

            is NonTerminal -> {
                val refRule = ruleItem.referencedRule(this.grammar)
                createPropertyDeclarationForReferencedRule(refRule, et, ruleItem, childIndex)
            }

            is SimpleList -> {
                val isNullable = ruleItem.min == 0 && ruleItem.max == 1
                val t = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t), t, isNullable, childIndex)
            }

            is SeparatedList -> {
                val t = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t), t, false, childIndex)
            }

            is Group -> {
                val choiceType = populateTypeForChoice(ruleItem.choice, null)
                if (choiceType is NothingType) {
                    Unit
                } else {
                    val pName = propertyNameFor(et, ruleItem, choiceType)
                    createUniquePropertyDeclaration(et, pName, choiceType, false, childIndex)
                }
            }

            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun createPropertyDeclarationForReferencedRule(refRule: GrammarRule, et: StructuredRuleType, ruleItem: SimpleItem, childIndex: Int) {
        val rhs = refRule.rhs
        when (rhs) {
            is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, StringType), StringType, false, childIndex)
            is Concatenation -> when {
                1 == rhs.items.size -> when (rhs.items[0]) {
                    is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, StringType), StringType, false, childIndex)
                    is ListOfItems -> {
                        val propType = typeForRuleItem(rhs) //to get list type
                        val isNullable = (rhs.items[0] as ListOfItems).min == 0 && (rhs.items[0] as ListOfItems).min == -1
                        createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType), propType, isNullable, childIndex)
                    }

                    else -> {
                        val t = typeForRuleItem(ruleItem)
                        createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, t), t, false, childIndex)
                    }
                }

                else -> {
                    val t = typeForRuleItem(ruleItem)
                    createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, t), t, false, childIndex)
                }
            }

            is Choice -> {
                val choiceType = typeForChoiceRule(refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType)
                createUniquePropertyDeclaration(et, pName, choiceType, false, childIndex)
            }

            else -> {
                val propType = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType), propType, false, childIndex)
            }
        }
    }

    private fun propertyNameFor(et: StructuredRuleType, ruleItem: SimpleItem, ruleItemType: RuleType): String {
        return when (configuration) {
            null -> when (ruleItem) {
                is EmptyRule -> error("should not happen")
                is Terminal -> when (ruleItemType) {
                    is StringType -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                    is ListSimpleType -> UNNAMED_LIST_PROPERTY_NAME
                    is ListSeparatedType -> UNNAMED_LIST_PROPERTY_NAME
                    is TupleType -> UNNAMED_TUPLE_PROPERTY_NAME
                    else -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                }

                is Embedded -> ruleItem.embeddedGoalName
                is NonTerminal -> ruleItem.name
                is Group -> UNNAMED_GROUP_PROPERTY_NAME
                else -> error("Internal error, unhandled subtype of SimpleItem")
            }

            else -> configuration.propertyNameFor.invoke(ruleItem, ruleItemType)
        }
    }

    private fun createUniquePropertyDeclaration(et: StructuredRuleType, name: String, type: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration? {
        val uniqueName = createUniquePropertyNameFor(et, name)
        return PropertyDeclaration(et, uniqueName, type, isNullable, childIndex)
    }

    private fun createUniquePropertyNameFor(et: StructuredRuleType, name: String): String {
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


    override fun toString(): String = "TypeModel(${grammar.qualifiedName})"
}