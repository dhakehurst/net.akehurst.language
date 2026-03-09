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

package net.akehurst.language.m2mTransform.api

import net.akehurst.kotlinx.issues.api.Issue
import net.akehurst.language.base.api.*
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain



interface M2mTransformDomain : Domain<M2mTransformNamespace, M2mTransformRuleSet> {
    override val namespace: List<M2mTransformNamespace>
    val allTransformRuleSet: List<M2mTransformRuleSet>
    val allTransformTest: List<M2mTransformTest>

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): M2mTransformNamespace
    fun findTestDefinitionByQualifiedNameOrNull(qualifiedName: QualifiedName): M2mTransformTest?

}

interface M2mTransformNamespace : Namespace<M2mTransformRuleSet> {

    val testDefinition: List<M2mTransformTest>

    val testDefinitionByName: Map<SimpleName, M2mTransformTest>

    fun createOwnedTransformRuleSet(name: SimpleName, extends: List<M2MTransformDefinition>, options: OptionHolder): M2mTransformRuleSet

    fun findOwnedTestDefinitionOrNull(simpleName: SimpleName): M2mTransformTest?

    fun addTestDefinition(value: M2mTransformTest)

    override fun merge(value: Namespace<M2mTransformRuleSet>)
}

/**
 * entries in an M2mTransformNamespace, either a RuleSet or a Test
 */
interface M2MTransformDefinition : Definition<M2mTransformRuleSet>

interface M2mTransformRuleSetReference : DefinitionReference<M2mTransformRuleSet> {
    override fun resolveAs(resolved: M2mTransformRuleSet)
    fun cloneTo(ns: M2mTransformNamespace): M2mTransformRuleSetReference
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class DomainReference(val value: String)

interface M2mTransformRuleSet : M2MTransformDefinition {
    override val namespace: M2mTransformNamespace

    val domainParameters: Map<DomainReference, SimpleName>
    val domainParameterResolved: Map<DomainReference, TypesDomain>
    val extends: List<M2mTransformRuleSetReference>

    /**
     * Types in these namespaces can be referenced non-qualified
     * ordered so that 'first' imported name takes priority
     */
    val importTypes: List<Import>

    val topRule: List<M2mTransformRule>
    val rule: Map<SimpleName, M2mTransformRule>

    fun addImportType(value: Import)
    fun setRule(rule: M2mTransformRule)

    fun resolveDomainParameter(ref: DomainReference, typesDomain: TypesDomain)

    fun merge(value: M2mTransformRuleSet)
}

interface M2mTransformRuleReference {
    val nameOrQName: PossiblyQualifiedName
    var resolved: M2mTransformRule?

    fun resolveAs(resolved: M2mTransformRule)
}

interface M2mTransformRule {
    val isTop: Boolean
    val name: SimpleName
    val parameters: List<VariableDefinition>
    val extends: List<M2mTransformRuleReference>
    val domainSignature: Map<DomainReference, DomainSignature>

    fun conformsTo(other: M2mTransformRule): Boolean
}

interface DomainSignature {
    val domainRef: DomainReference
    val variable: VariableDefinition
}

interface VariableDefinition {
    val name: SimpleName
    val typeRef: TypeReference
    val type: TypeInstance

    fun resolveType(tm: TypesDomain)
}

interface M2mTransformAbstractRule : M2mTransformRule {

}

interface M2mTransformPatternRule : M2mTransformRule {
    val pivot: Map<SimpleName, VariableDefinition>
    val domainTemplate: Map<DomainReference, PropertyTemplateRhs>

    val when_: Expression?
    val where: List<RuleWhere>
}

interface RuleCall {
    val ruleName: SimpleName
    val ruleArguments: Map<SimpleName,Expression>
    val domainArguments: Map<DomainReference,Expression>

    var resolved: M2mTransformRule?

    fun resolveAs(resolved: M2mTransformRule)
}

interface RuleWhen : Expression, RuleCall
interface RuleWhenRelationHolds : RuleWhen
interface RuleWhenRelationHoldsForAll : RuleWhen
interface RuleWhenMappingHolds : RuleWhen
interface RuleWhenMappingHoldsForAll : RuleWhen

interface RuleWhere : RuleCall
interface RuleWhereCallRelation : RuleWhere
interface RuleWhereCallRelationForAll : RuleWhere
interface RuleWhereCallMapping : RuleWhere
interface RuleWhereCallMappingForAll : RuleWhere

interface M2MTransformRelation : M2mTransformPatternRule {

}

/**
 * should only have two domains
 */
interface M2MTransformMapping : M2mTransformPatternRule {
    /**
     * expression for constructing one domain from the other
     */
    val expression: Map<DomainReference, Expression?>
}

interface M2MTransformTable : M2mTransformRule {
    val values: List<Map<DomainReference, Expression>>
}

interface PropertyTemplateRhs {
    val identifier: SimpleName?
    fun setIdentifierValue(value: SimpleName)
    fun resolveTypes(tm: TypesDomain): List<LanguageIssue> //TODO: use generic Issues ! and move this to semanticAnalyser

    fun asString(indent: Indent = Indent()): String
}

interface ObjectTemplate : PropertyTemplateRhs {
    val type: TypeInstance
    val propertyTemplate: Map<SimpleName, PropertyTemplate>
}

interface CollectionTemplate : PropertyTemplateRhs {
    val isSubset: Boolean
    val elements: List<PropertyTemplateRhs>
}

interface PropertyTemplate {
    val propertyName: SimpleName
    val rhs: PropertyTemplateRhs
}



interface PropertyTemplateExpression : PropertyTemplateRhs {
    val expression: Expression
}

interface M2mTransformTest {
    val namespace: M2mTransformNamespace
    val name: SimpleName
    val qualifiedName: QualifiedName

    val options: OptionHolder

    val domainParameters: Map<DomainReference, SimpleName>
    val testCase: Map<SimpleName, M2mTransformTestCase>

    fun merge(value: M2mTransformTest)
}

interface M2mTransformTestCase {
    val name: SimpleName
    val domain: Map<DomainReference, Expression>
}