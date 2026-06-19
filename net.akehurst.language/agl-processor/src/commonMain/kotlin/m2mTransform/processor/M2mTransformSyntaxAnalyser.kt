/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.asm.*
import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class M2mTransformSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<M2mTransformDomain>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        AglExpressions.defaultTargetGrammar.qualifiedName to ExpressionsSyntaxAnalyser()
    )

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::transform)
        super.register(this::domainParams)
        super.register(this::parameterDefinition)
        super.register(this::extends)
        super.register(this::typeImport)
        super.register(this::transformRule)
        super.register(this::abstractRule)
        super.register(this::relationRule)
        super.register(this::mappingRule)
        super.register(this::tableRule)
        super.register(this::ruleParameters)
        super.register(this::pivot)
        super.register(this::domainPrimitive)
        super.register(this::domainSignature)
        super.register(this::unnamedDomainSignature)
        super.register(this::domainTemplate)
        super.register(this::domainTemplateRhs)
        super.register(this::domainPrimitiveRhs)
        super.register(this::domainObjectRhs)
        super.register(this::domainAssignment)
        super.register(this::variableDefinition)
        super.register(this::values)
        super.register(this::typeReference)
        super.register(this::expression)
        super.registerFor("when", this::when_)
        super.register(this::whenExpression)
        super.register(this::relationHolds)
        super.register(this::relationHoldsForAll)
        super.register(this::mappingHolds)
        super.register(this::mappingHoldsForAll)
        super.register(this::where)
        super.register(this::whereExpression)
        super.register(this::callRelation)
        super.register(this::callRelationForAll)
        super.register(this::callMapping)
        super.register(this::callMappingForAll)
        super.register(this::ruleCall)
        super.register(this::arguments)
        super.register(this::argAssignment)
        super.register(this::propertyTemplateRhs)
        super.register(this::objectTemplate)
        super.register(this::propertyTemplateBlock)
        super.register(this::propertyTemplate)
        super.register(this::collectionTemplate)

        super.register(this::testUnit)
        super.register(this::testNamespace)
        super.register(this::transformTest)
        super.register(this::testCase)
        super.register(this::testDomain)
    }

    // override unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<M2mTransformNamespace>
        val name = SimpleName("ParsedTransformUnit") //TODO: how to specify name, does it matter?
        val optHolder = OptionHolderDefault(null, options.toMap())
        return M2mTransformDomainDefault(
            name = name,
            optHolder,
            namespaces
        )
    }

    // override namespace = 'namespace' possiblyQualifiedName option* import* transform* ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val transformBuilders = children[4] as List<(M2mTransformNamespaceDefault) -> M2mTransformRuleSet>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val namespace = M2mTransformNamespaceDefault(nsName, optHolder, imports)
        transformBuilders.map { it.invoke(namespace) }
        return namespace
    }

    //  transform = 'transform' IDENTIFIER '(' domainParams ')' extends? '{' option* typeImport* transformRule* '} ;
    private fun transform(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (M2mTransformNamespace) -> M2mTransformRuleSet {
        val name = SimpleName(children[1] as String)
        val domParams = children[3] as List<Pair<String, SimpleName>>
        val extends = children[5] as List<PossiblyQualifiedName>? ?: emptyList()
        val options = children[7] as List<Pair<String, String>>
        val typeImports = children[8] as List<Import>
        val rules = children[9] as List<M2mTransformRule>

        val optHolder = OptionHolderDefault(null, options.toMap())
        return { namespace ->
            val extendRefs = extends.map { M2mTransformRuleSetReferenceDefault(namespace, it) }
            val asm = M2mTransformRuleSetDefault(namespace, name, domParams.associate { Pair(DomainReference(it.first), it.second) }, extendRefs, optHolder, rules)
            typeImports.forEach { asm.addImportType(it) }
            rules.forEach { asm.setRule(it) }
            asm.also { setLocationFor(it, nodeInfo, sentence) }
        }
    }

    // domainParams = [parameterDefinition / ',']2+ ;
    private fun domainParams(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<String, SimpleName>> =
        (children as List<Any>).toSeparatedList<Any, Pair<String, SimpleName>, String>().items

    // parameterDefinition = IDENTIFIER ':' DOMAIN_NAME ;
    private fun parameterDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, SimpleName> {
        val id = children[0] as String
        val dn = children[2] as String
        return Pair(id, SimpleName(dn))
    }

    //  leaf DOMAIN_NAME = IDENTIFIER ;
    // extends = ':' [possiblyQualifiedName / ',']+ ;
    private fun extends(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<M2mTransformRuleReference> =
        (children[1] as List<Any>).toSeparatedList<Any, PossiblyQualifiedName, String>().items.map {
            M2mTransformRuleReferenceDefault(it)
        }

    // option = 'option' IDENTIFIER '=' expression ;
    // import = 'import' qualifiedName ;
    //  typeImport = 'import-types' possiblyQualifiedName ;
    private fun typeImport(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Import =
        Import((children[1] as PossiblyQualifiedName).value)

    // transformRule = abstractRule | relationRule | mappingRule | tableRule ;
    private fun transformRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformRule =
        children[0] as M2mTransformRule

    // abstractRule = 'abstract' 'top'? 'rule' ruleName ruleParameters? extends? '{' domainSignature* '}' ;
    private fun abstractRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformAbstractRule {
        val isTop = children[1] == "top"
        val name = SimpleName(children[3] as String)
        val parameters = children[4] as? List<VariableDefinition> ?: emptyList()
        val extends = children[5] as? List<M2mTransformRuleReference> ?: emptyList()
        val sigDomains = children[7] as List<DomainSignature>
        return M2MTransformAbstractRuleDefault(isTop, name).also { rel ->
            (rel.parameters as MutableList).addAll(parameters)
            (rel.extends as MutableList).addAll(extends)
            sigDomains.forEach { rd ->
                (rel.domainSignature as MutableMap)[rd.domainRef] = rd
            }
        }
    }

    // relationRule = 'top'? 'relation' ruleName ruleParameters? extends? '{' pivot* domainTemplate{2+} when? where? '}' ;
    private fun relationRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2MTransformRelation {
        val isTop = children[0] == "top"
        val name = SimpleName(children[2] as String)
        val parameters = children[3] as? List<VariableDefinition> ?: emptyList()
        val extends = children[4] as? List<M2mTransformRuleReference> ?: emptyList()
        val pivots = children[6] as List<VariableDefinition>
        val relDomains = children[7] as List<Pair<DomainSignature, ObjectTemplate>>
        val whenExpression = children[8] as Expression?
        val whereExpression = children[9] as List<RuleWhere>?
        return M2MTransformRelationDefault(isTop, name).also { rel ->
            (rel.parameters as MutableList).addAll(parameters)
            (rel.extends as MutableList).addAll(extends)
            pivots.forEach { (rel.pivot as MutableMap)[it.name] = it }
            relDomains.forEach { rd ->
                (rel.domainSignature as MutableMap)[rd.first.domainRef] = rd.first
                (rel.domainTemplate as MutableMap)[rd.first.domainRef] = rd.second
            }
            rel.when_ = whenExpression
            whereExpression?.let { (rel.where as MutableList).addAll(whereExpression) }
        }
    }

    // mappingRule = 'top'? 'mapping' ruleName ruleParameters? extends? '{' domainTemplate+ domainAssignment when? where? '}' ;
    private fun mappingRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2MTransformMapping {
        val isTop = children[0] == "top"
        val name = SimpleName(children[2] as String)
        val parameters = children[3] as? List<VariableDefinition> ?: emptyList()
        val extends = children[4] as?List<M2mTransformRuleReference>? ?: emptyList()
        val inputDomains = children[6] as List<Pair<DomainSignature, ObjectTemplate>>
        val outputDomain = children[7] as Pair<DomainSignature, Expression?>
        val whenExpression = children[8] as Expression?
        val whereExpression = children[9] as List<RuleWhere>?
        return M2MTransformMappingDefault(isTop, name).also { mp ->
            (mp.parameters as MutableList).addAll(parameters)
            (mp.extends as MutableList).addAll(extends)
            inputDomains.forEach { rd ->
                (mp.domainSignature as MutableMap)[rd.first.domainRef] = rd.first
                (mp.domainTemplate as MutableMap)[rd.first.domainRef] = rd.second
            }
            (mp.domainSignature as MutableMap)[outputDomain.first.domainRef] = outputDomain.first
            (mp.expression as MutableMap)[outputDomain.first.domainRef] = outputDomain.second
            mp.when_ = whenExpression
            whereExpression?.let { (mp.where as MutableList).addAll(whereExpression) }
        }
    }

    // tableRule = 'top'? 'table' ruleName ruleParameters? extends? '{' unnamedDomainSignature{2+} values+ when? where? '}' ;
    private fun tableRule(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2MTransformTable {
        val isTop = children[0] == "top"
        val name = SimpleName(children[2] as String)
        val parameters = children[3] as? List<VariableDefinition> ?:emptyList()
        val extends = children[4] as List<M2mTransformRuleReference>? ?: emptyList()
        val sigDomains = children[6] as List<DomainSignature>
        val values = children[7] as List<List<Expression>>
        val whenExpression = children[8] as Expression?
        val whereExpression = children[9] as Expression?
        return M2MTransformTableDefault(isTop, name).also { rel ->
            (rel.parameters as MutableList).addAll(parameters)
            (rel.extends as MutableList).addAll(extends)
            sigDomains.forEach { rd ->
                (rel.domainSignature as MutableMap)[rd.domainRef] = rd
            }
            val valsMap = values.map { vs ->
                vs.mapIndexed { i, v -> Pair(sigDomains[i].domainRef, v) }.toMap()
            }
            (rel.values as MutableList).addAll(valsMap)
        }
    }

    // ruleParameters = '(' [variableDefinition / ',']+ ')' ;
    private fun ruleParameters(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<VariableDefinition> =
        children[1] as List<VariableDefinition>

    // pivot = 'pivot' variableDefinition ;
    private fun pivot(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val vd = children[1] as VariableDefinition
        return vd
    }

    // domainPrimitive = 'primitive 'domain' variableDefinition ;
    private fun domainPrimitive(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        return children[2] as VariableDefinition
    }

    // domainSignature = 'domain' domainReference variableDefinition ;
    private fun domainSignature(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): DomainSignature {
        val dr = children[1] as String
        val vd = children[2] as VariableDefinition
        return DomainSignatureDefault(DomainReference(dr), vd)
    }

    // unnamedDomainSignature = 'domain' domainReference ':' typeReference ;
    private fun unnamedDomainSignature(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): DomainSignature {
        val dr = children[1] as String
        val tr = children[3] as TypeReference
        val vd = VariableDefinitionDefault(SimpleName("<unnamed>"), tr)
        return DomainSignatureDefault(DomainReference(dr), vd)
    }

    // domainTemplate = domainSignature domainTemplateRhs ;
    private fun domainTemplate(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainSignature, PropertyTemplateRhs> {
        val ds = children[0] as DomainSignature
        val rhs = children[1]
        val template = when (rhs) {
            is PropertyTemplateExpression -> rhs
            is Map<*, *> -> ObjectTemplateDefault(ds.variable.typeRef, rhs as Map<SimpleName, PropertyTemplate>)
            else -> error("Invalid state")
        }.apply {
            setIdentifierValue(ds.variable.name)
        }


        return Pair(ds, template)
    }

    // domainTemplateRhs = domainPrimitiveRhs | domainObjectRhs ;
    private fun domainTemplateRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Any =
        children[0] as Any

    // domainPrimitiveRhs = '==' expression ;
    private fun domainPrimitiveRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyTemplateExpression =
        PropertyTemplateExpressionDefault(children[1] as Expression)

    // domainObjectRhs = propertyTemplateBlock ;
    private fun domainObjectRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Map<SimpleName, PropertyTemplate> =
        children[0] as Map<SimpleName, PropertyTemplate>

    // domainAssignment = domainSignature ':=' expression ;
    private fun domainAssignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainSignature, Expression?> {
        val ds = children[0] as DomainSignature
        val expr = children[2] as Expression
        return Pair(ds, expr)
    }

    // variableDefinition = variableName ':' typeReference ;
    private fun variableDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): VariableDefinition {
        val name = children[0] as String
        val typeRef = children[2] as TypeReference
        return VariableDefinitionDefault(SimpleName(name), typeRef)
    }

    // values = 'values' [expression / '<->']2+ ;
    private fun values(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Expression> {
        val exprSepList = (children[1] as List<Any>).toSeparatedList<Any, Expression, String>()
        return exprSepList.items
    }

    // expression = Expressions::expression ;
    private fun expression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // typeReference = Expressions::typeReference ;
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): TypeReference =
        children[0] as TypeReference

    // when = 'when' '{' whenExpression '}' ;
    private fun when_(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[2] as Expression

    // whenExpression = expression | relationHolds | relationHoldsForAll | mappingHolds | mappingHoldsForAll ;
    private fun whenExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // relationHolds = 'related' ruleCall ;
    private fun relationHolds(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhenRelationHolds {
        val (ruleName, ruleArguments, domainArguments) = children[1] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhenRelationHoldsDefault(ruleName, ruleArguments, domainArguments)
    }

    // relationHoldsForAll = 'related' 'all' ruleCall ;
    private fun relationHoldsForAll(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhenRelationHoldsForAll {
        val (ruleName, ruleArguments, domainArguments) = children[2] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhenRelationHoldsForAllDefault(ruleName, ruleArguments, domainArguments)
    }

    // mappingHolds = 'mapped' ruleCall ;
    private fun mappingHolds(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhenMappingHolds {
        val (ruleName, ruleArguments, domainArguments) = children[1] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhenMappingHoldsDefault(ruleName, ruleArguments, domainArguments)
    }

    // mappingHoldsForAll = 'mapped' 'all' ruleCall ;
    private fun mappingHoldsForAll(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhenMappingHoldsForAll {
        val (ruleName, ruleArguments, domainArguments) = children[2] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhenMappingHoldsForAllDefault(ruleName, ruleArguments, domainArguments)
    }

    // where = 'where' '{' whereExpression+ '}' ;
    private fun where(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<RuleWhere> =
        children[2] as List<RuleWhere>

    // whereExpression = callRelation | callRelationForAll | callMapping | callMappingForAll ;
    private fun whereExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhere =
        children[0] as RuleWhere

    // callRelation = 'relate' ruleCall ;
    private fun callRelation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhereCallRelation {
        val (ruleName, ruleArguments, domainArguments) = children[1] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhereCallRelationDefault(ruleName, ruleArguments, domainArguments)
    }

    // callRelationForAll = 'relate' 'all' ruleCall ;
    private fun callRelationForAll(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhereCallRelationForAll {
        val (ruleName, ruleArguments, domainArguments) = children[2] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhereCallRelationForAllDefault(ruleName, ruleArguments, domainArguments)
    }

    // callMapping = 'map' ruleCall ;
    private fun callMapping(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhereCallMapping {
        val (ruleName, ruleArguments, domainArguments) = children[1] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhereCallMappingDefault(ruleName, ruleArguments, domainArguments)
    }

    // callMappingForAll = 'map' 'all' ruleCall ;
    private fun callMappingForAll(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): RuleWhereCallMappingForAll {
        val (ruleName, ruleArguments, domainArguments) = children[2] as Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>>
        return RuleWhereCallMappingForAllDefault(ruleName, ruleArguments, domainArguments)
    }

    // ruleCall = ruleName arguments? '{' argAssignment{2+} '}' ;
    private fun ruleCall(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Triple<SimpleName, Map<SimpleName, Expression>, Map<DomainReference, Expression>> {
        val ruleName = SimpleName(children[0] as String)
        val ruleArguments = (children[1] as? List<Pair<String, Expression>>)?.associate{Pair(SimpleName(it.first), it.second)} ?: emptyMap()
        val domainArguments = (children[3] as List<Pair<String, Expression>>).associate{Pair(DomainReference(it.first), it.second)} ?: emptyMap()
        return Triple(ruleName, ruleArguments, domainArguments)
    }

    // arguments = '(' [argAssignment / ',']+ ')' ;
    private fun arguments(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<String, Expression>> {
        return (children[1] as List<Any>).toSeparatedList<Any, Pair<String, Expression>, String>().items
    }

    // argAssignment = variableName ':=' expression ;
    private fun argAssignment(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, Expression> {
        return Pair(children[0] as String, children[2] as Expression)
    }

    // propertyTemplateRhs =  objectTemplate | collectionTemplate | expression ;
    private fun propertyTemplateRhs(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyTemplateRhs =
        when (nodeInfo.alt.option) {
            OptionNum(0) -> children[0] as ObjectTemplate
            OptionNum(1) -> children[0] as CollectionTemplate
            OptionNum(2) -> PropertyTemplateExpressionDefault(children[0] as Expression)
            else -> error("Invalid state")
        }

    // objectTemplate = (variableName ':')? typeReference propertyTemplateBlock ;
    private fun objectTemplate(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ObjectTemplate {
        val variableName = (children[0] as? List<Any>)?.getOrNull(0) as? String
        val typeReference = children[1] as TypeReference
        val propertyTemplateBlock = children[2] as Map<SimpleName, PropertyTemplate>
        return ObjectTemplateDefault(typeReference, propertyTemplateBlock).apply {
            variableName?.let { setIdentifierValue(SimpleName(variableName)) }
        }
    }

    // propertyTemplateBlock = '{' propertyPattern*  '}';
    private fun propertyTemplateBlock(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Map<SimpleName, PropertyTemplate> {
        val propPats = children[1] as List<PropertyTemplate>
        return propPats.associateBy { it.propertyName }
    }

    // propertyTemplate = propertyName '==' propertyPatternRhs ;
    private fun propertyTemplate(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyTemplate {
        val id = children[0] as String
        val rhs = children[2] as PropertyTemplateRhs
        return PropertyTemplateDefault(SimpleName(id), rhs)
    }

    // collectionTemplate = (variableName ':')? '[' ('...')? [propertyPatternRhs / ',']* ']' ;
    private fun collectionTemplate(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CollectionTemplate {
        val variableName = (children[0] as? List<Any>)?.getOrNull(0) as? String
        val isSubset = children[2] != null
        val els = (children[3] as List<Any>).toSeparatedList<Any, PropertyTemplateRhs, String>()
        return CollectionTemplateDefault(isSubset, els.items).apply {
            variableName?.let { setIdentifierValue(SimpleName(variableName)) }
        }
    }

    // testUnit = option* import* testNamespace* ;
    private fun testUnit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformDomain {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[2] as List<M2mTransformNamespace>
        val name = SimpleName("ParsedTransformTestUnit") //TODO: how to specify name, does it matter?
        val optHolder = OptionHolderDefault(null, options.toMap())
        return M2mTransformDomainDefault(
            name = name,
            optHolder,
            namespaces
        )
    }

    // testNamespace = 'namespace' possiblyQualifiedName option* import* transformTest* ;
    private fun testNamespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val transformBuilders = children[4] as List<(M2mTransformNamespaceDefault) -> M2mTransformTest>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val namespace = M2mTransformNamespaceDefault(nsName, optHolder, imports)
        transformBuilders.map { it.invoke(namespace) }
        return namespace
    }

    // transformTest = 'transform-test' IDENTIFIER '(' domainParams ')' '{'
    //    testCase*
    // '}' ;
    private fun transformTest(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (M2mTransformNamespaceDefault) -> M2mTransformTest {
        val id = children[1] as String
        val domParams = children[3] as List<Pair<String, SimpleName>>
        val testCases = children[6] as List<M2mTransformTestCase>
        return { namespace ->
            M2MTransformTestDefault(
                namespace = namespace,
                name = SimpleName(id),
                domainParameters = domParams.associate { Pair(DomainReference(it.first), it.second) }
            ).apply {
                testCases.forEach { tc ->
                    this.testCase[tc.name] = tc
                }
            }
        }
    }

    // testCase = 'test-case' IDENTIFIER '{'
    //  testDomain*
    // '}' ;
    private fun testCase(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformTestCase {
        val id = children[1] as String
        val testDomains = children[3] as List<Pair<String, Expression>>
        return M2mTransformTestCaseDefault(SimpleName(id)).apply {
            testDomains.forEach { (dr, exp) ->
                domain[DomainReference(dr)] = exp
            }
        }
    }

    // testDomain := 'domain' IDENTIFIER ':=' expression ;
    private fun testDomain(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<String, Expression> {
        val id = children[1] as String
        val exp = children[3] as Expression
        return Pair(id, exp)
    }
}