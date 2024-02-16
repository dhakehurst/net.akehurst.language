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

    val isEmpty: Boolean

    fun isScopeDefinedFor(possiblyQualifiedTypeName: String): Boolean
    fun referencesFor(possiblyQualifiedTypeName: String): List<ReferenceExpression>
    fun referenceForProperty(typeQualifiedName: String, propertyName: String): List<String>
}

interface DeclarationsForNamespace {

    val qualifiedName: String
    val importedNamespaces: List<String>

    /**
     * typeName -> ScopeDefinition
     */
    val scopeDefinition: Map<String, ScopeDefinition>
    val references: List<ReferenceDefinition>

    val isEmpty: Boolean

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

    fun referenceForPropertyOrNull(typeName: String, propertyName: String): ReferenceExpression?

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
    val inTypeName: String
    val referenceExpressionList: List<ReferenceExpression>
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

data class ScopedItem<ItemType>(
    val referableName: String,
    val qualifiedTypeName: String,
    val item: ItemType
)

/**
 * ItemType - type of elements in the scope
 * TypeNames are specifically passed as Strings so that A Scope is easily serialised
 */
interface Scope<ItemType> {

    /**
     * unqualified TypeName from the ScopeDefinition,
     * i.e., the identity of the ScopeDefinition
     */
    val forTypeName: String

    val scopeIdentity: String

    /**
     * item.name -> item.type -> item
     */
    val items: Map<String, Map<String, ItemType>>

    /**
     * childScopeIdentityInThis -> child Scope
     */
    val childScopes: Map<String, Scope<ItemType>>

    val rootScope: Scope<ItemType>

    val isEmpty: Boolean

    fun contains(referableName: String, typeName: String, conformsToFunc: (typeName1: String, typeName2: String) -> Boolean): Boolean

    /**
     * find all items in this scope with the given <name>, return list of pairs (item,its-typeName)
     */
    fun findItemsNamed(name: String): Set<ScopedItem<ItemType>>

    /**
     * return List<Pair<referableName, item>>
     */
    fun findItemsConformingTo(conformsToFunc: (itemTypeName: String) -> Boolean): List<ScopedItem<ItemType>>

    fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: String) -> Boolean): List<ScopedItem<ItemType>>

    /**
     * find all items with the given qualified name, return list of pairs (item,its-typeName)
     * if qualifiedName contains only one name, first try to find it
     */
    fun findItemsByQualifiedName(qualifiedName: List<String>): Set<ScopedItem<ItemType>>

    fun findItemsByQualifiedNameConformingTo(qualifiedName: List<String>, conformsToFunc: (itemTypeName: String) -> Boolean): List<ScopedItem<ItemType>>

    fun getChildScopeOrNull(childScopeIdentityInThis: String): Scope<ItemType>?

    fun createOrGetChildScope(childScopeIdentityInThis: String, forTypeName: String, item: ItemType): Scope<ItemType>

    /**
     * adds Pair(item, typeName) to this scope
     * return true if added, false if the pair is already in the scope
     */
    fun addToScope(referableName: String, qualifiedTypeName: String, item: ItemType): Boolean

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String
}