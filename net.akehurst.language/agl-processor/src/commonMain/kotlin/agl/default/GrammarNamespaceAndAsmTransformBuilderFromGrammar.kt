/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.language.asmTransform.AsmTransformModelSimple
import net.akehurst.language.agl.language.asmTransform.CreateObjectRuleSimple
import net.akehurst.language.agl.language.asmTransform.SubtypeTransformationRuleSimple
import net.akehurst.language.agl.language.asmTransform.TransformationRuleAbstract
import net.akehurst.language.agl.language.expressions.NavigationDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.language.asmTransform.CreateObjectRule
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.grammar.*
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple

class GrammarNamespaceAndAsmTransformBuilderFromGrammar(
    val typeModel: TypeModel,
    val grammar: Grammar,
    val configuration: Grammar2TypeModelMapping? = TypeModelFromGrammar.defaultConfiguration
) {

    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)
    val transformModel = AsmTransformModelSimple(grammar.qualifiedName)
    val namespace = GrammarTypeNamespaceSimple(
        qualifiedName = "${grammar.namespace.qualifiedName}.${grammar.name}",
        imports = mutableListOf(SimpleTypeModelStdLib.qualifiedName)
    )

    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredType, String>, Int>()
    private val _grRuleNameToTrRule = mutableMapOf<String, TransformationRule>()
    private val _grRuleItemToTrRule = mutableMapOf<RuleItem, TransformationRule>()


    fun build() {
        val nonSkipRules = grammar.allResolvedGrammarRule.filter { it.isSkip.not() }
        for (gr in nonSkipRules) {
            createOrFindTrRuleForGrammarRule(gr)
        }
    }

    private fun findOrCreateTrRule(rule: GrammarRule, ifCreate: (DataType, CreateObjectRule) -> Unit): TransformationRule {
        val ruleName = rule.name
        val existing = _grRuleNameToTrRule[ruleName]
        return if (null == existing) {
            val tn = this.configuration?.typeNameFor(rule) ?: ruleName
            val dt = namespace.findOwnedOrCreateDataTypeNamed(tn) // DataTypeSimple(this, elTypeName)
            val tt = dt.type()
            val cr = CreateObjectRuleSimple(tn)
            cr.resolveTypeAs(tt)
            transformModel.addRule(cr)
            _grRuleNameToTrRule[ruleName] = cr
            ifCreate.invoke(dt, cr)
            _grRuleNameToTrRule[ruleName]!!
        } else {
            existing
        }
    }

    private fun builderForEmbedded(ruleItem: Embedded): GrammarNamespaceAndAsmTransformBuilderFromGrammar {
        val embGrammar = ruleItem.embeddedGrammarReference.resolved!!
        val embBldr = GrammarNamespaceAndAsmTransformBuilderFromGrammar(typeModel, embGrammar, this.configuration)
        if (typeModel.namespace.containsKey(embBldr.namespace.qualifiedName)) {
            //already added
        } else {
            embBldr.build()
            (typeModel as TypeModelSimple).addNamespace(embBldr.namespace)
        }
        namespace.addImport(embBldr.namespace.qualifiedName)
        return embBldr
    }

    private fun createOrFindTrRuleForGrammarRule(gr: GrammarRule): TransformationRule {
        val cor = _grRuleNameToTrRule[gr.name]
        return when {
            null != cor -> cor
            gr.isLeaf -> {
                val t = SimpleTypeModelStdLib.String
                // return unused TrRule to carry the Type consistently
                CreateObjectRuleSimple(t.typeName).also { it.resolveTypeAs(t) }
            }

            else -> {
                val rhs = gr.rhs
                when (rhs) {
                    is EmptyRule -> findOrCreateTrRule(gr) { _, _ -> }
                    is Terminal -> trRuleForRuleItemList(gr, listOf(rhs))
                    is NonTerminal -> trRuleForRuleItemList(gr, listOf(rhs))
                    is Embedded -> trRuleForRuleItemList(gr, listOf(rhs))
                    is Concatenation -> trRuleForRuleItemList(gr, rhs.items)
                    is Choice -> trRuleForChoiceRule(rhs, gr)
                    is OptionalItem -> trRuleForRuleItemList(gr, listOf(rhs))
                    is SimpleList -> trRuleForRuleItemList(gr, listOf(rhs))
                    is SeparatedList -> trRuleForRuleItemList(gr, listOf(rhs))
                    is Group -> typeForGroup(rhs, false)
                    else -> error("Internal error, unhandled subtype of rule '${gr.name}'.rhs '${rhs::class.simpleName}' when creating TypeNamespace from grammar '${grammar.qualifiedName}'")
                }
            }
        }
    }

    private fun trRuleForRuleItemList(rule: GrammarRule, items: List<RuleItem>): TransformationRule {
        val concatType = findOrCreateTrRule(rule) { newType, cor ->
            items.forEachIndexed { idx, it -> createPropertyDeclarationAndAssignment(newType, it, idx) }
        }
        return concatType
    }

    private fun trRuleForChoiceRule(choice: Choice, choiceRule: GrammarRule): TransformationRule {
        val subtypes = choice.alternative.map { typeForRuleItem(it, false) }
        return when {
            subtypes.all { it.declaration == SimpleTypeModelStdLib.NothingType.declaration } -> {
                val t = SimpleTypeModelStdLib.NothingType.declaration.type(emptyList(), subtypes.any { it.isNullable })
                SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) }
            }

            subtypes.all { it.declaration is PrimitiveType } -> {
                SimpleTypeModelStdLib.String.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
            }

            subtypes.all { it.declaration is DataType } -> findOrCreateTrRule(choiceRule) { newType, cor ->
                subtypes.forEach {
                    (it.declaration as DataType).addSupertype(newType.name)
                    newType.addSubtype(it.declaration.name)
                }
            }

            subtypes.all { it.declaration == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.type(listOf(itemType))
                choiceType.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
            }

            subtypes.all { it.declaration is TupleType } -> when {
                1 == subtypes.map { (it.declaration as TupleType).property.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypes.first()
                    when {
                        t.declaration is TupleType && (t.declaration as TupleType).property.isEmpty() -> SimpleTypeModelStdLib.NothingType
                        else -> t
                    }
                }

                else -> namespace.createUnnamedSupertypeType(subtypes.map { it }).type()
            }.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }

            else -> namespace.createUnnamedSupertypeType(subtypes.map { it }).type().let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
        }
    }

    // Type for a GrammarRule is in some cases different than type for a rule item when part of something else in a rule
    private fun typeForRuleItem(ruleItem: RuleItem, forProperty: Boolean): TransformationRule {
        val existing = _grRuleItemToTrRule[ruleItem]
        return if (null != existing) {
            existing
        } else {
            val trRule: TransformationRule = when (ruleItem) {
                is EmptyRule -> SimpleTypeModelStdLib.NothingType.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
                is Terminal -> (if (forProperty) SimpleTypeModelStdLib.NothingType else SimpleTypeModelStdLib.String).let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
                is NonTerminal -> {
                    val refRule = ruleItem.referencedRuleOrNull(this.grammar)
                    when {
                        null == refRule -> SimpleTypeModelStdLib.NothingType.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
                        refRule.isLeaf -> SimpleTypeModelStdLib.String.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
                        refRule.rhs is EmptyRule -> SimpleTypeModelStdLib.NothingType.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
                        else -> createOrFindTrRuleForGrammarRule(refRule)
                    }
                }

                is Embedded -> {
                    val embBldr = builderForEmbedded(ruleItem)
                    val embNs = embBldr.namespace
                    embNs.findTypeUsageForRule(ruleItem.name) ?: error("Should never happen")
                }

                is Choice -> typeForChoiceRuleItem(ruleItem, forProperty)
                is Concatenation -> tupleTypeFor(ruleItem, ruleItem.items)
                is Group -> typeForGroup(ruleItem, forProperty)
                is OptionalItem -> {
                    val trRule = typeForRuleItem(ruleItem.item, forProperty) //TODO: could cause recursion overflow
                    when (trRule.resolvedType.declaration) {
                        SimpleTypeModelStdLib.NothingType.declaration -> SimpleTypeModelStdLib.NothingType.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }

                        else -> {
                            val t = trRule.resolvedType.declaration.type(emptyList(), true)
                            (trRule as TransformationRuleAbstract).resolveTypeAs(t) //change type to optional
                            _grRuleItemToTrRule[ruleItem] = trRule
                            trRule
                        }
                    }
                }

                is SimpleList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeInstance>()
                    val t = SimpleTypeModelStdLib.List.type(typeArgs)
                    _grRuleItemToTrRule[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item, forProperty)
                    when (itemType.declaration) {
                        SimpleTypeModelStdLib.NothingType.declaration -> {
                            _grRuleItemToTrRule.remove(ruleItem)
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
                    val t = SimpleTypeModelStdLib.ListSeparated.type(typeArgs)
                    _grRuleItemToTrRule[ruleItem] = t
                    val itemType = typeForRuleItem(ruleItem.item, forProperty)
                    val sepType = typeForRuleItem(ruleItem.separator, forProperty)
                    when {
                        itemType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> {
                            _grRuleItemToTrRule.remove(ruleItem)
                            SimpleTypeModelStdLib.NothingType
                        }

                        sepType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> {
                            val lt = SimpleTypeModelStdLib.List.type(listOf(itemType))
                            _grRuleItemToTrRule[ruleItem] = lt
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
            _grRuleItemToTrRule[ruleItem] = trRule
            trRule
        }
    }

    private fun typeForChoiceRuleItem(choice: Choice, forProperty: Boolean): TransformationRule {
        val subtypes = choice.alternative.map { typeForRuleItem(it, forProperty) }
        return when {
            subtypes.all { it.declaration == SimpleTypeModelStdLib.NothingType.declaration } -> SimpleTypeModelStdLib.NothingType.declaration.type(
                emptyList(),
                subtypes.any { it.isNullable })

            subtypes.all { it.declaration is PrimitiveType } -> SimpleTypeModelStdLib.String
            subtypes.all { it.declaration is DataType } -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).type()
            subtypes.all { it.declaration == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.type(listOf(itemType))
                choiceType
            }

            subtypes.all { it.declaration is TupleType } -> when {
                1 == subtypes.map { (it.declaration as TupleType).property.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypes.first()
                    when {
                        t.declaration is TupleType && (t.declaration as TupleType).property.isEmpty() -> SimpleTypeModelStdLib.NothingType
                        else -> t
                    }
                }

                else -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).type()
            }

            else -> _namespace.createUnnamedSupertypeType(subtypes.map { it }).type()
        }
    }

    private fun typeForGroup(group: Group, forProperty: Boolean): TransformationRule {
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

    private fun tupleTypeFor(ruleItem: RuleItem, items: List<RuleItem>): TransformationRule {
        val tt = _namespace.createTupleType()
        val ti = _namespace.createTupleTypeInstance(tt, emptyList(), false)
        this._typeForRuleItem[ruleItem] = ti
        items.forEachIndexed { idx, it -> createPropertyDeclaration(tt, it, idx) }
        return when {
            tt.allProperty.isEmpty() -> {
                this._typeForRuleItem[ruleItem] = SimpleTypeModelStdLib.NothingType
                SimpleTypeModelStdLib.NothingType
            }

            else -> ti
        }
    }

    private fun createPropertyDeclarationAndAssignment(et: StructuredType, cor: CreateObjectRuleSimple, ruleItem: RuleItem, childIndex: Int) {
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
                when (tu.declaration) {
                    SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val n = propertyNameFor(et, ruleItem, tu.declaration)
                        createUniquePropertyDeclarationAndAssignment(et, cor, n, tu, childIndex)
                    }
                }
            }

            is OptionalItem -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.declaration == SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> createUniquePropertyDeclarationAndAssignment(et, cor, propertyNameFor(et, ruleItem.item, t.declaration), t, childIndex)
                }
            }

            is SimpleList -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.declaration == SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> createUniquePropertyDeclarationAndAssignment(et, cor, propertyNameFor(et, ruleItem.item, t.declaration), t, childIndex)
                }
            }

            is SeparatedList -> {
                val t = typeForRuleItem(ruleItem, true)
                when {
                    t.declaration == SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> createUniquePropertyDeclarationAndAssignment(et, cor, propertyNameFor(et, ruleItem.item, t.declaration), t, childIndex)
                }
            }

            is Group -> {
                val gt = typeForGroup(ruleItem, true)
                when (gt.declaration) {
                    SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val content = ruleItem.groupedContent
                        val pName = when (content) {
                            is Choice -> propertyNameFor(et, content, gt.declaration)
                            else -> propertyNameFor(et, ruleItem, gt.declaration)
                        }

                        createUniquePropertyDeclarationAndAssignment(et, cor, pName, gt, childIndex)
                    }
                }
            }

            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun createPropertyAssignmentForReferencedRule(refRule: GrammarRule?, et: StructuredType, cor: CreateObjectRuleSimple, ruleItem: SimpleItem, childIndex: Int) {
        val rhs = refRule?.rhs
        when (rhs) {
            is Terminal -> {
                val t = SimpleTypeModelStdLib.String
                val pName = propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, t, childIndex)
            }

            is Concatenation -> {
                val t = typeForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, t.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, t, childIndex)
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
                    val pName = propertyNameFor(et, ruleItem, propType.declaration)
                    createUniquePropertyDeclarationAndAssignment(et, cor, pName, propType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = trRuleForChoiceRule(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, choiceType, childIndex)
            }

            else -> {
                val propType = typeForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, propType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, propType, childIndex)
            }
        }
    }

    //TODO: combine with above by passing in TypeModel
    private fun createPropertyDeclarationForEmbedded(et: StructuredType, cor: CreateObjectRuleSimple, ruleItem: Embedded, childIndex: Int) {
        val embBldr = grammarTypeNamespaceBuilderForEmbedded(ruleItem) //TODO: configuration
        val refRule = ruleItem.referencedRule(ruleItem.embeddedGrammarReference.resolved!!) //TODO: check for null
        val rhs = refRule.rhs
        when (rhs) {
            is Terminal -> createUniquePropertyDeclarationAndAssignment(
                et,
                cor,
                propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.declaration),
                SimpleTypeModelStdLib.String,
                childIndex
            )

            is Concatenation -> {
                val t = embBldr.typeForRuleItem(ruleItem, true)
                createUniquePropertyDeclarationAndAssignment(et, cor, propertyNameFor(et, ruleItem, t.declaration), t, childIndex)
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
                    createUniquePropertyDeclarationAndAssignment(et, cor, propertyNameFor(et, ruleItem, propType.declaration), propType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = embBldr.typeForChoiceRule(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, choiceType, childIndex)
            }

            else -> {
                val propType = embBldr.typeForRuleItem(ruleItem, true)
                createUniquePropertyDeclarationAndAssignment(et, cor, propertyNameFor(et, ruleItem, propType.declaration), propType, childIndex)
            }
        }
    }

    private fun propertyNameFor(et: StructuredType, ruleItem: RuleItem, ruleItemType: TypeDeclaration): String {
        return when (configuration) {
            null -> when (ruleItem) {
                is EmptyRule -> error("should not happen")
                is Terminal -> when (ruleItemType) {
                    is PrimitiveType -> GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
                    is CollectionType -> GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME
                    is TupleType -> GrammarTypeNamespaceFromGrammar.UNNAMED_TUPLE_PROPERTY_NAME
                    else -> GrammarTypeNamespaceFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME
                }

                is Embedded -> ruleItem.embeddedGoalName.lower()
                //is Embedded -> "${ruleItem.embeddedGrammarReference.resolved!!.name}_${ruleItem.embeddedGoalName}"
                is NonTerminal -> ruleItem.name
                is Group -> GrammarTypeNamespaceFromGrammar.UNNAMED_GROUP_PROPERTY_NAME
                else -> error("Internal error, unhandled subtype of SimpleItem")
            }

            else -> configuration.propertyNameFor(contextGrammar, ruleItem, ruleItemType)
        }
    }

    private fun createUniquePropertyDeclarationAndAssignment(et: StructuredType, cor: CreateObjectRuleSimple, name: String, type: TypeInstance, childIndex: Int) {
        val uniqueName = createUniquePropertyNameFor(et, name)
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        val rhs = NavigationDefault(listOf("child[$childIndex]"))
        val pd = et.appendPropertyStored(uniqueName, type, characteristics, childIndex)
        cor.appendAssignment(lhsPropertyName = uniqueName, rhs = rhs, rhsType = type)
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