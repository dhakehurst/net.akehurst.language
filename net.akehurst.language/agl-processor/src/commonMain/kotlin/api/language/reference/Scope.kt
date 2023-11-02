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

package net.akehurst.language.api.language.reference

import net.akehurst.language.api.language.expressions.Expression

interface CrossReferenceModel {

    val declarationsForNamespace: Map<String, DeclarationsForNamespace>

    fun isScopeDefinedFor(possiblyQualifiedTypeName: String): Boolean
    fun referencesFor(possiblyQualifiedTypeName: String): List<ReferenceExpression>
}

interface DeclarationsForNamespace {

    val qualifiedName: String
    val scopes: Map<String, ScopeDefinition>
    val externalTypes: List<String>
    val references: List<ReferenceDefinition>

    fun isScopeDefinedFor(typeName: String): Boolean

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
    fun referencesFor(typeName: String): List<ReferenceExpression>

    /**
     *
     * Find the expression that identifies the given type in the given scope
     *
     * @param scopeForTypeName name of the type whoe scope to look in
     * @param typeName name of the type to get an expression for
     */
    fun identifyingExpressionFor(scopeForTypeName: String, typeName: String): Expression?

}

interface ReferenceDefinition {

}

interface ScopeDefinition {
    val scopeForTypeName: String
    val identifiables: List<Identifiable>
}

interface Identifiable {
    val typeName: String
    val identifiedBy: Expression
}

interface ReferenceExpression {

}

/**
 * E - type of elements in the scope
 */
interface Scope<AsmElementIdType> {

    val forTypeName: String

    val items: Map<String, Map<String, AsmElementIdType>>

    val childScopes: Map<String, Scope<AsmElementIdType>>

    val rootScope: Scope<AsmElementIdType>

    fun isMissing(referableName: String, typeName: String): Boolean

    fun findOrNull(referableName: String, typeName: String): AsmElementIdType?

    fun createOrGetChildScope(forReferenceInParent: String, forTypeName: String, elementId: AsmElementIdType): Scope<AsmElementIdType>

    fun addToScope(referableName: String, typeName: String, asmElementId: AsmElementIdType)

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String
}