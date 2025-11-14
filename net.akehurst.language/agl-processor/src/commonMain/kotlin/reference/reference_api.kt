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
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression


interface CrossReferenceDomain : Domain<CrossReferenceNamespace, DeclarationsForNamespace> {

    val declarationsForNamespace: Map<QualifiedName, DeclarationsForNamespace>

    fun isScopeDefinedFor(possiblyQualifiedTypeName: PossiblyQualifiedName): Boolean
    fun referencesFor(possiblyQualifiedTypeName: PossiblyQualifiedName): List<ReferenceExpression>
    fun referenceForProperty(typeQualifiedName: QualifiedName, propertyName: String): List<QualifiedName> //TODO: strProp: PropertyName
    fun identifyingExpressionFor(scopeForTypeName: SimpleName, possiblyQualifiedTypeName: PossiblyQualifiedName): Expression?
}

interface CrossReferenceNamespace : Namespace<DeclarationsForNamespace> {
    val declarationsForNamespace: DeclarationsForNamespace
}

interface DeclarationsForNamespace : Definition<DeclarationsForNamespace> {

    val importedNamespaces: List<Import>

    /**
     * typeName -> ScopeDefinition
     */
    val scopeDefinition: Map<SimpleName, ScopeDefinition>
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
    val inTypeName: SimpleName
    val referenceExpressionList: List<ReferenceExpression>
}

interface ScopeDefinition : Formatable {
    val scopeForTypeName: SimpleName
    val identifiables: List<Identifiable>
}

interface Identifiable  : Formatable {
    val typeName: SimpleName
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

