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
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.jvm.JvmInline

interface M2mTransformDomain : Model<M2mTransformNamespace, M2mTransformRuleSet> {
    override val namespace: List<M2mTransformNamespace>

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): M2mTransformNamespace

}

interface M2mTransformNamespace : Namespace<M2mTransformRuleSet> {
    fun createOwnedTransformRuleSet(name: SimpleName, extends: List<M2mTransformRuleSetReference>, options: OptionHolder): M2mTransformRuleSet
}

interface M2mTransformRuleSetReference : DefinitionReference<M2mTransformRuleSet> {
    override fun resolveAs(resolved: M2mTransformRuleSet)
    fun cloneTo(ns: M2mTransformNamespace): M2mTransformRuleSetReference
}

@JvmInline
value class DomainReference(val value: String)

interface M2mTransformRuleSet : Definition<M2mTransformRuleSet> {
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
    val domainItem: Map<DomainReference, DomainItem>
}

interface DomainItem {
    val domainRef: DomainReference
    val variable: VariableDefinition
}

interface VariableDefinition {
    val name: SimpleName
    val type: TypeInstance

    fun resolveType(tm: TypeModel)
}

interface M2mRelation : M2mTransformRule {
    val pivot: Map<SimpleName, VariableDefinition>
}

/**
 * should only have two domains
 */
interface M2mMapping : M2mTransformRule {
    /**
     * expression for constructing one domain from the other
     */
    val expression: Map<DomainReference, Expression>
}

interface ObjectPattern : PropertyPatternRhs {
    val identifier: SimpleName?
    val type: TypeInstance?
    val propertyPattern: Map<SimpleName, PropertyPattern>

    fun setIdentifier(value: SimpleName)
}

interface PropertyPattern {
    val propertyName: SimpleName
    val rhs: PropertyPatternRhs
}

interface PropertyPatternRhs

interface PropertyPatternExpression : PropertyPatternRhs {
    val expression: Expression
}