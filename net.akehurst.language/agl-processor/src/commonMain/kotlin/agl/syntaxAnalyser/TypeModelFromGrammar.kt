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


import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelAbstract
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.grammarTypeModel.StringType
import net.akehurst.language.typemodel.api.*

class TypeModelFromGrammar(
    namespace: String,
    name: String
) : GrammarTypeModelAbstract(namespace, name) {
    constructor(grammar: Grammar, configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration) : this(listOf(grammar), configuration)
    constructor(grammars: List<Grammar>, configuration: TypeModelFromGrammarConfiguration? = defaultConfiguration)
            : this(grammars.last().namespace.qualifiedName, grammars.last().name) {
        this._configuration = configuration
        grammars.forEach { g ->
            this.grammar = g
            g.allResolvedGrammarRule
                .filter { it.isLeaf.not() && it.isSkip.not() }
                .forEach { typeForGrammarRule(it) }
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
        const val UNNAMED_CHOICE_PROPERTY_NAME = "\$choice"

        val defaultConfiguration = TypeModelFromGrammarConfigurationDefault()
    }

    // GrammarRule.name -> ElementType
    private val _ruleToType = mutableMapOf<String, TypeUsage>()
    private val _typeForRuleItem = mutableMapOf<RuleItem, TypeUsage>()
    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredRuleType, String>, Int>()
    private var _configuration: TypeModelFromGrammarConfiguration? = null

    // temp var - changes for each Grammar processed
    private lateinit var grammar: Grammar

    private fun stringTypeForRuleName(name: String): PrimitiveType {
        val existing = _ruleToType[name]
        return if (null == existing) {
            val type = this.StringType
            _ruleToType[name] = TypeUsage.ofType(type) //halt recursion
            type
        } else {
            existing as PrimitiveType
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

    private fun typeForGrammarRule(rule: GrammarRule): TypeUsage {
        val type = _ruleToType[rule.name]
        return if (null != type) {
            type // return the type if it exists, also stops recursion
        } else {
            val rhs = rule.rhs
            val ruleTypeUse: TypeUsage = when (rhs) {
                is EmptyRule -> findOrCreateElementType(rule) {}
                is Terminal -> elementTypeFor(rule, listOf(rhs))
                is NonTerminal -> elementTypeFor(rule, listOf(rhs))
                is Embedded -> elementTypeFor(rule, listOf(rhs))
                is Concatenation -> elementTypeFor(rule, rhs.items)
                is Choice -> typeForChoiceRule(rhs, rule)
                is OptionalItem -> findOrCreateElementType(rule) { et -> createPropertyDeclaration(et, rhs, 0) }
                is SimpleList -> findOrCreateElementType(rule) { et -> createPropertyDeclaration(et, rhs, 0) }
                is SeparatedList -> findOrCreateElementType(rule) { et -> createPropertyDeclaration(et, rhs, 0) }
                is Group -> typeForGroup(rhs, false)
                else -> error("Internal error, unhandled subtype of rule '${rule.name}'.rhs '${rhs::class.simpleName}' when getting TypeModel from grammar '${this.qualifiedName}'")
            }
            _ruleToType[rule.name] = ruleTypeUse
            ruleTypeUse
        }
    }

    /*
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
                    is Concatenation -> typeForConcatenationRule(rule, rhs.items)
                    is Choice -> typeForChoiceRule(rhs, rule)
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

                    is Group -> typeForGroup(rhs)

                    else -> error("Internal error, unhandled subtype of rule '${rule.name}'.rhs '${rhs::class.simpleName}' when getting TypeModel from grammar '${this.qualifiedName}'")
                }
                _ruleToType[rule.name] = ruleTypeUse
                ruleTypeUse
            }
        }
    */
    // Type for a GrammarRule is in some cases different than type for a rule item when part of something else in a rule
    private fun typeForRuleItem(ruleItem: RuleItem, forProperty: Boolean): TypeUsage {
        val type = _typeForRuleItem[ruleItem]
        return if (null != type) {
            type
        } else {
            val ruleType: TypeUsage = when (ruleItem) {
                is EmptyRule -> NothingType.use
                is Terminal -> if (forProperty) NothingType.use else StringType.use
                is NonTerminal -> {
                    val refRule = ruleItem.referencedRule(this.grammar)
                    when {
                        refRule.isLeaf -> StringType.use
                        refRule.rhs is EmptyRule -> NothingType.use
                        else -> typeForGrammarRule(refRule)
                    }
                }

                is Embedded -> {
                    val embTmfg = TypeModelFromGrammar(listOf(ruleItem.embeddedGrammarReference.resolved!!)) //TODO: check for null
                    val embTm = embTmfg
                    embTm.findTypeUsageForRule(ruleItem.name) ?: error("Should never happen")
                }

                is Choice -> typeForChoiceRuleItem(ruleItem, forProperty)
                is Concatenation -> tupleTypeFor(ruleItem, ruleItem.items)
                is Group -> typeForGroup(ruleItem, forProperty)
                is OptionalItem -> {
                    val itemType = typeForRuleItem(ruleItem.item, forProperty) //TODO: could cause recursion overflow
                    when (itemType.type) {
                        is NothingType -> {
                            NothingType.use
                        }

                        else -> {
                            val t = TypeUsage.ofType(itemType.type, emptyList(), true)
                            _typeForRuleItem[ruleItem] = t
                            t
                        }
                    }
                }

                is SimpleList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeUsage>()
                    val t = TypeUsage.ofType(ListSimpleType, typeArgs)
                    _typeForRuleItem[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item, forProperty)
                    when (itemType.type) {
                        is NothingType -> {
                            _typeForRuleItem.remove(ruleItem)
                            NothingType.use
                        }

                        else -> {
                            typeArgs.add(itemType)
                            t
                        }
                    }
                }

                is SeparatedList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeUsage>()
                    val t = TypeUsage.ofType(ListSeparatedType, typeArgs)
                    _typeForRuleItem[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item, forProperty)
                    val sepType = typeForRuleItem(ruleItem.separator, forProperty)
                    when {
                        itemType.type is NothingType -> {
                            _typeForRuleItem.remove(ruleItem)
                            NothingType.use
                        }

                        sepType.type is NothingType -> {
                            val lt = ListSimpleType.ofType(itemType)
                            _typeForRuleItem[ruleItem] = lt
                            lt
                        }

                        else -> {
                            typeArgs.add(itemType)
                            typeArgs.add(sepType)
                            t
                        }
                    }
                }

                else -> error("Internal error, unhandled subtype of RuleItem")
            }
            _typeForRuleItem[ruleItem] = ruleType
            ruleType
        }
    }

    private fun elementTypeFor(rule: GrammarRule, items: List<RuleItem>): TypeUsage {
        val concatType = findOrCreateElementType(rule) { newType ->
            items.forEachIndexed { idx, it -> createPropertyDeclaration(newType, it, idx) }
        }
        return concatType
    }

    private fun tupleTypeFor(ruleItem: RuleItem, items: List<RuleItem>): TypeUsage {
        val concatType = TupleType()
        val t = TypeUsage.ofType(concatType)
        this._typeForRuleItem[ruleItem] = t
        items.forEachIndexed { idx, it -> createPropertyDeclaration(concatType, it, idx) }
        return when {
            concatType.property.isEmpty() -> {
                this._typeForRuleItem[ruleItem] = NothingType.use
                NothingType.use
            }

            else -> t
        }
    }

    private fun typeForGroup(group: Group, forProperty: Boolean): TypeUsage {
        val content = group.groupedContent
        return when (content) {
            is Choice -> typeForChoiceRuleItem(content, forProperty)
            else -> {
                val items = when (content) {
                    is Concatenation -> content.items
                    else -> listOf(content)
                }
                tupleTypeFor(group, items)
            }
        }
    }

    /*
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
    */
    private fun typeForChoiceRule(choice: Choice, choiceRule: GrammarRule): TypeUsage {
        // if all choice gives ElementType then this type is a super type of all choices
        // else choices maps to properties
        val subtypes = choice.alternative.map { typeForRuleItem(it, false) }
        return when {
            subtypes.all { it.type is NothingType } -> TypeUsage.ofType(NothingType, emptyList(), subtypes.any { it.nullable })
            subtypes.all { it.type is PrimitiveType } -> TypeUsage.ofType(StringType, emptyList(), subtypes.any { it.nullable })

            subtypes.all { it.type is ElementType } -> findOrCreateElementType(choiceRule) { newType ->
                subtypes.forEach { (it.type as ElementType).addSuperType(newType) }
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
                        t.type is TupleType && (t.type as TupleType).properties.isEmpty() -> NothingType.use
                        else -> t
                    }
                }

                else -> TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, false))
            }

            else -> TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, true))
        }
    }

    private fun typeForChoiceRuleItem(choice: Choice, forProperty: Boolean): TypeUsage {
        // if all choice gives ElementType then this type is a super type of all choices
        // else choices maps to properties
        val subtypes = choice.alternative.map { typeForRuleItem(it, forProperty) }
        return when {
            subtypes.all { it.type is NothingType } -> TypeUsage.ofType(NothingType, emptyList(), subtypes.any { it.nullable })
            subtypes.all { it.type is PrimitiveType } -> TypeUsage.ofType(StringType, emptyList(), subtypes.any { it.nullable })
            subtypes.all { it.type is ElementType } -> TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, true))

            subtypes.all { it.type is ListSimpleType } -> { //=== PrimitiveType.LIST } -> {
                val itemType = TypeUsage.ofType(AnyType)//TODO: compute better elementType ?
                val choiceType = ListSimpleType.ofType(itemType)
                choiceType
            }

            subtypes.all { it.type is TupleType } -> when {
                1 == subtypes.map { (it.type as TupleType).property.values.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypes.first()
                    when {
                        t.type is TupleType && (t.type as TupleType).properties.isEmpty() -> NothingType.use
                        else -> t
                    }
                }

                else -> TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, false))
            }

            else -> TypeUsage.ofType(UnnamedSuperTypeType(subtypes.map { it }, false))
        }
    }

    private fun createPropertyDeclaration(et: StructuredRuleType, ruleItem: RuleItem, childIndex: Int) {
        // always called from within a concatenation
        // - never have Concat or Choice direct inside a Concat
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
            is Choice -> {
                val tu = typeForRuleItem(ruleItem, true)
                when (tu.type) {
                    is NothingType -> Unit
                    else -> {
                        val n = propertyNameFor(et, ruleItem, tu.type)
                        createUniquePropertyDeclaration(et, n, tu, childIndex)
                    }
                }
            }

            is OptionalItem -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.type is NothingType -> Unit
                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                }
            }

            is SimpleList -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.type is NothingType -> Unit
                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                }
            }

            is SeparatedList -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.type is NothingType -> Unit
                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                }
            }

            is Group -> {
                val gt = typeForGroup(ruleItem, true)
                when (gt.type) {
                    is NothingType -> Unit
                    else -> {
                        val content = ruleItem.groupedContent
                        val pName = when (content) {
                            is Choice -> propertyNameFor(et, content, gt.type)
                            else -> propertyNameFor(et, ruleItem, gt.type)
                        }

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
                val t = typeForRuleItem(ruleItem, true)
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
                    val propType = typeForRuleItem(rhs, true) //to get list type
                    createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType.type), propType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = typeForChoiceRule(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.type)
                createUniquePropertyDeclaration(et, pName, choiceType, childIndex)
            }

            else -> {
                val propType = typeForRuleItem(ruleItem, true)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType.type), propType, childIndex)
            }
        }
    }

    private fun propertyNameFor(et: StructuredRuleType, ruleItem: RuleItem, ruleItemType: TypeDefinition): String {
        return when (_configuration) {
            null -> when (ruleItem) {
                is EmptyRule -> error("should not happen")
                is Terminal -> when (ruleItemType) {
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

            else -> _configuration!!.propertyNameFor(ruleItem, ruleItemType)
        }
    }

    private fun createUniquePropertyDeclaration(et: StructuredRuleType, name: String, type: TypeUsage, childIndex: Int): PropertyDeclaration {
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