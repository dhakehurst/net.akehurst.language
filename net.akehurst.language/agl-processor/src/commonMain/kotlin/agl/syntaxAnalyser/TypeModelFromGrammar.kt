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
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.typeModel.*

class TypeModelFromGrammar(
    val grammar: Grammar
) : TypeModel {

    companion object {
        const val UNNAMED_PRIMITIVE_PROPERTY_NAME = "\$value"
        const val UNNAMED_LIST_PROPERTY_NAME = "\$list"
        const val UNNAMED_TUPLE_PROPERTY_NAME = "\$tuple"
        const val UNNAMED_GROUP_PROPERTY_NAME = "\$group"
    }

    // Rule.name -> ElementType
    private val _ruleToType = mutableMapOf<String, RuleType>()
    private val _typeForRuleItem = mutableMapOf<RuleItem, RuleType>()
    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredRuleType, String>, Int>()
    private val _pseudoRuleNameGenerator = PseudoRuleNames(grammar)

    override val types: Map<String, RuleType> by lazy {
        grammar.allRule
            .filter { it.isLeaf.not() && it.isSkip.not() }
            .associateBy({ it.name }) {
                typeForRhs(it) as RuleType
            }
    }

    override fun findType(name: String): RuleType? {
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

    private fun stringTypeForRuleName(name: String): PrimitiveType {
        val existing = _ruleToType[name]
        return if (null == existing) {
            val type = PrimitiveType.STRING
            _ruleToType[name] = type //halt recursion
            type
        } else {
            existing as PrimitiveType
        }
    }

    private fun findOrCreateElementType(name: String): ElementType {
        val existing = _ruleToType[name]
        return if (null == existing) {
            val type = ElementType(name)
            _ruleToType[name] = type //halt recursion
            type
        } else {
            existing as ElementType
        }
    }

    private fun createElementType(name: String): ElementType {
        val existing = _ruleToType[name]
        return if (null == existing) {
            val type = ElementType(name)
            _ruleToType[name] = type //halt recursion
            type
        } else {
            error("Internal Error: created duplicate ElementType for '$name'")
        }
    }

    private fun findElementType(name: String): ElementType? = _ruleToType[name] as ElementType?


    //fun derive(): TypeModel {
    //    for (rule in _grammar.allRule) {
    //        if (rule.isSkip.not() && rule.isLeaf.not()) {
    //            typeForRhs(rule)
    //        }
    //    }
    //    return _typeModel
    //}

    private fun typeForRhs(rule: Rule): RuleType {
        val type = _ruleToType[rule.name]
        return if (null != type) {
            type // return the type if it exists, also stops infinite recursion
        } else {
            val rhs = rule.rhs
            val ruleType = when (rhs) {
                // rhs's are only ever these things (currently)
                is EmptyRule -> findOrCreateElementType(rule.name)
                is Choice -> typeForChoiceRule(rule)
                is Concatenation -> typeForConcatenation(rule, rhs.items)
                else -> error("Internal error, unhandled subtype of rule.rhs")
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
                is EmptyRule -> PrimitiveType.NOTHING
                is Terminal -> PrimitiveType.STRING
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
                    val embTmfg = TypeModelFromGrammar(ruleItem.embeddedGrammar)
                    val embTm = embTmfg
                    embTm.findType(ruleItem.name) ?: error("Should never happen")
                }

                is NonTerminal -> when {
                    ruleItem.referencedRule(this.grammar).isLeaf -> PrimitiveType.STRING
                    ruleItem.referencedRule(this.grammar).rhs is EmptyRule -> PrimitiveType.NOTHING
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
                            concatType.property.isEmpty() -> PrimitiveType.NOTHING
                            else -> concatType
                        }
                    }
                }

                is Group -> {//when {
                    when {
                        // one choice in the group
                        //1 == ruleItem.choice.alternative.size -> {
                        //    val concat = ruleItem.choice.alternative[0]
                        //     val concatType = TupleType()
                        //    concat.items.forEachIndexed { idx, it ->
                        //         createPropertyDeclaration(concatType, it, idx)
                        //    }
                        //    concatType
                        // }
                        // multiple choices in group
                        else -> {
                            //val choiceType = TupleType()
                            populateTypeForChoice(ruleItem.choice, UNNAMED_GROUP_PROPERTY_NAME)
                            //choiceType
                        }
                    }
                }

                else -> error("Internal error, unhandled subtype of RuleItem")
            }
            _typeForRuleItem[ruleItem] = ruleType
            ruleType
        }
    }

    private fun typeForConcatenation(rule: Rule, items: List<ConcatenationItem>): StructuredRuleType {
        val concatType = findOrCreateElementType(rule.name) as StructuredRuleType
        //this._ruleToType[rule.name] = concatType //halt recursion
        items.forEachIndexed { idx, it ->
            createPropertyDeclaration(concatType, it, idx)
        }
        return concatType
    }

    private fun typeForChoiceRule(choiceRule: Rule): RuleType { //name: String, alternative: List<Concatenation>): RuleType {
        //val choiceType = findOrCreateElementType(choiceRule.name)
        val t = populateTypeForChoice(choiceRule.rhs as Choice, choiceRule.name) as RuleType
        return when (t) {
            is PrimitiveType -> when (t) {
                PrimitiveType.ANY -> {
                    val existing = findElementType(choiceRule.name)
                    if (null == existing) {
                        val nt = createElementType(choiceRule.name)
                        createUniquePropertyDeclaration(nt, UNNAMED_PRIMITIVE_PROPERTY_NAME, t, false, 0)
                        nt
                    } else {
                        existing
                    }
                }

                PrimitiveType.NOTHING -> TODO()
                PrimitiveType.STRING -> stringTypeForRuleName(choiceRule.name)
                //PrimitiveType.STRING -> findOrCreateElementType(choiceRule.name).also {
                //    createUniquePropertyDeclaration(it, UNNAMED_PRIMITIVE_PROPERTY_NAME, t, false, 0)
                //}

                else -> error("Internal error, unhandled PrimitiveType $t")
            }

            else -> t
        }
        /*
        val subtypes = (choiceRule.rhs as Choice).alternative.map { typeForRuleItem(it) }
        return when {
            subtypes.all { it is ElementType } -> {
                val choiceType = findOrCreateElementType(choiceRule.name)
                _ruleToType[choiceRule] = choiceType
                subtypes.forEach {
                    (it as ElementType).addSuperType(choiceType)
                }
                choiceType
            }
            //subtypes.all { it === PrimitiveType.STRING } -> PrimitiveType.STRING
            subtypes.all { it === PrimitiveType.STRING } -> {
                val choiceType = findOrCreateElementType(choiceRule.name)
                _ruleToType[choiceRule] = choiceType
                createUniquePropertyDeclaration(choiceType, UNNAMED_STRING_PROPERTY_NAME, PrimitiveType.STRING, false, 0)
                choiceType
            }
            //subtypes.all { it is ListSimpleType } -> ListSimpleType(PrimitiveType.ANY)
            subtypes.all { it is ListSimpleType } -> { //=== PrimitiveType.LIST } -> {
                val choiceType = findOrCreateElementType(choiceRule.name)
                _ruleToType[choiceRule] = choiceType
                createUniquePropertyDeclaration(choiceType, UNNAMED_LIST_PROPERTY_VALUE, ListSimpleType(PrimitiveType.ANY), false, 0)
                choiceType
            }
            else -> {
                val choiceType = findOrCreateElementType(choiceRule.name)
                _ruleToType[choiceRule] = choiceType
                createUniquePropertyDeclaration(choiceType, UNNAMED_ANY_PROPERTY_NAME, PrimitiveType.ANY, false, 0)
                choiceType
            }
        }
         */
    }

    private fun populateTypeForChoice(choice: Choice, choiceRuleName: String): RuleType {
        // if all choice gives ElementType then this type is a super type of all choices
        // else choices maps to properties
        val subtypes = choice.alternative.map { typeForRuleItem(it) }
        return when {
            subtypes.all { it === PrimitiveType.NOTHING } -> PrimitiveType.NOTHING
            subtypes.all { it is ElementType } -> {
                val choiceType = findOrCreateElementType(choiceRuleName) as ElementType
                subtypes.forEach {
                    (it as ElementType).addSuperType(choiceType)
                }
                choiceType
            }
            //subtypes.all { it === PrimitiveType.STRING } -> PrimitiveType.STRING
            subtypes.all { it === PrimitiveType.STRING } -> {
                val choiceType = PrimitiveType.STRING
                //_ruleToType[choiceRule] = choiceType
                //val pName = propertyNameFor(et, choice)
                //createUniquePropertyDeclaration(choiceType, UNNAMED_STRING_PROPERTY_NAME, PrimitiveType.STRING, false, 0)
                choiceType
            }
            //subtypes.all { it is ListSimpleType } -> ListSimpleType(PrimitiveType.ANY)
            subtypes.all { it is ListSimpleType } -> { //=== PrimitiveType.LIST } -> {
                val choiceType = ListSimpleType(PrimitiveType.ANY)
                //_ruleToType[choiceRule] = choiceType
                //createUniquePropertyDeclaration(choiceType, UNNAMED_LIST_PROPERTY_VALUE, ListSimpleType(PrimitiveType.ANY), false, 0)
                choiceType
            }

            subtypes.all { it is TupleType } -> when {
                1 == subtypes.map { (it as TupleType).property.values.map { Pair(it.name, it.type) }.toSet() }.toSet().size -> subtypes.first()
                else -> PrimitiveType.ANY
            }

            else -> {
                val choiceType = PrimitiveType.ANY
                //_ruleToType[choiceRule] = choiceType
                //createUniquePropertyDeclaration(choiceType, UNNAMED_ANY_PROPERTY_NAME, PrimitiveType.ANY, false, 0)
                choiceType
            }
        }
    }

    private fun newUnnamed(owningRule: Rule): String {
        return "Tuple"
    }

    private fun createPropertyDeclaration(et: StructuredRuleType, ruleItem: ConcatenationItem, childIndex: Int) {
        when (ruleItem) {
            is EmptyRule -> Unit
            is Terminal -> Unit //createUniquePropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is Embedded -> {
                val refRule = ruleItem.referencedRule(ruleItem.embeddedGrammar)
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
                when {
                    // one choice in the group
                    /*
                    1 == ruleItem.choice.alternative.size -> {
                        val concat = ruleItem.choice.alternative[0]
                        when {
                            // one concatenation item
                            // 1 == concat.items.size -> createPropertyDeclaration(et, concat.items[0], childIndex)
                            // multiple concatenation items
                            else -> {
                                val concatType = TupleType()
                                concat.items.forEachIndexed { idx, it ->
                                    createPropertyDeclaration(concatType, it, idx)
                                }
                                when (concatType.property.size) {
                                    0 -> null // if no items in group, don;t create this property
                                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem), concatType, false, childIndex)
                                }
                            }
                        }
                    }
                     */
                    // multiple choices in group
                    else -> {
                        val choiceType = populateTypeForChoice(ruleItem.choice, UNNAMED_GROUP_PROPERTY_NAME)
                        if (PrimitiveType.NOTHING == choiceType) {
                            Unit
                        } else {
                            val pName = propertyNameFor(et, ruleItem, choiceType)
                            createUniquePropertyDeclaration(et, pName, choiceType, false, childIndex)
                        }
                    }
                }

            }

            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun createPropertyDeclarationForReferencedRule(refRule: Rule, et: StructuredRuleType, ruleItem: SimpleItem, childIndex: Int) {
        val rhs = refRule.rhs
        when (rhs) {
            is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, PrimitiveType.STRING), PrimitiveType.STRING, false, childIndex)
            is Concatenation -> when {
                1 == rhs.items.size -> when (rhs.items[0]) {
                    is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, PrimitiveType.STRING), PrimitiveType.STRING, false, childIndex)
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

    private fun propertyNameFor(et: StructuredRuleType, ruleItem: SimpleItem, type: RuleType): String = when (ruleItem) {
        is EmptyRule -> error("should not happen")
        is Terminal -> when (type) {
            is PrimitiveType -> UNNAMED_PRIMITIVE_PROPERTY_NAME
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

}