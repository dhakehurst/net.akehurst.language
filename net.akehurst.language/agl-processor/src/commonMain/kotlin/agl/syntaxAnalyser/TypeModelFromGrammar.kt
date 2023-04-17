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

import net.akehurst.language.agl.agl.typemodel.TypeModelAbstract
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.typemodel.*

class TypeModelFromGrammar(
    namespace: String,
    name: String
) : TypeModelAbstract(namespace, name) {
    constructor(grammar:Grammar, configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration):this(listOf(grammar),configuration)
    constructor(grammars:List<Grammar>, configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration)
            : this(grammars.last().namespace.qualifiedName, grammars.last().name) {
        this._configuration = configuration
        grammars.forEach { g ->
            this.grammar = g
            g.allResolvedGrammarRule
                .filter { it.isLeaf.not() && it.isSkip.not() }
                .forEach {
                    val key = it.name
                    val value = typeForRhs(it)
                    super.allTypes[key] = value
                }
        }
    }

    companion object {
        const val UNNAMED_PRIMITIVE_PROPERTY_NAME = "\$value"
        const val UNNAMED_LIST_PROPERTY_NAME = "\$list"
        const val UNNAMED_TUPLE_PROPERTY_NAME = "\$tuple"
        const val UNNAMED_GROUP_PROPERTY_NAME = "\$group"

        val defaultConfiguration = TypeModelFromGrammarConfigurationDefault()
    }

    // GrammarRule.name -> ElementType
    private val _ruleToType = mutableMapOf<String, RuleType>()
    private val _typeForRuleItem = mutableMapOf<RuleItem, RuleType>()
    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredRuleType, String>, Int>()
    private var _configuration:TypeModelFromGrammarConfiguration?=null
    // temp var - changes for each Grammar processed
    private lateinit var grammar: Grammar
    //private val _pseudoRuleNameGenerator = PseudoRuleNames(grammar)

    //override fun findTypeForRule(ruleName: String): RuleType? = _ruleToType[ruleName]

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

    private fun findOrCreateElementType(rule: GrammarRule, ifCreate: (ElementType) -> Unit): ElementType {
        val ruleName = rule.name
        val existing = _ruleToType[ruleName]
        return if (null == existing) {
            val elTypeName = _configuration?.typeNameFor(rule) ?: ruleName
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
    private fun findElementType(rule: GrammarRule): ElementType? = _ruleToType[rule.name] as ElementType?

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
                is Concatenation -> when (rhs.items.size) {
                    0 -> error("Should not happen")
                    1 -> when (rhs.items[0]) {
                        //is SimpleList -> when ((rhs.items[0] as SimpleList).item) {
                        //    is Terminal -> typeForConcatenation(rule, rhs.items)
                        //    else -> typeForRuleItem(rhs.items[0])
                        //}
                        //is SeparatedList -> typeForRuleItem(rhs.items[0])
                        else -> typeForConcatenation(rule, rhs.items)
                    }

                    else -> typeForConcatenation(rule, rhs.items)
                }

                is SimpleList -> when (rhs.max) {
                    1 -> typeForRuleItem(rhs.item) //no need for nullable, when min is 0 we get empty list
                    else -> {
                        val t = ListSimpleType()
                        t.elementType = typeForRuleItem(rhs.item)
                        t
                    }
                }

                is SeparatedList -> when (rhs.max) {
                    1 -> typeForRuleItem(rhs.item) //no need for nullable, when min is 0 we get empty list
                    else -> {
                        val itemType = typeForRuleItem(rhs.item)
                        val sepType = typeForRuleItem(rhs.separator)
                        val t = ListSeparatedType()
                        t.itemType = itemType
                        t.separatorType = sepType
                        t
                    }
                }

                else -> error("Internal error, unhandled subtype of rule '${rule.name}'.rhs '${rhs::class.simpleName}' when getting TypeModel from grammar '${this.qualifiedName}'")
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
                    else -> {
                        val t = ListSimpleType()
                        _typeForRuleItem[ruleItem] = t
                        t.elementType = typeForRuleItem(ruleItem.item)
                        t
                    }
                }

                is SeparatedList -> when (ruleItem.max) {
                    1 -> typeForRuleItem(ruleItem.item) //no need for nullable, when min is 0 we get empty list
                    else -> {
                        val t = ListSeparatedType()
                        _typeForRuleItem[ruleItem] = t
                        t.itemType = typeForRuleItem(ruleItem.item)
                        t.separatorType = typeForRuleItem(ruleItem.separator)
                        t
                    }
                }

                is Embedded -> {
                    val embTmfg = TypeModelFromGrammar(listOf(ruleItem.embeddedGrammarReference.resolved!!)) //TODO: check for null
                    val embTm = embTmfg
                    embTm.findTypeForRule(ruleItem.name) ?: error("Should never happen")
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
                    1 == ruleItem.items.size -> {
                        val t = typeForRuleItem(ruleItem.items[0])
                        this._typeForRuleItem[ruleItem] = t
                        t
                    }

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
        val concatType = findOrCreateElementType(rule) { newType ->
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
                    val choiceType = findOrCreateElementType(choiceRule) { newType ->
                        subtypes.forEach {
                            (it as ElementType).addSuperType(newType)
                        }
                    }
                    choiceType
                }
            }

            subtypes.all { it is ListSimpleType } -> { //=== PrimitiveType.LIST } -> {
                val choiceType = ListSimpleType()
                choiceType.elementType = AnyType //TODO: compute better elementType ?
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
                when (ruleItem.item) {
                    is Terminal -> Unit //do not add optional literals
                    else -> {
                        val isNullable = ruleItem.min == 0 && ruleItem.max == 1
                        val t = typeForRuleItem(ruleItem)
                        createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t), t, isNullable, childIndex)
                    }
                }
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
        return when (_configuration) {
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

            else -> _configuration!!.propertyNameFor(ruleItem, ruleItemType)
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

    override fun toString(): String = "TypeModel(${this.qualifiedName})"
}