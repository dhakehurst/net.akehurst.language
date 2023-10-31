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

package net.akehurst.language.agl.default


import net.akehurst.language.agl.grammarTypeModel.GrammarTypeNamespaceSimple
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.grammar.*
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple
import net.akehurst.language.typemodel.simple.TypeNamespaceSimple

object TypeModelFromGrammar {

    val defaultConfiguration = TypeModelFromGrammarConfigurationDefault()

    fun create(
        grammar: Grammar,
        defaultGoalRuleName: String? = null,
        configuration: TypeModelFromGrammarConfiguration = defaultConfiguration
    ): TypeModel = createFromGrammarList(listOf(grammar), configuration)

    fun createFromGrammarList(
        grammarList: List<Grammar>,
        configuration: TypeModelFromGrammarConfiguration = defaultConfiguration
    ): TypeModel {
        val grmrTypeModel = TypeModelSimple(grammarList.last().name)
        for (grammar in grammarList) {
            grmrTypeModel.addNamespace(SimpleTypeModelStdLib)
            //val goalRuleName = defaultGoalRuleName ?: grammar.grammarRule.first { it.isSkip.not() }.name
            //val goalRule = grammar.findAllResolvedGrammarRule(goalRuleName) ?: error("Cannot find grammar rule '$goalRuleName'")
            val ns = GrammarTypeNamespaceFromGrammar(grammar).build(grmrTypeModel, grammar)
            grmrTypeModel.addNamespace(ns)
        }
        grmrTypeModel.resolveImports()
        return grmrTypeModel
    }

    fun addNamespaceFromGrammar(typeModel: TypeModel, grammar: Grammar) {
        val namespaceFromGrammar = TypeNamespaceSimple(grammar.qualifiedName, listOf(SimpleTypeModelStdLib.qualifiedName))
        /*        this.contextGrammar = context
                this._configuration = configuration
                this.grammar = grammar
                this.model = typeModel
                typeModel.addNamespace(this)
                typeModel.resolveImports() //need to resolve std lib
                val nonSkipRules = grammar.allResolvedGrammarRule
                    .filter { it.isSkip.not() }
                //populate rule type content
                nonSkipRules.forEach {
                    typeForGrammarRule(it)
                }
                this._ruleToType.entries.forEach {
                    val key = it.key
                    val value = it.value
                    super.allRuleNameToType[key] = value
                }*/
    }

}

class GrammarTypeNamespaceFromGrammar(
    val grammar: Grammar
) {

    companion object {
        const val UNNAMED_PRIMITIVE_PROPERTY_NAME = "\$value"
        const val UNNAMED_LIST_PROPERTY_NAME = "\$list"
        const val UNNAMED_TUPLE_PROPERTY_NAME = "\$tuple"
        const val UNNAMED_GROUP_PROPERTY_NAME = "\$group"
        const val UNNAMED_CHOICE_PROPERTY_NAME = "\$choice"
    }

    fun build(
        model: TypeModelSimple,
        context: Grammar,
        configuration: TypeModelFromGrammarConfiguration? = TypeModelFromGrammar.defaultConfiguration
    ): GrammarTypeNamespace {
        this.contextGrammar = context
        this._configuration = configuration
        this.model = model
        _namespace.resolveImports(model) //need to resolve std lib
        val nonSkipRules = grammar.allResolvedGrammarRule.filter { it.isSkip.not() }
        //create DataType for each Rule
        nonSkipRules.forEach {
            //findOrCreateElementType(it) {}
        }
        //populate rule type content
        nonSkipRules.forEach {
            typeForGrammarRule(it)
        }
        this._ruleToType.entries.forEach {
            val key = it.key
            val value = it.value
            _namespace.allRuleNameToType[key] = value
            //if (value.type is DataType) {
            // super.allTypesByName[value.type.name] = value.type
            //}
        }
        return this._namespace
    }

    private val _namespace = GrammarTypeNamespaceSimple("${grammar.namespace.qualifiedName}.${grammar.name}", mutableListOf(SimpleTypeModelStdLib.qualifiedName))

    // GrammarRule.name -> ElementType
    private val _ruleToType = mutableMapOf<String, TypeInstance>()
    private val _typeForRuleItem = mutableMapOf<RuleItem, TypeInstance>()
    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredType, String>, Int>()
    private var _configuration: TypeModelFromGrammarConfiguration? = null

    private lateinit var model: TypeModelSimple
    private lateinit var contextGrammar: Grammar

    private fun findOrCreateElementType(rule: GrammarRule, ifCreate: (DataType) -> Unit): TypeInstance {
        val ruleName = rule.name
        val existing = _ruleToType[ruleName]
        return if (null == existing) {
            val elTypeName = _configuration?.typeNameFor(rule) ?: ruleName
            val et = _namespace.findOwnedOrCreateDataTypeNamed(elTypeName) // DataTypeSimple(this, elTypeName)
            val tt = et.instance()
            _ruleToType[ruleName] = tt
            ifCreate.invoke(et)
            tt
        } else {
            existing
        }
    }

    private fun grammarTypeNamespaceBuilderForEmbedded(ruleItem: Embedded): GrammarTypeNamespaceFromGrammar {
        val embGrammar = ruleItem.embeddedGrammarReference.resolved!!
        val embBldr = GrammarTypeNamespaceFromGrammar(embGrammar)
        val embNs = embBldr.build(model, this.contextGrammar, this._configuration)
        if (model.namespace.containsKey(embNs.qualifiedName)) {
            //already added
        } else {
            model.addNamespace(embNs)
        }
        _namespace.addImport(embNs.qualifiedName)
        return embBldr
    }

    private fun typeForGrammarRule(rule: GrammarRule): TypeInstance {
        val type = _ruleToType[rule.name]
        return if (null != type) {
            type // return the type if it exists, also stops recursion
        } else {
            val ruleTypeUse: TypeInstance = when {
                rule.isLeaf -> SimpleTypeModelStdLib.String
                else -> {
                    val rhs = rule.rhs
                    when (rhs) {
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
                        else -> error("Internal error, unhandled subtype of rule '${rule.name}'.rhs '${rhs::class.simpleName}' when creating TypeNamespace from grammar '${grammar.qualifiedName}'")
                    }
                }
            }
            _ruleToType[rule.name] = ruleTypeUse
            ruleTypeUse
        }
    }

    // Type for a GrammarRule is in some cases different than type for a rule item when part of something else in a rule
    private fun typeForRuleItem(ruleItem: RuleItem, forProperty: Boolean): TypeInstance {
        val type = _typeForRuleItem[ruleItem]
        return if (null != type) {
            type
        } else {
            val ruleType: TypeInstance = when (ruleItem) {
                is EmptyRule -> SimpleTypeModelStdLib.NothingType
                is Terminal -> if (forProperty) SimpleTypeModelStdLib.NothingType else SimpleTypeModelStdLib.String
                is NonTerminal -> {
                    val refRule = ruleItem.referencedRuleOrNull(this.grammar)
                    when {
                        null == refRule -> SimpleTypeModelStdLib.NothingType
                        refRule.isLeaf -> SimpleTypeModelStdLib.String
                        refRule.rhs is EmptyRule -> SimpleTypeModelStdLib.NothingType
                        else -> typeForGrammarRule(refRule)
                    }
                }

                is Embedded -> {
                    val embBldr = grammarTypeNamespaceBuilderForEmbedded(ruleItem)
                    val embNs = embBldr._namespace
                    embNs.findTypeUsageForRule(ruleItem.name) ?: error("Should never happen")
                }

                is Choice -> typeForChoiceRuleItem(ruleItem, forProperty)
                is Concatenation -> tupleTypeFor(ruleItem, ruleItem.items)
                is Group -> typeForGroup(ruleItem, forProperty)
                is OptionalItem -> {
                    val itemType = typeForRuleItem(ruleItem.item, forProperty) //TODO: could cause recursion overflow
                    when (itemType.type) {
                        SimpleTypeModelStdLib.NothingType.type -> SimpleTypeModelStdLib.NothingType

                        else -> {
                            val t = itemType.type.instance(emptyList(), true)
                            _typeForRuleItem[ruleItem] = t
                            t
                        }
                    }
                }

                is SimpleList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeInstance>()
                    val t = SimpleTypeModelStdLib.List.instance(typeArgs)
                    _typeForRuleItem[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item, forProperty)
                    when (itemType.type) {
                        SimpleTypeModelStdLib.NothingType.type -> {
                            _typeForRuleItem.remove(ruleItem)
                            SimpleTypeModelStdLib.NothingType
                        }

                        else -> {
                            typeArgs.add(itemType)
                            t
                        }
                    }
                }

                is SeparatedList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeInstance>()
                    val t = SimpleTypeModelStdLib.ListSeparated.instance(typeArgs)
                    _typeForRuleItem[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item, forProperty)
                    val sepType = typeForRuleItem(ruleItem.separator, forProperty)
                    when {
                        itemType.type == SimpleTypeModelStdLib.NothingType.type -> {
                            _typeForRuleItem.remove(ruleItem)
                            SimpleTypeModelStdLib.NothingType
                        }

                        sepType.type == SimpleTypeModelStdLib.NothingType.type -> {
                            val lt = SimpleTypeModelStdLib.List.instance(listOf(itemType))
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

    private fun elementTypeFor(rule: GrammarRule, items: List<RuleItem>): TypeInstance {
        val concatType = findOrCreateElementType(rule) { newType ->
            items.forEachIndexed { idx, it -> createPropertyDeclaration(newType, it, idx) }
        }
        return concatType
    }

    private fun tupleTypeFor(ruleItem: RuleItem, items: List<RuleItem>): TypeInstance {
        val tt = _namespace.createTupleType()
        val ti = _namespace.createTupleTypeInstance(tt, emptyList(), false)
        this._typeForRuleItem[ruleItem] = ti
        items.forEachIndexed { idx, it -> createPropertyDeclaration(tt, it, idx) }
        return when {
            tt.property.isEmpty() -> {
                this._typeForRuleItem[ruleItem] = SimpleTypeModelStdLib.NothingType
                SimpleTypeModelStdLib.NothingType
            }

            else -> ti
        }
    }

    private fun typeForGroup(group: Group, forProperty: Boolean): TypeInstance {
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

    private fun typeForChoiceRule(choice: Choice, choiceRule: GrammarRule): TypeInstance {
        val subtypes = choice.alternative.map { typeForRuleItem(it, false) }
        return when {
            subtypes.all { it.type == SimpleTypeModelStdLib.NothingType.type } -> SimpleTypeModelStdLib.NothingType.type.instance(emptyList(), subtypes.any { it.isNullable })
            subtypes.all { it.type is PrimitiveType } -> SimpleTypeModelStdLib.String
            subtypes.all { it.type is DataType } -> findOrCreateElementType(choiceRule) { newType ->
                subtypes.forEach {
                    (it.type as DataType).addSupertype(newType.name)
                    newType.addSubtype(it.type.name)
                }
            }

            subtypes.all { it.type == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.instance(listOf(itemType))
                choiceType
            }

            subtypes.all { it.type is TupleType } -> when {
                1 == subtypes.map { (it.type as TupleType).property.values.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypes.first()
                    when {
                        t.type is TupleType && (t.type as TupleType).properties.isEmpty() -> SimpleTypeModelStdLib.NothingType
                        else -> t
                    }
                }

                else -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).instance()
            }

            else -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).instance()
        }
    }

    private fun typeForChoiceRuleItem(choice: Choice, forProperty: Boolean): TypeInstance {
        val subtypes = choice.alternative.map { typeForRuleItem(it, forProperty) }
        return when {
            subtypes.all { it.type == SimpleTypeModelStdLib.NothingType.type } -> SimpleTypeModelStdLib.NothingType.type.instance(emptyList(), subtypes.any { it.isNullable })
            subtypes.all { it.type is PrimitiveType } -> SimpleTypeModelStdLib.String
            subtypes.all { it.type is DataType } -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).instance()
            subtypes.all { it.type == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.instance(listOf(itemType))
                choiceType
            }

            subtypes.all { it.type is TupleType } -> when {
                1 == subtypes.map { (it.type as TupleType).property.values.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypes.first()
                    when {
                        t.type is TupleType && (t.type as TupleType).properties.isEmpty() -> SimpleTypeModelStdLib.NothingType
                        else -> t
                    }
                }

                else -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).instance()
            }

            else -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).instance()
        }
    }

    private fun createPropertyDeclaration(et: StructuredType, ruleItem: RuleItem, childIndex: Int) {
        // always called from within a concatenation
        // - never have Concat or Choice direct inside a Concat
        when (ruleItem) {
            is EmptyRule -> Unit
            is Terminal -> Unit //createUniquePropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is Embedded -> createPropertyDeclarationForEmbedded(et, ruleItem, childIndex)

            is NonTerminal -> {
                val refRule = ruleItem.referencedRuleOrNull(this.grammar)
                createPropertyDeclarationForReferencedRule(refRule, et, ruleItem, childIndex)
            }

            is Concatenation -> TODO("Concatenation")
            is Choice -> {
                val tu = typeForRuleItem(ruleItem, true)
                when (tu.type) {
                    SimpleTypeModelStdLib.NothingType.type -> Unit
                    else -> {
                        val n = propertyNameFor(et, ruleItem, tu.type)
                        createUniquePropertyDeclaration(et, n, tu, childIndex)
                    }
                }
            }

            is OptionalItem -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.type == SimpleTypeModelStdLib.NothingType.type -> Unit
                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                }
            }

            is SimpleList -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.type == SimpleTypeModelStdLib.NothingType.type -> Unit
                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                }
            }

            is SeparatedList -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.type == SimpleTypeModelStdLib.NothingType.type -> Unit
                    else -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem.item, t.type), t, childIndex)
                }
            }

            is Group -> {
                val gt = typeForGroup(ruleItem, true)
                when (gt.type) {
                    SimpleTypeModelStdLib.NothingType.type -> Unit
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

    private fun createPropertyDeclarationForReferencedRule(refRule: GrammarRule?, et: StructuredType, ruleItem: SimpleItem, childIndex: Int) {
        val rhs = refRule?.rhs
        when (rhs) {
            is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.type), SimpleTypeModelStdLib.String, childIndex)

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

    //TODO: combine with above by passing in TypeModel
    private fun createPropertyDeclarationForEmbedded(et: StructuredType, ruleItem: Embedded, childIndex: Int) {
        val embBldr = grammarTypeNamespaceBuilderForEmbedded(ruleItem) //TODO: configuration
        val refRule = ruleItem.referencedRule(ruleItem.embeddedGrammarReference.resolved!!) //TODO: check for null
        val rhs = refRule.rhs
        when (rhs) {
            is Terminal -> createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.type), SimpleTypeModelStdLib.String, childIndex)

            is Concatenation -> {
                val t = embBldr.typeForRuleItem(ruleItem, true)
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
                    val propType = embBldr.typeForRuleItem(rhs, true) //to get list type
                    createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType.type), propType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = embBldr.typeForChoiceRule(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.type)
                createUniquePropertyDeclaration(et, pName, choiceType, childIndex)
            }

            else -> {
                val propType = embBldr.typeForRuleItem(ruleItem, true)
                createUniquePropertyDeclaration(et, propertyNameFor(et, ruleItem, propType.type), propType, childIndex)
            }
        }
    }

    private fun propertyNameFor(et: StructuredType, ruleItem: RuleItem, ruleItemType: TypeDeclaration): String {
        return when (_configuration) {
            null -> when (ruleItem) {
                is EmptyRule -> error("should not happen")
                is Terminal -> when (ruleItemType) {
                    is PrimitiveType -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                    is CollectionType -> UNNAMED_LIST_PROPERTY_NAME
                    is TupleType -> UNNAMED_TUPLE_PROPERTY_NAME
                    else -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                }

                is Embedded -> "${ruleItem.embeddedGoalName.lower()}"
                //is Embedded -> "${ruleItem.embeddedGrammarReference.resolved!!.name}_${ruleItem.embeddedGoalName}"
                is NonTerminal -> ruleItem.name
                is Group -> UNNAMED_GROUP_PROPERTY_NAME
                else -> error("Internal error, unhandled subtype of SimpleItem")
            }

            else -> _configuration!!.propertyNameFor(contextGrammar, ruleItem, ruleItemType)
        }
    }

    private fun createUniquePropertyDeclaration(et: StructuredType, name: String, type: TypeInstance, childIndex: Int): PropertyDeclaration {
        val uniqueName = createUniquePropertyNameFor(et, name)
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        return et.appendStoredProperty(uniqueName, type, characteristics, childIndex)
    }

    private fun createUniquePropertyNameFor(et: StructuredType, name: String): String {
        val key = Pair(et, name)
        val nameCount = this._uniquePropertyNames[key]
        val uniqueName = if (null == nameCount) {
            this._uniquePropertyNames[key] = 2
            name
        } else {
            this._uniquePropertyNames[key] = nameCount + 1
            "$name$nameCount"
        }
        return uniqueName
    }

}