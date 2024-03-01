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
import net.akehurst.language.agl.language.asmTransform.*
import net.akehurst.language.agl.language.expressions.IndexOperationDefault
import net.akehurst.language.agl.language.expressions.LiteralExpressionDefault
import net.akehurst.language.agl.language.expressions.NavigationDefault
import net.akehurst.language.agl.language.expressions.RootExpressionDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.language.asmTransform.TransformationRule
import net.akehurst.language.api.language.grammar.*
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple

class ConstructAndModify<TP : DataType, TR : TransformationRuleAbstract>(
    val construct: (TP) -> TR,
    val modify: (TR) -> Unit
)

class GrammarNamespaceAndAsmTransformBuilderFromGrammar(
    val typeModel: TypeModel,
    val grammar: Grammar,
    val configuration: Grammar2TypeModelMapping? = TypeModelFromGrammar.defaultConfiguration
) {

    companion object {
        fun TypeInstance.toNoActionTrRule() = this.let { t -> NothingTransformationRuleSimple().also { it.resolveTypeAs(SimpleTypeModelStdLib.NothingType) } }
        fun TypeInstance.toSelfAssignChild0TrRule() = this.let { t -> Child0AsStringTransformationRuleSimple().also { it.resolveTypeAs(t) } }
        fun TypeInstance.toListTrRule() = this.let { t -> ListTransformationRuleSimple().also { it.resolveTypeAs(t) } }
        fun TypeInstance.toSubtypeTrRule() = this.let { t -> SubtypeTransformationRuleSimple(t.typeName).also { it.resolveTypeAs(t) } }
        fun TypeInstance.toUnnamedSubtypeTrRule() = this.let { t -> UnnamedSubtypeTransformationRuleSimple().also { it.resolveTypeAs(t) } }
    }

    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)
    val transformModel = AsmTransformModelSimple(grammar.qualifiedName)
    val namespace = typeModel.namespace[grammar.qualifiedName] as GrammarTypeNamespaceSimple? ?: let {
        val ns = GrammarTypeNamespaceSimple(
            qualifiedName = grammar.qualifiedName,
            imports = mutableListOf(SimpleTypeModelStdLib.qualifiedName)
        )
        typeModel.addAllNamespace(listOf(ns))
        ns
    }

    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredType, String>, Int>()
    private val _grRuleNameToTrRule = mutableMapOf<String, TransformationRule>()
    private val _grRuleItemToTrRule = mutableMapOf<RuleItem, TransformationRule>()

    fun build() {
        val nonSkipRules = grammar.allResolvedGrammarRule.filter { it.isSkip.not() }
        for (gr in nonSkipRules) {
            createOrFindTrRuleForGrammarRule(gr)
        }
        // TODO: why iterate here?...just add to transformModel & namespace when first created !
        this._grRuleNameToTrRule.entries.forEach {
            val key = it.key
            val value = it.value
            transformModel.addRule(value)
            namespace.allRuleNameToType[key] = value.resolvedType
        }
        transformModel.typeModel = typeModel
    }

    private fun <TP : DataType, TR : TransformationRuleAbstract> findOrCreateTrRule(rule: GrammarRule, ifCreate: ConstructAndModify<TP, TR>): TransformationRule {
        val ruleName = rule.name
        val existing = _grRuleNameToTrRule[ruleName]
        return if (null == existing) {
            val tn = this.configuration?.typeNameFor(rule) ?: ruleName
            val tp = namespace.findOwnedOrCreateDataTypeNamed(tn) // DataTypeSimple(this, elTypeName)
            val tt = tp.type()
            val tr = ifCreate.construct(tp as TP)
            tr.grammarRuleName = rule.name
            tr.resolveTypeAs(tt)
            _grRuleNameToTrRule[ruleName] = tr
            ifCreate.modify(tr)
            tr
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
                val trRule = t.toSelfAssignChild0TrRule()
                (trRule as TransformationRuleAbstract).grammarRuleName = gr.name
                _grRuleNameToTrRule[gr.name] = trRule
                trRule
            }

            else -> {
                val rhs = gr.rhs
                val trRule = when (rhs) {
                    is EmptyRule -> trRuleForRuleItemList(gr, emptyList())
                    is Terminal -> trRuleForRuleItemList(gr, listOf(rhs))
                    is NonTerminal -> trRuleForRuleItemList(gr, listOf(rhs))
                    is Embedded -> trRuleForRuleItemList(gr, listOf(rhs))
                    is Concatenation -> trRuleForRuleItemList(gr, rhs.items)
                    is Choice -> trRuleForChoiceRule(rhs, gr)
                    is OptionalItem -> trRuleForRuleItemList(gr, listOf(rhs))
                    is SimpleList -> trRuleForRuleItemList(gr, listOf(rhs)) //findOrCreateTrRule(gr) { t-> t.type().toListTrRule()}  //
                    is SeparatedList -> trRuleForRuleItemList(gr, listOf(rhs))
                    is Group -> trRuleForGroup(rhs, false)
                    else -> error("Internal error, unhandled subtype of rule '${gr.name}'.rhs '${rhs::class.simpleName}' when creating TypeNamespace from grammar '${grammar.qualifiedName}'")
                }
                (trRule as TransformationRuleAbstract).grammarRuleName = gr.name
                _grRuleNameToTrRule[gr.name] = trRule
                trRule
            }
        }
    }

    private fun trRuleForRuleItemList(rule: GrammarRule, items: List<RuleItem>): TransformationRule {
        val trRule = findOrCreateTrRule(rule, ConstructAndModify({ CreateObjectRuleSimple(it.name) }, { tr ->
            items.forEachIndexed { idx, it -> createPropertyDeclarationAndAssignment(tr, it, idx) }
        }))
        return trRule
    }

    private fun trRuleForChoiceRule(choice: Choice, choiceRule: GrammarRule): TransformationRule {
        val subtypeTransforms = choice.alternative.map { trRuleForRuleItem(it, false) }
        return when {
            subtypeTransforms.all { it.resolvedType == SimpleTypeModelStdLib.NothingType } -> {
                val t = SimpleTypeModelStdLib.NothingType.declaration.type(emptyList(), subtypeTransforms.any { it.resolvedType.isNullable })
                t.toNoActionTrRule()
            }

            subtypeTransforms.all { it.resolvedType.declaration is PrimitiveType } -> {
                SimpleTypeModelStdLib.String.toSelfAssignChild0TrRule()
            }

            subtypeTransforms.all { it.resolvedType.declaration is DataType } -> findOrCreateTrRule(
                choiceRule,
                ConstructAndModify({ SubtypeTransformationRuleSimple(it.name) }, { tr ->
                    val tp = tr.resolvedType.declaration as DataType
                    subtypeTransforms.forEach {
                        (it.resolvedType.declaration as DataType).addSupertype(tp.name)
                        tp.addSubtype(it.resolvedType.declaration.name)
                    }
                })
            )

            subtypeTransforms.all { it.resolvedType.declaration == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.type(listOf(itemType))
                choiceType.toSubtypeTrRule()
            }

            subtypeTransforms.all { it.resolvedType.declaration is TupleType } -> when {
                1 == subtypeTransforms.map { (it.resolvedType.declaration as TupleType).property.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypeTransforms.first()
                    when {
                        t.resolvedType.declaration is TupleType && (t.resolvedType.declaration as TupleType).property.isEmpty() -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                        else -> t
                    }
                }

                else -> namespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type().toUnnamedSubtypeTrRule()
            }

            else -> namespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type().toUnnamedSubtypeTrRule()
        }
    }

    // Type for a GrammarRule is in some cases different to type for a rule item when part of something else in a rule
    private fun trRuleForRuleItem(ruleItem: RuleItem, forProperty: Boolean): TransformationRule {
        val existing = _grRuleItemToTrRule[ruleItem]
        return if (null != existing) {
            existing
        } else {
            val trRule: TransformationRule = when (ruleItem) {
                is EmptyRule -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                is Terminal -> if (forProperty) SimpleTypeModelStdLib.NothingType.toNoActionTrRule() else SimpleTypeModelStdLib.String.toSelfAssignChild0TrRule()
                is NonTerminal -> {
                    val refRule = ruleItem.referencedRuleOrNull(this.grammar)
                    when {
                        null == refRule -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                        refRule.isLeaf -> SimpleTypeModelStdLib.String.toSelfAssignChild0TrRule()
                        refRule.rhs is EmptyRule -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                        else -> createOrFindTrRuleForGrammarRule(refRule)
                    }
                }

                is Embedded -> {
                    val embBldr = builderForEmbedded(ruleItem)
                    val embNs = embBldr.namespace
                    embNs.findTypeForRule(ruleItem.name)?.toNoActionTrRule() //TODO: needs own action
                        ?: error("Should never happen")
                }

                is Choice -> trRuleForChoiceRuleItem(ruleItem, forProperty)
                is Concatenation -> trRuleForTupleType(ruleItem, ruleItem.items)
                is Group -> trRuleForGroup(ruleItem, forProperty)
                is OptionalItem -> {
                    val trRule = trRuleForRuleItem(ruleItem.item, forProperty) //TODO: could cause recursion overflow
                    when (trRule.resolvedType.declaration) {
                        SimpleTypeModelStdLib.NothingType.declaration -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()

                        else -> {
                            val optType = trRule.resolvedType.declaration.type(emptyList(), true)
                            val optTr = OptionalItemTransformationRuleSimple(optType.qualifiedTypeName).also { it.resolveTypeAs(optType) }
                            _grRuleItemToTrRule[ruleItem] = optTr
                            optTr
                        }
                    }
                }

                is SimpleList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeInstance>()
                    val t = SimpleTypeModelStdLib.List.type(typeArgs).toListTrRule()
                    _grRuleItemToTrRule[ruleItem] = t
                    val trRuleForItem = trRuleForRuleItem(ruleItem.item, forProperty)
                    when (trRuleForItem.resolvedType.declaration) {
                        SimpleTypeModelStdLib.NothingType.declaration -> {
                            _grRuleItemToTrRule.remove(ruleItem)
                            SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                        }

                        else -> {
                            typeArgs.add(trRuleForItem.resolvedType)
                            t
                        }
                    }
                }

                is SeparatedList -> {
                    // assign type to rule item before getting arg types to avoid recursion overflow
                    val typeArgs = mutableListOf<TypeInstance>()
                    val t = SimpleTypeModelStdLib.ListSeparated.type(typeArgs).toListTrRule() //TODO: needs action for sep-lists!
                    _grRuleItemToTrRule[ruleItem] = t
                    val trRuleForItem = trRuleForRuleItem(ruleItem.item, forProperty)
                    val trRuleForSep = trRuleForRuleItem(ruleItem.separator, forProperty)
                    when {
                        trRuleForItem.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> {
                            _grRuleItemToTrRule.remove(ruleItem)
                            SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                        }

                        trRuleForSep.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> {
                            val lt = SimpleTypeModelStdLib.List.type(listOf(trRuleForItem.resolvedType)).toListTrRule()
                            _grRuleItemToTrRule[ruleItem] = lt
                            lt
                        }

                        else -> {
                            typeArgs.add(trRuleForItem.resolvedType)
                            typeArgs.add(trRuleForSep.resolvedType)
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

    private fun trRuleForChoiceRuleItem(choice: Choice, forProperty: Boolean): TransformationRule {
        val subtypeTransforms = choice.alternative.map { trRuleForRuleItem(it, forProperty) }
        return when {
            subtypeTransforms.all { it.resolvedType == SimpleTypeModelStdLib.NothingType } -> {
                val t = SimpleTypeModelStdLib.NothingType.declaration.type(emptyList(), subtypeTransforms.any { it.resolvedType.isNullable })
                t.toNoActionTrRule()
            }

            subtypeTransforms.all { it.resolvedType.declaration is PrimitiveType } -> SimpleTypeModelStdLib.String.toNoActionTrRule()
            subtypeTransforms.all { it.resolvedType.declaration is DataType } -> namespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type()
                .toSubtypeTrRule()

            subtypeTransforms.all { it.resolvedType.declaration == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.type(listOf(itemType))
                choiceType.toSubtypeTrRule() //TODO: ??
            }

            subtypeTransforms.all { it.resolvedType.declaration is TupleType } -> when {
                1 == subtypeTransforms.map { (it.resolvedType.declaration as TupleType).property.map { Pair(it.name, it) }.toSet() }.toSet().size -> {
                    val t = subtypeTransforms.first()
                    when {
                        t.resolvedType.declaration is TupleType && (t.resolvedType.declaration as TupleType).property.isEmpty() -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                        else -> t
                    }
                }

                else -> namespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type().toSubtypeTrRule()
            }

            else -> namespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type().toSubtypeTrRule()
        }
    }

    private fun trRuleForGroup(group: Group, forProperty: Boolean): TransformationRule {
        val content = group.groupedContent
        return when (content) {
            is Choice -> trRuleForChoiceRuleItem(content, forProperty)
            else -> {
                val items = when (content) {
                    is Concatenation -> content.items
                    else -> listOf(content)
                }
                trRuleForTupleType(group, items)
            }
        }
    }

    private fun trRuleForTupleType(ruleItem: RuleItem, items: List<RuleItem>): TransformationRule {
        val tt = namespace.createTupleType()
        val ti = namespace.createTupleTypeInstance(tt, emptyList(), false)
        val cor = CreateObjectRuleSimple(ti.typeName).also { it.resolveTypeAs(ti) }
        this._grRuleItemToTrRule[ruleItem] = cor
        items.forEachIndexed { idx, it -> createPropertyDeclarationAndAssignment(cor, it, idx) }
        return when {
            tt.allProperty.isEmpty() -> {
                val tr = SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                this._grRuleItemToTrRule[ruleItem] = tr
                tr
            }

            else -> cor
        }
    }

    private fun createPropertyDeclarationAndAssignment(cor: TransformationRuleAbstract, ruleItem: RuleItem, childIndex: Int) {
        val et: StructuredType = cor.resolvedType.declaration as StructuredType
        when (ruleItem) {
            is EmptyRule -> Unit
            is Terminal -> Unit //createUniquePropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is Embedded -> createPropertyDeclarationForEmbedded(et, cor, ruleItem, childIndex)

            is NonTerminal -> {
                val refRule = ruleItem.referencedRuleOrNull(this.grammar)
                createPropertyDeclarationAndAssignmentForReferencedRule(refRule, et, cor, ruleItem, childIndex)
            }

            is Concatenation -> TODO("Concatenation")
            is Choice -> {
                val tr = trRuleForRuleItem(ruleItem, true)
                when (tr.resolvedType.declaration) {
                    SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val n = propertyNameFor(et, ruleItem, tr.resolvedType.declaration)
                        createUniquePropertyDeclarationAndAssignment(et, cor, n, tr.resolvedType, childIndex)
                    }
                }
            }

            is OptionalItem -> {
                val t = trRuleForRuleItem(ruleItem, true)
                when {
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val pName = propertyNameFor(et, ruleItem.item, t.resolvedType.declaration)
                        createUniquePropertyDeclarationAndAssignment(et, cor, pName, t.resolvedType, childIndex)
                    }
                }
            }

            is SimpleList -> {
                val t = trRuleForRuleItem(ruleItem, true)
                when {
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val pName = propertyNameFor(et, ruleItem.item, t.resolvedType.declaration)
                        createUniquePropertyDeclarationAndAssignment(et, cor, pName, t.resolvedType, childIndex)
                    }
                }
            }

            is SeparatedList -> {
                val t = trRuleForRuleItem(ruleItem, true)
                when {
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val pName = propertyNameFor(et, ruleItem.item, t.resolvedType.declaration)
                        createUniquePropertyDeclarationAndAssignment(et, cor, pName, t.resolvedType, childIndex)
                    }
                }
            }

            is Group -> {
                val gt = trRuleForGroup(ruleItem, true)
                when (gt.resolvedType.declaration) {
                    SimpleTypeModelStdLib.NothingType.declaration -> Unit
                    else -> {
                        val content = ruleItem.groupedContent
                        val pName = when (content) {
                            is Choice -> propertyNameFor(et, content, gt.resolvedType.declaration)
                            else -> propertyNameFor(et, ruleItem, gt.resolvedType.declaration)
                        }

                        createUniquePropertyDeclarationAndAssignment(et, cor, pName, gt.resolvedType, childIndex)
                    }
                }
            }

            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun createPropertyDeclarationAndAssignmentForReferencedRule(
        refRule: GrammarRule?,
        et: StructuredType,
        cor: TransformationRuleAbstract,
        ruleItem: SimpleItem,
        childIndex: Int
    ) {
        val rhs = refRule?.rhs
        when (rhs) {
            is Terminal -> {
                val t = SimpleTypeModelStdLib.String
                val pName = propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, t, childIndex)
            }

            is Concatenation -> {
                val t = trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, t.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, t.resolvedType, childIndex)
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
                    val propType = trRuleForRuleItem(rhs, true) //to get list type
                    val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                    createUniquePropertyDeclarationAndAssignment(et, cor, pName, propType.resolvedType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = trRuleForChoiceRule(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, choiceType.resolvedType, childIndex)
            }

            else -> {
                val propType = trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, propType.resolvedType, childIndex)
            }
        }
    }

    //TODO: combine with above by passing in TypeModel
    private fun createPropertyDeclarationForEmbedded(et: StructuredType, cor: TransformationRuleAbstract, ruleItem: Embedded, childIndex: Int) {
        val embBldr = builderForEmbedded(ruleItem) //TODO: configuration
        val refRule = ruleItem.referencedRule(ruleItem.embeddedGrammarReference.resolved!!) //TODO: check for null
        val rhs = refRule.rhs
        when (rhs) {
            is Terminal -> {
                val pName = propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, SimpleTypeModelStdLib.String, childIndex)
            }

            is Concatenation -> {
                val t = embBldr.trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, t.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, t.resolvedType, childIndex)
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
                    val propType = embBldr.trRuleForRuleItem(rhs, true) //to get list type
                    val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                    createUniquePropertyDeclarationAndAssignment(et, cor, pName, propType.resolvedType, childIndex)
                }
            }

            is Choice -> {
                val choiceType = embBldr.trRuleForChoiceRule(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, choiceType.resolvedType, childIndex)
            }

            else -> {
                val propType = embBldr.trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, cor, pName, propType.resolvedType, childIndex)
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

            else -> configuration.propertyNameFor(this.grammar, ruleItem, ruleItemType)
        }
    }

    private fun createUniquePropertyDeclarationAndAssignment(et: StructuredType, trRule: TransformationRuleAbstract, name: String, type: TypeInstance, childIndex: Int) {
        val uniqueName = createUniquePropertyNameFor(et, name)
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        val rhs = NavigationDefault(
            start = RootExpressionDefault("child"),
            parts = listOf(IndexOperationDefault(listOf(LiteralExpressionDefault(LiteralExpressionDefault.INTEGER, childIndex))))
        )
        val pd = et.appendPropertyStored(uniqueName, type, characteristics, childIndex)
        (trRule as TransformationRuleAbstract).appendAssignment(lhsPropertyName = uniqueName, rhs = rhs)
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