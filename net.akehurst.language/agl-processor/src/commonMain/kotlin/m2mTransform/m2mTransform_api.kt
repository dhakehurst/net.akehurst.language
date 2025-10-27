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

import net.akehurst.language.base.api.*
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import kotlin.jvm.JvmInline

interface M2mTransformDomain : Domain<M2mTransformNamespace, M2MTransformDefinition> {
    override val namespace: List<M2mTransformNamespace>
    val allTransformRuleSet: List<M2mTransformRuleSet>
    val allTransformTest: List<M2mTransformTest>

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): M2mTransformNamespace

}

interface M2mTransformNamespace : Namespace<M2MTransformDefinition> {
    fun createOwnedTransformRuleSet(name: SimpleName, extends: List<M2MTransformDefinition>, options: OptionHolder): M2mTransformRuleSet
}

/**
 * entries in an M2mTransformNamespace, either a RuleSet or a Test
 */
interface M2MTransformDefinition : Definition<M2MTransformDefinition>

interface M2mTransformRuleSetReference : DefinitionReference<M2MTransformDefinition> {
    override fun resolveAs(resolved: M2MTransformDefinition)
    fun cloneTo(ns: M2mTransformNamespace): M2mTransformRuleSetReference
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class DomainReference(val value: String)

interface M2mTransformRuleSet : M2MTransformDefinition {
    override val namespace: M2mTransformNamespace

    val domainParameters: Map<DomainReference, SimpleName>
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
}

interface M2mTransformRule {
    val isTop: Boolean
    val name: SimpleName
    val primitiveDomains: List<VariableDefinition>
    val domainSignature: Map<DomainReference, DomainSignature>
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
}

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

interface ObjectTemplate :  PropertyTemplateRhs {

    val type: TypeInstance
    val propertyTemplate: Map<SimpleName, PropertyTemplate>

    fun resolveType(tm: TypesDomain)
}

interface CollectionTemplate : PropertyTemplateRhs {
    val isSubset: Boolean
    val elements: List<PropertyTemplateRhs>
}

interface PropertyTemplate {
    val propertyName: SimpleName
    val rhs: PropertyTemplateRhs
}

interface PropertyTemplateRhs {
    val identifier: SimpleName?
    fun setIdentifierValue(value: SimpleName)
}

interface PropertyTemplateExpression :  PropertyTemplateRhs {
    val expression: Expression
}

interface M2mTransformTest : M2MTransformDefinition {
    val domainParameters: Map<DomainReference, SimpleName>
    val testCase : Map<SimpleName, M2mTransformTestCase>
}

interface M2mTransformTestCase {
    val name: SimpleName
    val domain: Map<DomainReference, Expression>
}