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
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.typemodel.*

class TypeModelFromGrammar(
    namespace: String,
    name: String
) : TypeModelAbstract(namespace, name) {
    constructor(grammar: Grammar, configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration) : this(listOf(grammar), configuration)
    constructor(grammars: List<Grammar>, configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration)
            : this(grammars.last().namespace.qualifiedName, grammars.last().name) {
        this._configuration = configuration
        grammars.forEach { g ->
            this.grammar = g
            g.allResolvedGrammarRule
                .filter { it.isLeaf.not() && it.isSkip.not() }
                .forEach { typeForRhs(it) }
            this._ruleToType.entries.forEach {
                val key = it.key
                val value = it.value
                super.allRuleNameToType[key] = value
                super.allTypesByName[value.type.name] = value.type
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
    private val _ruleToType = mutableMapOf<String, TypeUsage>()
    private val _typeForRuleItem = mutableMapOf<RuleItem, TypeUsage>()
    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredRuleType, String>, Int>()
    private var _configuration: TypeModelFromGrammarConfiguration? = null
    private var _psuedoTypes = mutableSetOf<RuleType>()

    // temp var - changes for each Grammar processed
    private lateinit var grammar: Grammar
    //private val _pseudoRuleNameGenerator = PseudoRuleNames(grammar)

    //override fun findTypeForRule(ruleName: String): RuleType? = _ruleToType[ruleName]

    private fun stringTypeForRuleName(name: String): StringType {
        val existing = _ruleToType[name]
        return if (null == existing) {
            val type = StringType
            _ruleToType[name] = TypeUsage.ofType(type) //halt recursion
            type
        } else {
            existing as StringType
        }
    }

    private fun findOrCreateTypeForRule(rule: GrammarRule, ifCreate: () -> TypeUsage): TypeUsage {
        val ruleName = rule.name
        val existing = _ruleToType[ruleName]
        return if (null == existing) {
            val t = ifCreate.invoke()
            _ruleToType[ruleName] = t
            t
        } else {
            existing
        }
    }

    private fun findOrCreateElementType(rule: GrammarRule, ifCreate: (ElementType) -> Unit): TypeUsage {
        val ruleName = rule.name
        val existing = _ruleToType[ruleName]
        return if (null == existing) {
            val elTypeName = _configuration?.typeNameFor(rule) ?: ruleName
            val et = ElementType(this, elTypeName)
            val tt = TypeUsage.ofType(et)
            _ruleToType[ruleName] = tt
            ifCreate.invoke(et)
            tt
        } else {
            existing
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

    // Type for a GrammarRule is in some cases different than type for a rule item when part of something else in a rule
    private fun typeForRhs(rule: GrammarRule): TypeUsage {
        val type = _ruleToType[rule.name]
        return if (null != type) {
            type // return the type if it exists, also stops recursion
        } else {
            val rhs = rule.rhs
            val ruleTypeUse: TypeUsage = when (rhs) {
                is EmptyRule -> findOrCreateElementType(rule) {}
                is Terminal -> typeForConcatenationRule(rule, listOf(rhs))
                is NonTerminal -> typeForConcatenationRule(rule, listOf(rhs))
                is Embedded -> typeForConcatenationRule(rule, listOf(rhs))
                is Choice -> typeForChoiceRule(rule)
                is Concatenation -> typeForConcatenationRule(rule, rhs.items)
                is Group -> typeForGroup(rhs)
                is OptionalItem -> {
                    val t = typeForRuleItem(rhs.item)
                    TypeUsage.ofType(t.type, t.arguments, true)
                }

                is SimpleList -> {
                    when (rhs.item) {
                        is Terminal -> typeForConcatenationRule(rule, listOf())
                        else -> typeForConcatenationRule(rule, listOf(rhs))
                    }
                    //typeForSimpleList(rule, rhs)
                }

                is SeparatedList -> {
                    when {
                        rhs.item is Terminal -> typeForConcatenationRule(rule, listOf())
                        else -> typeForConcatenationRule(rule, listOf(rhs))
                    }
                    //typeForConcatenationRule(rule, listOf(rhs))
                    //typeForSeparatedList(rule, rhs)
                }

                else -> error("Internal error, unhandled subtype of rule '${rule.name}'.rhs '${rhs::class.simpleName}' when getting TypeModel from grammar '${this.qualifiedName}'")
            }
            _ruleToType[rule.name] = ruleTypeUse
            ruleTypeUse
        }
    }

    // Type for a GrammarRule is in some cases different than type for a rule item when part of something else in a rule
    private fun typeForRuleItem(ruleItem: RuleItem): TypeUsage {
        val type = _typeForRuleItem[ruleItem]
        return if (null != type) {
            type
        } else {
            val ruleType: TypeUsage = when (ruleItem) {
                is EmptyRule -> NothingType.use
                is Terminal -> StringType.use
                is NonTerminal -> {
                    val refRule = ruleItem.referencedRule(this.grammar)
                    when {
                        refRule.isLeaf -> TypeUsage.ofType(StringType)
                        refRule.rhs is EmptyRule -> NothingType.use
                        refRule.rhs is Choice -> {
                            typeForRhs(refRule)
                        }

                        else -> typeForRhs(refRule)
                    }
                }

                is Embedded -> {
                    val embTmfg = TypeModelFromGrammar(listOf(ruleItem.embeddedGrammarReference.resolved!!)) //TODO: check for null
                    val embTm = embTmfg
                    embTm.findTypeUsageForRule(ruleItem.name) ?: error("Should never happen")
                }

                is Choice -> TODO()
                is Concatenation -> typeForConcatenationAsRuleItem(ruleItem)
                is Group -> typeForGroup(ruleItem)
                is OptionalItem -> {
                    val elementType = typeForRuleItem(ruleItem.item) //TODO: could cause recursion overflow
                    val t = TypeUsage.ofType(elementType.type, emptyList(), true)
                    _typeForRuleItem[ruleItem] = t
                    t
                }

                is SimpleList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeUsage>()
                    val t = TypeUsage.ofType(ListSimpleType, typeArgs)
                    _typeForRuleItem[ruleItem] = t
                    val elementType = typeForRuleItem(ruleItem.item)
                    typeArgs.add(elementType)
                    t
                }

                is SeparatedList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeUsage>()
                    val t = TypeUsage.ofType(ListSeparatedType, typeArgs)
                    _typeForRuleItem[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item)
                    val sepType = typeForRuleItem(ruleItem.separator)
                    typeArgs.add(itemType)
                    typeArgs.add(sepType)
                    t
                }

                else -> error("Internal error, unhandled subtype of RuleItem")
            }
            _typeForRuleItem[ruleItem] = ruleType
            ruleType
        }
    }

    private fun typeForConcatenationRule(rule: GrammarRule, items: List<ConcatenationItem>): TypeUsage {
        val concatType = findOrCreateElementType(rule) { newType ->
            items.forEachIndexed { idx, it -> createPropertyDeclaration(newType, it, idx) }
        }
        return concatType
    }

    private fun typeForConcatenationAsRuleItem(ruleItem: Concatenation): TypeUsage {
        if (Debug.CHECK) check(ruleItem.items.size > 1)
        val concatType = TupleType()
        val t = TypeUsage.ofType(concatType)
        this._typeForRuleItem[ruleItem] = t
        ruleItem.items.forEachIndexed { idx, it ->
            createPropertyDeclaration(concatType, it, idx)
        }
        return when {
            concatType.property.isEmpty() -> NothingType.use
            else -> t
        }
        //return t
    }

    private fun typeForChoiceRule(choiceRule: GrammarRule): TypeUsage { //name: String, alternative: List<Concatenation>): RuleType {
        val t = populateTypeForChoice(choiceRule.rhs as Choice, choiceRule)
        return t
    }

    private fun typeForGroup(group: Group): TypeUsage {
        val content = group.groupedContent
        return when (content) {
            is Choice -> populateTypeForChoice(content, null)
            else -> typeForRuleItem(content)
        }
    }

    private fun typeForSimpleList(rule: GrammarRule, list: SimpleList): TypeUsage {
        val args = mutableListOf<TypeUsage>()
        val rt = findOrCreateTypeForRule(rule) {
            TypeUsage.ofType(ListSimpleType, args)
        }
        val et = typeForRuleItem(list.item)
        args.add(rt)
        return rt
    }

    private fun typeForSeparatedList(rule: GrammarRule, slist: SeparatedList): TypeUsage {
        val args = mutableListOf<TypeUsage>()
        val rt = findOrCreateTypeForRule(rule) {
            TypeUsage.ofType(ListSimpleType, args)
        }
        val itemType = typeForRuleItem(slist.item)
        val sepType = typeForRuleItem(slist.separator)
        args.add(itemType)
        args.add(sepType)
        return rt
    }

    private fun populateTypeForChoice(choice: Choice, choiceRule: GrammarRule?): TypeUsage {
        // if all choice gives ElementType then this type is a super type of all choices
        // else choices maps to properties
        val subtypes = choice.alternative.map { typeForRuleItem(it) }
        return when {
            subtypes.all { it.type is NothingType } -> TypeUsage.ofType(NothingType, emptyList(), subtypes.any { it.nullable })
            subtypes.all { it.type is StringType } -> TypeUsage.ofType(StringType, emptyList(), subtypes.any { it.nullable })

            subtypes.all { it.type is ElementType } -> when {
                null == choiceRule -> {
                    TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, true))
                }

                else -> {
                    val choiceType = findOrCreateElementType(choiceRule) { newType ->
                        subtypes.forEach {
                            (it.type as ElementType).addSuperType(newType)
                        }
                    }
                    choiceType
                }
            }

            subtypes.all { it.type is ListSimpleType } -> { //=== PrimitiveType.LIST } -> {
                val itemType = TypeUsage.ofType(AnyType)//TODO: compute better elementType ?
                val choiceType = ListSimpleType.ofType(itemType)
                choiceType
            }

            subtypes.all { it.type is TupleType } -> when {
                1 == subtypes.map { (it.type as TupleType).property.values.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypes.first()
                    when {
                        t.type is TupleType && t.type.properties.isEmpty() -> NothingType.use
                        else -> t
                    }
                }

                else -> {
                    TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, false))
                }
            }

            else -> when {
                null == choiceRule -> {
                    TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, false))
                }

                else -> {/*
                    val elTypes = choice.alternative.mapIndexed { i, it ->
                        val itType = subtypes[i]
                        when (itType) {
                            is ElementType -> itType
                            else -> {
                                val baseElTypeName = _configuration?.typeNameFor(choiceRule) ?: choiceRule.name
                                val psudoRuleName = choiceRule.name + "Choice$i"
                                val elTypeName = "${baseElTypeName}Choice$i"
                                val et = ElementType(this, elTypeName)
                                et.appendProperty("\$value", PropertyDeclaration(et, "\$value", itType, false, 0))
                                _ruleToType[psudoRuleName] = et
                                et
                            }
                        }
                    }
                    val choiceType = findOrCreateElementType(choiceRule) { newType ->
                        elTypes.forEach {
                            (it as ElementType).addSuperType(newType)
                        }
                    }
                    choiceType
                    */
                    TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, true))
                }
            }
        }
    }

    private fun createPropertyDeclaration(et: StructuredRuleType, ruleItem: RuleItem, childIndex: Int) {
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

            is Concatenation -> TODO("Concatenation")
            is Choice -> TODO("Choice")

            is OptionalItem -> {
                when {
                    ruleItem.item is Terminal -> Unit //do not add optional literals
                    else -> {
                        val t = typeForRuleItem(ruleItem)
                        createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                    }
                }
            }

            is SimpleList -> {
                val t = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
            }

            is SeparatedList -> {
                val t = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
            }

            is Group -> {
                val gt = typeForGroup(ruleItem)
                when (gt.type) {
                    is NothingType -> Unit
                    else -> {
                        val pName = propertyNameFor(et, ruleItem, gt.type)
                        createUniquePropertyDeclaration(et, pName, gt, childIndex)
                    }
                }
            }

            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun createPropertyDeclarationForReferencedRule(refRule: GrammarRule, et: StructuredRuleType, ruleItem: SimpleItem, childIndex: Int) {
        val rhs = refRule.rhs
        when (rhs) {
            is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, StringType), StringType.use, childIndex)

            is Concatenation -> {
                val t = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, t.type), t, childIndex)
            }

            is ListOfItems -> {
                val ignore = when (rhs) {
                    is SimpleList -> when (rhs.item) {
                        is Terminal -> true
                        else -> false
                    }

                    is SeparatedList -> when (rhs.item) {
                        is Terminal -> true
                        else -> false
                    }

                    else -> error("Internal Error: not handled ${rhs::class.simpleName}")
                }
                if (ignore) {
                    Unit
                } else {
                    val propType = typeForRuleItem(rhs) //to get list type
                    createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType.type), propType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = typeForChoiceRule(refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.type)
                createUniquePropertyDeclaration(et, pName, choiceType, childIndex)
            }

            else -> {
                val propType = typeForRuleItem(ruleItem)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType.type), propType, childIndex)
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

    private fun createUniquePropertyDeclaration(et: StructuredRuleType, name: String, type: TypeUsage, childIndex: Int): PropertyDeclaration? {
        val uniqueName = createUniquePropertyNameFor(et, name)
        return PropertyDeclaration(et, uniqueName, type, childIndex)
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