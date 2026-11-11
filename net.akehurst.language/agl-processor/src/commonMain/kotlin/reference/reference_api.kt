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

package net.akehurst.language.reference.api

import net.akehurst.language.base.api.*
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.types.api.TypesDomain


interface CrossReferenceDomain : Domain<CrossReferenceNamespace, DeclarationsForNamespace> {

    val declarationsForNamespace: Map<QualifiedName, DeclarationsForNamespace>

    fun isScopeDefinedFor(possiblyQualifiedTypeName: PossiblyQualifiedName): Boolean
    fun referencesFor(possiblyQualifiedTypeName: PossiblyQualifiedName): List<ReferenceExpression>
    fun referenceForProperty(typeQualifiedName: QualifiedName, propertyName: String): List<QualifiedName> //TODO: strProp: PropertyName
    fun identifyingExpressionFor(scopeForTypeName: SimpleName, possiblyQualifiedTypeName: PossiblyQualifiedName): Expression?

    /**
     * true if a scope is defined for the type, or for a type that it conforms to (supertype or union type).
     */
    fun isScopeDefinedFor(typesDomain: TypesDomain, typeQualifiedName: QualifiedName): Boolean

    /**
     * Find how an element of type 'typeQualifiedName' is identified in the scope created by an element of type 'scopeForTypeQualifiedName'.
     * Declared type names match by type conformance (supertypes and union types).
     *  - an 'identify' inside a 'scope' block wins over a top-level 'identify'
     *  - the nearest scope type wins, then the nearest identified type
     *  - top-level 'identify' applies in every scope (including the root scope)
     *  - more than one candidate in the result means the lookup is ambiguous
     *
     * @param scopeForTypeQualifiedName type of the element that created the scope, CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME for the root scope
     */
    fun identifyingExpressionFor(typesDomain: TypesDomain, scopeForTypeQualifiedName: QualifiedName, typeQualifiedName: QualifiedName): CrossReferenceMatch<Identifiable>

    /**
     * The reference expressions that apply to the type, from every 'in' block whose type the given type conforms to.
     * When several blocks give a reference for the same property (or collection), the nearest type wins.
     * One item per property (or collection), with more than one candidate if ambiguous.
     */
    fun referencesFor(typesDomain: TypesDomain, typeQualifiedName: QualifiedName): List<CrossReferenceMatch<ReferenceExpression>>
}

/**
 * Result of a lookup by type conformance.
 * Empty: nothing found. One candidate: found. More than one candidate: ambiguous.
 */
data class CrossReferenceMatch<T>(
    val candidates: List<T>
) {
    companion object {
        fun <T> none() = CrossReferenceMatch<T>(emptyList())
    }

    val isAmbiguous: Boolean get() = candidates.size > 1
    val singleOrNull: T? get() = candidates.singleOrNull()
}

interface CrossReferenceNamespace : Namespace<DeclarationsForNamespace> {
    val declarationsForNamespace: DeclarationsForNamespace
}

interface DeclarationsForNamespace : Definition<DeclarationsForNamespace> {

    val importedNamespaces: List<Import>

    /**
     * typeName -> ScopeDefinition
     */
    val scopeDefinition: Map<PossiblyQualifiedName, ScopeDefinition>
    val references: List<ReferenceDefinition>

    val isEmpty: Boolean

    fun isScopeDefinedFor(typeName: SimpleName): Boolean

    /**
     * Is the property inTypeName.propertyName a reference ?
     *
     * @param inTypeName name of the asm type in which contains the property
     * @param propertyName name of the property that might be a reference
     */
    //fun isReference(inTypeName: String, propertyName: String): Boolean

    /**
     * The list of reference-expressions defined for the given type
     */
    fun referencesFor(typeName: SimpleName): List<ReferenceExpression>

    fun referenceForPropertyOrNull(typeName: SimpleName, propertyName: String): ReferenceExpression?

    /**
     *
     * Find the expression that identifies the given type in the given scope
     *
     * @param scopeForTypeName name of the type whoes scope to look in
     * @param typeName name of the type to get an expression for
     */
    fun identifyingExpressionFor(scopeForTypeName: SimpleName, typeName: SimpleName): Expression?

}

interface ReferenceDefinition  : Formatable {
    val inTypeName: PossiblyQualifiedName
    val referenceExpressionList: List<ReferenceExpression>
}

interface ScopeDefinition : Formatable {
    val scopeForTypeName: PossiblyQualifiedName
    val identifiables: List<Identifiable>
}

interface Identifiable  : Formatable {
    val typeName: PossiblyQualifiedName
    val identifiedBy: Expression
}

interface ReferenceExpression  : Formatable {

}

interface ReferenceExpressionProperty : ReferenceExpression {
    /**
     * navigation to the property that is a reference
     */
    val referringPropertyNavigation: NavigationExpression

    /**
     * type of the asm element referred to
     */
    val refersToTypeName: List<PossiblyQualifiedName>

    val fromNavigation: NavigationExpression?
}

interface ReferenceExpressionCollection : ReferenceExpression {
    val expression: Expression
    val ofType: PossiblyQualifiedName?
    val referenceExpressionList: List<ReferenceExpression>
}

