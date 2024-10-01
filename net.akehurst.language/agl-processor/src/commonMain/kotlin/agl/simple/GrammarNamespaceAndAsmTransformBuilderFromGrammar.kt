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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.grammarTypeModel.GrammarTypeNamespaceSimple
import net.akehurst.language.transform.asm.*
import net.akehurst.language.expressions.asm.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.api.TransformationRule
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.AssignmentStatement
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammar.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.DataTypeSimple
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.TypeArgumentNamedSimple
import kotlin.reflect.KClass


internal class ConstructAndModify<TP : DataType, TR : TransformationRuleAbstract>(
    val construct: (TP) -> TR,
    val modify: (TR) -> Unit
)

/**
 * A GrammarModel contains a number of Grammar Definitions grouped into various namespaces.
 * Each Grammar maps to:
 *   - A TransformRuleSet with a TransformRule for each GrammarRule.
 *   - A TypeNamespace that contains the type-definition of the result of each GrammarRules TransformRule
 *
 *   The default mapping is as follows:
 *   transform GrammarModel2TransformModel {
 *     relate Grammar2Namespaces {
 *       grm : Grammar {
 *          ns <-> namespace.qualifiedName
 *          gqn <-> qualifiedName
 *       }
 *       tgt: Tuple(tpNs:TypeNamespace, trNs:TransformNamespace, trRs:TransformRuleSet) {
 *          tpNs <-> TypeNamespace { gqn <-> qualifiedName }
 *          trNs <-> TransformNamespace {  ns <-> qualifiedName  }
 *       }
 *       where {  Grammar2TransformRuleSet(grm, tgt.trRs)  }
 *     }
 *
 *     relate Grammar2TransformRuleSet {
 *        grm : Grammar { n <-> name }
 *        trRs:TransformRuleSet { n <-> name }
 *        where { GrammarRule2TransformRule.foreachOf(grm.grammarRules, trRs.rules) }
 *     }
 *
 *     relate GrammarRule2TransformRule {
 *       gr : GrammarRule
 *       tr : TransformRule
 *     }
 *   }
 *
 */
internal class GrammarModel2TransformModel(
    /** base type model, could be empty if can create types, else must hold all tyeps needed **/
    val typeModel: TypeModel,
    val grammarModel: GrammarModel,
    val configuration: Grammar2TypeModelMapping? = Grammar2TransformRuleSet.defaultConfiguration
) {
    val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    fun build(): TransformModel {
        val transformNamespaces = mutableListOf<TransformNamespace>()
        for (grammar in grammarModel.allDefinitions) {
            val rel = Grammar2Namespaces(issues, typeModel, grammar, configuration)
            rel.build()
            transformNamespaces.add(rel.trNs!!)
        }
        return TransformModelDefault(
            name = grammarModel.allDefinitions.last().name,
            typeModel = typeModel,
            namespace = transformNamespaces
        )
    }

}

internal class Grammar2Namespaces(
    val issues: IssueHolder,
    val typeModel: TypeModel,
    val grammar: Grammar,
    val configuration: Grammar2TypeModelMapping?
) {
    var tpNs: GrammarTypeNamespace? = null
    var trNs: TransformNamespace? = null
    var g2rs: Grammar2TransformRuleSet? = null

    fun build() {
        val ns = grammar.namespace.qualifiedName
        val gqn = grammar.qualifiedName
        tpNs = finsOrCreateGrammarNamespace(gqn)//.append(SimpleName("asm")))
        trNs = TransformNamespaceDefault(ns)//.append(SimpleName("transform")))

        g2rs = Grammar2TransformRuleSet(issues, typeModel, tpNs!!, trNs!!, grammar, configuration)
        val rs = g2rs!!.build()
        (trNs as TransformNamespaceDefault).addDefinition(rs)
    }

    private fun finsOrCreateGrammarNamespace(qualifiedName: QualifiedName) =
        typeModel.findNamespaceOrNull(qualifiedName) as GrammarTypeNamespaceSimple?
            ?: let {
                val ns = GrammarTypeNamespaceSimple(
                    qualifiedName = qualifiedName,
                    imports = mutableListOf(Import(SimpleTypeModelStdLib.qualifiedName.value))
                )
                typeModel.addAllNamespaceAndResolveImports(listOf(SimpleTypeModelStdLib, ns))
                ns
            }

}

internal class Grammar2TransformRuleSet(
    val issues: IssueHolder,
    val typeModel: TypeModel,
    val grammarTypeNamespace: GrammarTypeNamespace,
    val transformNamespace: TransformNamespace,
    val grammar: Grammar,
    val configuration: Grammar2TypeModelMapping?
) {

    companion object {
        val defaultConfiguration = TypeModelFromGrammarConfigurationDefault()

        val UNNAMED_PRIMITIVE_PROPERTY_NAME = PropertyName("\$value")
        val UNNAMED_LIST_PROPERTY_NAME = PropertyName("\$list")
        val UNNAMED_TUPLE_PROPERTY_NAME = PropertyName("\$tuple")
        val UNNAMED_GROUP_PROPERTY_NAME = PropertyName("\$group")
        val UNNAMED_CHOICE_PROPERTY_NAME = PropertyName("\$choice")


        fun TypeInstance.toNoActionTrRule() = this.let { t -> transformationRule(t, RootExpressionSimple.NOTHING) }
        fun TypeInstance.toLeafAsStringTrRule() = this.let { t -> transformationRule(t, RootExpressionSimple("leaf")) }
        fun TypeInstance.toListTrRule() = this.let { t -> transformationRule(t, RootExpressionSimple("children")) }
        fun TypeInstance.toSListItemsTrRule() =
            this.let { t -> transformationRule(t, NavigationSimple(RootExpressionSimple("children"), listOf(PropertyCallSimple(PropertyName("items"))))) }

        fun TypeInstance.toSubtypeTrRule() = this.let { t -> transformationRule(t, EXPRESSION_CHILD(0)) }
        fun TypeInstance.toUnnamedSubtypeTrRule() = this.let { t -> transformationRule(t, EXPRESSION_CHILD(0)) }

        fun EXPRESSION_CHILD(childIndex: Int) = NavigationSimple(
            start = RootExpressionSimple("child"),
            parts = listOf(IndexOperationSimple(listOf(LiteralExpressionSimple(LiteralExpressionSimple.INTEGER, childIndex))))
        )

        fun EXPRESSION_CHILD_i_prop(childIndex: Int, pName: PropertyName) = NavigationSimple(
            start = RootExpressionSimple("child"),
            parts = listOf(
                IndexOperationSimple(listOf(LiteralExpressionSimple(LiteralExpressionSimple.INTEGER, childIndex))),
                PropertyCallSimple(pName)
            )
        )

        val EXPRESSION_CHILDREN = RootExpressionSimple("children")

        private fun List<TransformationRule>.allOfType(typeDecl: TypeDeclaration) = this.all { it.resolvedType.declaration == typeDecl }
        private fun List<TransformationRule>.allOfTypeIs(klass: KClass<*>) = this.all { klass.isInstance(it.resolvedType.declaration) }
        private fun List<TransformationRule>.allTupleTypesMatch() =
            1 == this.map { tr -> (tr.resolvedType as TupleTypeInstance).typeArguments.toSet() }.toSet().size
    }

    private val _uniquePropertyNames = mutableMapOf<Pair<StructuredType, PropertyName>, Int>()
    private val _grRuleNameToTrRule = mutableMapOf<GrammarRuleName, TransformationRule>()
    private val _grRuleItemToTrRule = mutableMapOf<RuleItem, TransformationRule>()

    fun build(): TransformRuleSet {
        val nonSkipRules = grammar.allResolvedGrammarRule.filter { it.isSkip.not() }
        for (gr in nonSkipRules) {
            createOrFindTrRuleForGrammarRule(gr)
        }
        val trRules = mutableListOf<TransformationRule>()
        // TODO: why iterate here?...just add to transformModel & namespace when first created !
        this._grRuleNameToTrRule.entries.forEach {
            val key = it.key
            val value = it.value
            trRules.add(value)
            (grammarTypeNamespace as GrammarTypeNamespaceSimple).allRuleNameToType[key] = value.resolvedType
        }
        return TransformRuleSetDefault(transformNamespace, grammar.name, trRules)
    }

    private fun <TP : DataType, TR : TransformationRuleAbstract> findOrCreateTrRule(rule: GrammarRule, cnm: ConstructAndModify<TP, TR>): TransformationRule {
        val ruleName = rule.name
        val existing = _grRuleNameToTrRule[ruleName]
        return if (null == existing) {
            val tn = this.configuration?.typeNameFor(rule) ?: SimpleName(ruleName.value)
            val tp = grammarTypeNamespace.findOwnedOrCreateDataTypeNamed(tn) // DataTypeSimple(this, elTypeName)
            val tt = tp.type()
            val tr = cnm.construct(tp as TP)
            tr.grammarRuleName = rule.name
            tr.resolveTypeAs(tt)
            _grRuleNameToTrRule[ruleName] = tr
            cnm.modify(tr)
            tr
        } else {
            existing
        }
    }

    private fun builderForEmbedded(ruleItem: Embedded): Grammar2Namespaces {
        val embGrammar = ruleItem.embeddedGrammarReference.resolved!!
        val g2ns = Grammar2Namespaces(issues, typeModel, embGrammar, this.configuration)
        g2ns.build()
        //if (null != typeModel.findNamespaceOrNull(g2ns.grNs!!.qualifiedName)) {
        //already added
        //} else {
        //     embBldr.build()
        //     (typeModel as TypeModelSimple).addNamespace(grNs)
        // }
        grammarTypeNamespace.addImport(Import(g2ns.tpNs!!.qualifiedName.value))
        return g2ns
    }

    private fun createOrFindTrRuleForGrammarRule(gr: GrammarRule): TransformationRule {
        val cor = _grRuleNameToTrRule[gr.name]
        return when {
            null != cor -> cor
            gr.isLeaf -> {
                val t = SimpleTypeModelStdLib.String
                val trRule = t.toLeafAsStringTrRule()
                (trRule as TransformationRuleAbstract).grammarRuleName = gr.name
                _grRuleNameToTrRule[gr.name] = trRule
                trRule
            }

            else -> {
                val rhs = gr.rhs
                val trRule =  trRuleForRhs(gr,rhs)
                (trRule as TransformationRuleAbstract).grammarRuleName = gr.name
                _grRuleNameToTrRule[gr.name] = trRule
                trRule
            }
        }
    }

    private fun trRuleForRhs(gr: GrammarRule, rhs:RuleItem) = when (rhs) {
        is EmptyRule -> trRuleForRhsItemList(gr, emptyList())
        is Terminal -> trRuleForRhsItemList(gr, listOf(rhs))
        is NonTerminal -> trRuleForRhsItemList(gr, listOf(rhs))
        is Embedded -> trRuleForRhsItemList(gr, listOf(rhs))
        is Concatenation -> trRuleForRhsItemList(gr, rhs.items)
        is Choice -> trRuleForRhsChoice(rhs, gr)
        is OptionalItem -> trRuleForRhsOptional(gr, rhs)
        is SimpleList -> trRuleForRhsListSimple(gr, rhs)
        is SeparatedList -> trRuleForRhsListSeparated(gr, rhs)
        is Group -> trRuleForRhsGroup(gr, rhs)
        else -> error("Internal error, unhandled subtype of rule '${gr.name}'.rhs '${rhs::class.simpleName}' when creating TypeNamespace from grammar '${grammar.qualifiedName}'")
    }

    private fun trRuleForRhsItemList(rule: GrammarRule, items: List<RuleItem>): TransformationRule {
        val trRule = findOrCreateTrRule(
            rule, ConstructAndModify(
                construct = {
                    transformationRule(
                        type = it.type(),
                        expression = CreateObjectExpressionSimple(it.qualifiedName, emptyList())
                    )
                },
                modify = { tr ->
                    val ass = items.mapIndexedNotNull { idx, it ->
                        createPropertyDeclarationAndAssignment(tr.resolvedType.declaration as StructuredType, it, idx)
                    }
                    (tr.expression as CreateObjectExpressionSimple).propertyAssignments = ass
                })
        )
        return trRule
    }

    private fun trRuleForRhsChoice(choice: Choice, choiceRule: GrammarRule): TransformationRule {
        return when (choice.alternative.size) {
            1 -> error("Internal Error: choice should have more than one alternative")
            else -> {
                val subtypeTransforms = choice.alternative.map { trRuleForRuleItem(it, false) }
                when {
                    subtypeTransforms.all { it.resolvedType == SimpleTypeModelStdLib.NothingType } -> {
                        val t = SimpleTypeModelStdLib.NothingType.declaration.type(emptyList(), subtypeTransforms.any { it.resolvedType.isNullable })
                        t.toNoActionTrRule()
                    }

                    subtypeTransforms.all { it.resolvedType.declaration is PrimitiveType } -> transformationRule(
                        type = SimpleTypeModelStdLib.String,
                        expression = EXPRESSION_CHILD(0)
                    )

                    subtypeTransforms.all { it.resolvedType.declaration is DataType } -> findOrCreateTrRule(
                        choiceRule,
                        ConstructAndModify(
                            construct = {
                                transformationRule(
                                    type = it.type(),
                                    expression = EXPRESSION_CHILD(0)
                                )
                            },
                            modify = { tr ->
                                val tp = tr.resolvedType.declaration as DataType
                                subtypeTransforms.forEach {
                                    (it.resolvedType.declaration as DataType).addSupertype(tp.qualifiedName)
                                    tp.addSubtype(it.resolvedType.declaration.qualifiedName)
                                }
                            })
                    )

                    subtypeTransforms.all { it.resolvedType.declaration == SimpleTypeModelStdLib.List } -> { //=== PrimitiveType.LIST } -> {
                        val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                        val choiceType = SimpleTypeModelStdLib.List.type(listOf(itemType.asTypeArgument))
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

                        else -> grammarTypeNamespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type().toUnnamedSubtypeTrRule()
                    }

                    else -> grammarTypeNamespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type().toUnnamedSubtypeTrRule()
                }
            }
        }
    }

    private fun trRuleForRhsOptional(rule: GrammarRule, optItem: OptionalItem): TransformationRule {
        val trRule = findOrCreateTrRule(
            rule, ConstructAndModify(
                construct = {
                    transformationRule(
                        type = it.type(),
                        expression = CreateObjectExpressionSimple(it.qualifiedName, emptyList())
                    )
                },
                modify = { tr ->
                    val et: StructuredType = tr.resolvedType.declaration as StructuredType
                    val t = trRuleForRuleItem(optItem, true)
                    val ass = when {
                        // no property if list of non-leaf literals
                        t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> null
                        else -> {
                            val childIndex = 0 //always first and only child
                            val pName = propertyNameFor(et, optItem.item, t.resolvedType.declaration)
                            val rhs = EXPRESSION_CHILD(childIndex)
                            createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, rhs)
                        }
                    }
                    (tr.expression as CreateObjectExpressionSimple).propertyAssignments = listOf(ass).filterNotNull()
                })
        )
        return trRule
    }

    private fun trRuleForRhsListSimple(rule: GrammarRule, listItem: SimpleList): TransformationRule {
        val trRule = findOrCreateTrRule(
            rule, ConstructAndModify(
                construct = {
                    transformationRule(
                        type = it.type(),
                        expression = CreateObjectExpressionSimple(it.qualifiedName, emptyList())
                    )
                },
                modify = { tr ->
                    val et: StructuredType = tr.resolvedType.declaration as StructuredType
                    val t = trRuleForRuleItem(listItem, true)
                    val ass = when {
                        // no property if list of non-leaf literals
                        t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> null
                        else -> {
                            val childIndex = 0 //always first and only child
                            val pName = propertyNameFor(et, listItem.item, t.resolvedType.declaration)
                            val rhs = EXPRESSION_CHILDREN
                            createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, rhs)
                        }
                    }
                    (tr.expression as CreateObjectExpressionSimple).propertyAssignments = listOf(ass).filterNotNull()
                })
        )
        return trRule
    }

    private fun trRuleForRhsListSeparated(rule: GrammarRule, listItem: SeparatedList): TransformationRule {
        val trRule = findOrCreateTrRule(rule, ConstructAndModify(
            construct = {
                transformationRule(
                    type = it.type(),
                    expression = CreateObjectExpressionSimple(it.qualifiedName, emptyList())
                )
            },
            modify = { tr ->
                val et: StructuredType = tr.resolvedType.declaration as StructuredType
                val t = trRuleForRuleItem(listItem, true)
                val ass = when {
                    // no property if list of non-leaf literals
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> null
                    else -> {
                        val childIndex = 0 //always first and only child
                        val pName = propertyNameFor(et, listItem.item, t.resolvedType.declaration)
                        val rhs = t.expression
                        createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, rhs)
                    }
                }
                (tr.expression as CreateObjectExpressionSimple).propertyAssignments = listOf(ass).filterNotNull()
            }
        ))
        return trRule
    }

    private fun trRuleForRhsGroup(rule: GrammarRule, group: Group): TransformationRule =
        trRuleForRuleItemGroup(group, false)

    // Type for a GrammarRule is in some cases different to type for a rule item when part of something else in a rule
    private fun trRuleForRuleItem(ruleItem: RuleItem, forProperty: Boolean): TransformationRule {
        val existing = _grRuleItemToTrRule[ruleItem]
        return if (null != existing) {
            existing
        } else {
            val trRule: TransformationRule = when (ruleItem) {
                is EmptyRule -> trRuleForRuleItemEmpty(ruleItem, forProperty)
                is Terminal -> trRuleForRuleItemTerminal(ruleItem, forProperty)
                is NonTerminal -> trRuleForRuleItemNonTerminal(ruleItem, forProperty)
                is Embedded -> trRuleForRuleItemEmbedded(ruleItem, forProperty)
                is Concatenation -> trRuleForRuleItemConcatenation(ruleItem, ruleItem.items)
                is Choice -> trRuleForRuleItemChoice(ruleItem, forProperty)
                is OptionalItem -> trRuleForRuleItemOptional(ruleItem, forProperty)
                is SimpleList -> trRuleForRuleItemListSimple(ruleItem, forProperty)
                is SeparatedList -> trRuleForRuleItemListSeparated(ruleItem, forProperty)
                is Group -> trRuleForRuleItemGroup(ruleItem, forProperty)
                else -> error("Internal error, unhandled subtype of RuleItem")
            }
            _grRuleItemToTrRule[ruleItem] = trRule
            trRule
        }
    }

    private fun trRuleForRuleItemEmpty(ruleItem: EmptyRule, forProperty: Boolean): TransformationRule {
        return SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
    }

    private fun trRuleForRuleItemTerminal(ruleItem: Terminal, forProperty: Boolean): TransformationRule {
        return if (forProperty) {
            SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
        } else {
            SimpleTypeModelStdLib.String.toLeafAsStringTrRule()
        }
    }

    private fun trRuleForRuleItemNonTerminal(ruleItem: NonTerminal, forProperty: Boolean): TransformationRule {
        val refRule = ruleItem.referencedRuleOrNull(this.grammar)
        return when {
            null == refRule -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
            refRule.isLeaf -> transformationRule(SimpleTypeModelStdLib.String, EXPRESSION_CHILD(0))//SimpleTypeModelStdLib.String.toLeafAsStringTrRule()
            refRule.rhs is EmptyRule -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
            else -> {
                val trForRefRule = createOrFindTrRuleForGrammarRule(refRule)
                transformationRule(trForRefRule.resolvedType, EXPRESSION_CHILD(0))
            }
        }
    }

    private fun trRuleForRuleItemEmbedded(ruleItem: Embedded, forProperty: Boolean): TransformationRule {
        val g2ns = builderForEmbedded(ruleItem)
        return g2ns.tpNs!!.findTypeForRule(ruleItem.embeddedGoalName)?.toNoActionTrRule() //TODO: needs own action
            ?: error("Should never happen")
    }

    private fun trRuleForRuleItemChoice(choice: Choice, forProperty: Boolean): TransformationRule {
        val subtypeTransforms = choice.alternative.map { trRuleForRuleItem(it, forProperty) }
        return when {
            subtypeTransforms.allOfType(SimpleTypeModelStdLib.NothingType.declaration) -> {
                val t = SimpleTypeModelStdLib.NothingType.declaration.type(emptyList(), subtypeTransforms.any { it.resolvedType.isNullable })
                t.toNoActionTrRule()
            }

            subtypeTransforms.allOfType(SimpleTypeModelStdLib.String.declaration) -> transformationRule(
                SimpleTypeModelStdLib.String,
                EXPRESSION_CHILD(0)
            )

            subtypeTransforms.allOfTypeIs(DataType::class) -> grammarTypeNamespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type()
                .toSubtypeTrRule()

            subtypeTransforms.allOfType(SimpleTypeModelStdLib.List) -> { //=== PrimitiveType.LIST } -> {
                val itemType = SimpleTypeModelStdLib.AnyType//TODO: compute better elementType ?
                val choiceType = SimpleTypeModelStdLib.List.type(listOf(itemType.asTypeArgument))
                choiceType.toSubtypeTrRule() //TODO: ??
            }

            subtypeTransforms.allOfTypeIs(TupleType::class) && subtypeTransforms.allTupleTypesMatch() -> {
                val t = subtypeTransforms.first()
                val rt = t.resolvedType
                when {
                    rt.declaration is TupleType && (rt as TupleTypeInstance).typeArguments.isEmpty() -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                    else -> t
                }
            }

            else -> {
                val subType = grammarTypeNamespace.createUnnamedSupertypeType(subtypeTransforms.map { it.resolvedType }).type()
                val options = subtypeTransforms.mapIndexed { idx, it ->
                    WhenOptionSimple(
                        condition = InfixExpressionSimple(
                            listOf(LiteralExpressionSimple(LiteralExpressionSimple.INTEGER, idx), RootExpressionSimple("\$alternative")),
                            listOf("==")
                        ),
                        expression = it.expression
                    )
                }
                transformationRule(
                    type = subType,
                    expression = WhenExpressionSimple(options)
                )
            }
        }
    }

    private var tupleCount = 0
    private fun trRuleForRuleItemConcatenation(ruleItem: RuleItem, items: List<RuleItem>): TransformationRule {
        //val cor = CreateTupleTransformationRuleSimple(ti.typeName).also { it.resolveTypeAs(ti) }
        val ttSub = DataTypeSimple(grammarTypeNamespace, SimpleName("TupleTypeSubstitute-${tupleCount++}"))
        val assignments = items.mapIndexedNotNull { idx, it -> createPropertyDeclarationAndAssignment(ttSub, it, idx) }
        val typeArgs = ttSub.property.map { TypeArgumentNamedSimple(it.name, it.typeInstance) }
        val ti = grammarTypeNamespace.createTupleTypeInstance(typeArgs, false)
        val tr = transformationRule(ti, CreateTupleExpressionSimple(assignments))
        this._grRuleItemToTrRule[ruleItem] = tr
        return when {
            ttSub.allProperty.isEmpty() -> {
                val trn = SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
                this._grRuleItemToTrRule[ruleItem] = tr
                trn
            }

            else -> tr
        }
    }

    private fun trRuleForRuleItemOptional(ruleItem: OptionalItem, forProperty: Boolean): TransformationRule {
        val trRule = trRuleForRuleItem(ruleItem.item, forProperty) //TODO: could cause recursion overflow
        return when (trRule.resolvedType.declaration) {
            SimpleTypeModelStdLib.NothingType.declaration -> SimpleTypeModelStdLib.NothingType.toNoActionTrRule()

            else -> {
                val optType = trRule.resolvedType.declaration.type(emptyList(), true)
                val expr = when (ruleItem.item) {
                    is Choice -> WithExpressionSimple(withContext = EXPRESSION_CHILD(0), expression = trRule.expression)
                    is OptionalItem -> WithExpressionSimple(withContext = EXPRESSION_CHILD(0), expression = trRule.expression)
                    is SimpleList -> WithExpressionSimple(withContext = EXPRESSION_CHILD(0), expression = trRule.expression)
                    is SeparatedList -> WithExpressionSimple(withContext = EXPRESSION_CHILD(0), expression = trRule.expression)
                    is Group -> WithExpressionSimple(withContext = EXPRESSION_CHILD(0), expression = trRule.expression)
                    else -> trRule.expression //EXPRESSION_CHILD(0)
                }
                val optTr = transformationRule(optType, expr)
                _grRuleItemToTrRule[ruleItem] = optTr
                optTr
            }
        }
    }

    private fun trRuleForRuleItemListSimple(ruleItem: SimpleList, forProperty: Boolean): TransformationRule {
        // assign type to rule item before getting arg types to avoid recursion overflow
        val typeArgs = mutableListOf<TypeArgument>()
        val t = SimpleTypeModelStdLib.List.type(typeArgs).toListTrRule()
        _grRuleItemToTrRule[ruleItem] = t
        val trRuleForItem = trRuleForRuleItem(ruleItem.item, forProperty)
        return when (trRuleForItem.resolvedType.declaration) {
            SimpleTypeModelStdLib.NothingType.declaration -> {
                _grRuleItemToTrRule.remove(ruleItem)
                SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
            }

            else -> {
                typeArgs.add(trRuleForItem.resolvedType.asTypeArgument)
                t
            }
        }
    }

    private fun trRuleForRuleItemListSeparated(ruleItem: SeparatedList, forProperty: Boolean): TransformationRule {
        // assign type to rule item before getting arg types to avoid recursion overflow
        val typeArgs = mutableListOf<TypeArgument>()
        val t = SimpleTypeModelStdLib.ListSeparated.type(typeArgs).toListTrRule() //TODO: needs action for sep-lists!
        _grRuleItemToTrRule[ruleItem] = t
        val trRuleForItem = trRuleForRuleItem(ruleItem.item, forProperty)
        val trRuleForSep = trRuleForRuleItem(ruleItem.separator, forProperty)
        return when {
            trRuleForItem.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> {
                _grRuleItemToTrRule.remove(ruleItem)
                SimpleTypeModelStdLib.NothingType.toNoActionTrRule()
            }

            trRuleForSep.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> {
                val lt = SimpleTypeModelStdLib.List.type(listOf(trRuleForItem.resolvedType.asTypeArgument)).toSListItemsTrRule()
                _grRuleItemToTrRule[ruleItem] = lt
                lt
            }

            else -> {
                typeArgs.add(trRuleForItem.resolvedType.asTypeArgument)
                typeArgs.add(trRuleForSep.resolvedType.asTypeArgument)
                t
            }
        }
    }

    private fun trRuleForRuleItemGroup(group: Group, forProperty: Boolean): TransformationRule {
        val content = group.groupedContent
        return when (content) {
            is Choice -> trRuleForRuleItemChoice(content, forProperty)
            is OptionalItem -> trRuleForGroupContentOptional(content)
            else -> {
               // val contentTr = trRuleForGroupContent(group, content)
                val items = when (content) {
                    is Concatenation -> content.items
                    else -> listOf(content)
                }
                trRuleForRuleItemConcatenation(group, items)
            }
        }
    }
/*
    private fun trRuleForGroupContent(group:Group, content:RuleItem) = when (content) {
        is EmptyRule -> trRuleForRuleItemConcatenation(group, emptyList())
        is Terminal -> trRuleForRuleItemConcatenation(group, listOf(content))
        is NonTerminal -> trRuleForRuleItemConcatenation(group,listOf(content))
        is Embedded -> trRuleForRuleItemConcatenation(group,listOf(content))
        is Concatenation -> trRuleForRuleItemConcatenation(group,content.items)
        //is Choice -> trRuleForRhsChoice(rhs, gr)
        is OptionalItem -> trRuleForRuleItemConcatenation(group,listOf(content.item))
        is SimpleList -> trRuleForRuleItemConcatenation(group,listOf(content))
        is SeparatedList -> trRuleForRhsListSeparated(gr, rhs)
        is Group -> trRuleForRhsGroup(gr, rhs)
        else -> error("Internal error, unhandled subtype of rule '${gr.name}'.rhs '${rhs::class.simpleName}' when creating TypeNamespace from grammar '${grammar.qualifiedName}'")
    }
    */

    private fun trRuleForGroupContentOptional(optItem: OptionalItem): TransformationRule {
        val ttSub = DataTypeSimple(grammarTypeNamespace, SimpleName("TupleTypeSubstitute-${tupleCount++}"))
        val assignment = createPropertyDeclarationAndAssignment(ttSub, optItem.item, 0)
            ?: error("No property assignment!")
        val typeArgs = ttSub.property.map {
            //Optional items are nullable, this is why need this special function
            TypeArgumentNamedSimple(it.name, it.typeInstance.nullable())
        }
        val ti = grammarTypeNamespace.createTupleTypeInstance(typeArgs, false)
        val tr = transformationRule(ti, CreateTupleExpressionSimple(listOf(assignment)))
        return tr
    }

    private fun createPropertyDeclarationAndAssignment(et: StructuredType, ruleItem: RuleItem, childIndex: Int): AssignmentStatement? {
        //val et: StructuredType = cor.resolvedType.declaration as StructuredType
        return when (ruleItem) {
            is EmptyRule -> null
            is Terminal -> null //createUniquePropertyDeclaration(et, UNNAMED_STRING_VALUE, propType)
            is Embedded -> createPropertyDeclarationForEmbedded(et, ruleItem, childIndex)

            is NonTerminal -> {
                val refRule = ruleItem.referencedRuleOrNull(this.grammar)
                createPropertyDeclarationAndAssignmentForReferencedRule(refRule, et, ruleItem, childIndex)
            }

            is Concatenation -> TODO("Concatenation")
            is Choice -> {
                val tr = trRuleForRuleItem(ruleItem, true)
                when (tr.resolvedType.declaration) {
                    SimpleTypeModelStdLib.NothingType.declaration -> null
                    else -> {
                        val n = propertyNameFor(et, ruleItem, tr.resolvedType.declaration)
                        val rhs = WithExpressionSimple(
                            withContext = EXPRESSION_CHILD(childIndex),
                            expression = tr.expression
                        )
                        createUniquePropertyDeclarationAndAssignment(et, n, tr.resolvedType, childIndex, rhs)
                    }
                }
            }

            is OptionalItem -> {
                val t = trRuleForRuleItem(ruleItem, true)
                when {
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> null
                    else -> {
                        val pName = propertyNameFor(et, ruleItem.item, t.resolvedType.declaration)
                        val exp = when (ruleItem.item) {
                            // is EmptyRule, is Terminal, is NonTerminal -> t.expression
                            else -> WithExpressionSimple(withContext = EXPRESSION_CHILD(childIndex), expression = t.expression)
                        }
                        createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, exp)
                    }
                }
            }

            is SimpleList -> {
                val t = trRuleForRuleItem(ruleItem, true)
                when {
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> null
                    else -> {
                        val pName = propertyNameFor(et, ruleItem.item, t.resolvedType.declaration)
                        val rhs = when {
                            else -> EXPRESSION_CHILD(childIndex)
                        }
                        //createUniquePropertyDeclarationAndAssignment(et, cor, pName, t.resolvedType, childIndex, EXPRESSION_CHILDREN)
                        createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, rhs)
                    }
                }
            }

            is SeparatedList -> {
                val t = trRuleForRuleItem(ruleItem, true)
                when {
                    t.resolvedType.declaration == SimpleTypeModelStdLib.NothingType.declaration -> null
                    else -> {
                        val pName = propertyNameFor(et, ruleItem.item, t.resolvedType.declaration)
                        createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, EXPRESSION_CHILDREN)
                    }
                }
            }

            is Group -> {
                val gt = trRuleForRuleItemGroup(ruleItem, true)
                when (gt.resolvedType.declaration) {
                    SimpleTypeModelStdLib.NothingType.declaration -> null
                    else -> {
                        val content = ruleItem.groupedContent
                        val pName = when (content) {
                            is Choice -> propertyNameFor(et, content, gt.resolvedType.declaration)
                            else -> propertyNameFor(et, ruleItem, gt.resolvedType.declaration)
                        }
                        val rhs = WithExpressionSimple(
                            withContext = EXPRESSION_CHILD(childIndex),
                            expression = gt.expression
                        )
                        createUniquePropertyDeclarationAndAssignment(et, pName, gt.resolvedType, childIndex, rhs)
                    }
                }
            }

            else -> error("Internal error, unhandled subtype of ConcatenationItem")
        }
    }

    private fun createPropertyDeclarationAndAssignmentForReferencedRule(
        refRule: GrammarRule?,
        et: StructuredType,
        ruleItem: SimpleItem,
        childIndex: Int
    ): AssignmentStatement? {
        val rhs = refRule?.rhs
        return when (rhs) {
            is Terminal -> {
                val t = SimpleTypeModelStdLib.String
                val pName = propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, t, childIndex, EXPRESSION_CHILD(childIndex))
            }

            is Concatenation -> {
                val t = trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, t.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
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
                    null
                } else {
                    val propType = trRuleForRuleItem(rhs, true) //to get list type
                    val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                    val colItem = when (rhs) {
                        is SimpleList -> rhs.item
                        is SeparatedList -> rhs.item
                        else -> error("Internal Error: not handled ${rhs::class.simpleName}")
                    }
                    val colPName = propertyNameFor(et, colItem, propType.resolvedType.declaration)
                    createUniquePropertyDeclarationAndAssignment(et, pName, propType.resolvedType, childIndex, EXPRESSION_CHILD_i_prop(childIndex, colPName))
                }
            }

            is Choice -> {
                val choiceType = trRuleForRhsChoice(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, choiceType.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
            }

            else -> {
                val propType = trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, propType.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
            }
        }
    }

    //TODO: combine with above by passing in TypeModel
    private fun createPropertyDeclarationForEmbedded(et: StructuredType, ruleItem: Embedded, childIndex: Int): AssignmentStatement? {
        val g2ns = builderForEmbedded(ruleItem) //TODO: configuration
        val g2rs = g2ns.g2rs!!
        val refRule = ruleItem.referencedRule(ruleItem.embeddedGrammarReference.resolved!!) //TODO: check for null
        val rhs = refRule.rhs
        return when (rhs) {
            is Terminal -> {
                val pName = propertyNameFor(et, ruleItem, SimpleTypeModelStdLib.String.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, SimpleTypeModelStdLib.String, childIndex, EXPRESSION_CHILD(childIndex))
            }

            is Concatenation -> {
                val t = g2rs.trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, t.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, t.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
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
                    null
                } else {
                    val propType = g2rs.trRuleForRuleItem(rhs, true) //to get list type
                    val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                    createUniquePropertyDeclarationAndAssignment(et, pName, propType.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
                }
            }

            is Choice -> {
                val choiceType = g2rs.trRuleForRhsChoice(rhs, refRule) //pName, rhs.alternative)
                val pName = propertyNameFor(et, ruleItem, choiceType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, choiceType.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
            }

            else -> {
                val propType = g2rs.trRuleForRuleItem(ruleItem, true)
                val pName = propertyNameFor(et, ruleItem, propType.resolvedType.declaration)
                createUniquePropertyDeclarationAndAssignment(et, pName, propType.resolvedType, childIndex, EXPRESSION_CHILD(childIndex))
            }
        }
    }

    private fun propertyNameFor(et: StructuredType, ruleItem: RuleItem, ruleItemType: TypeDeclaration): PropertyName {
        return when (configuration) {
            null -> when (ruleItem) {
                is EmptyRule -> error("should not happen")
                is Terminal -> when (ruleItemType) {
                    is PrimitiveType -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                    is CollectionType -> UNNAMED_LIST_PROPERTY_NAME
                    is TupleType -> UNNAMED_TUPLE_PROPERTY_NAME
                    else -> UNNAMED_PRIMITIVE_PROPERTY_NAME
                }

                is Embedded -> PropertyName(ruleItem.embeddedGoalName.value.lower())
                //is Embedded -> "${ruleItem.embeddedGrammarReference.resolved!!.name}_${ruleItem.embeddedGoalName}"
                is NonTerminal -> PropertyName(ruleItem.ruleReference.value)
                is Group -> UNNAMED_GROUP_PROPERTY_NAME
                else -> error("Internal error, unhandled subtype of SimpleItem")
            }

            else -> configuration.propertyNameFor(this.grammar, ruleItem, ruleItemType)
        }
    }

    private fun createUniquePropertyDeclarationAndAssignment(
        et: StructuredType,
//        trRule: TransformationRuleAbstract,
        name: PropertyName,
        type: TypeInstance,
        childIndex: Int,
        rhsExpression: Expression
    ): AssignmentStatement {
        val uniqueName = createUniquePropertyNameFor(et, name)
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        val pd = et.appendPropertyStored(uniqueName, type, characteristics, childIndex)
        //(trRule as TransformationRuleAbstract).appendAssignment(lhsPropertyName = uniqueName, rhs = rhsExpression)
        return AssignmentStatementSimple(lhsPropertyName = uniqueName, rhs = rhsExpression)
    }

    private fun createUniquePropertyNameFor(et: StructuredType, name: PropertyName): PropertyName {
        val key = Pair(et, name)
        val nameCount = this._uniquePropertyNames[key]
        val uniqueName = if (null == nameCount) {
            this._uniquePropertyNames[key] = 2
            name
        } else {
            this._uniquePropertyNames[key] = nameCount + 1
            PropertyName("${name.value}$nameCount")
        }
        return uniqueName
    }
}