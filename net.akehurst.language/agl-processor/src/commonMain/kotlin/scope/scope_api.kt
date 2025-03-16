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

package net.akehurst.language.scope.api

import net.akehurst.language.base.api.QualifiedName

data class ItemInScope<ItemInScopeType>(
    val referableName: String,
    val qualifiedTypeName: QualifiedName,
    val item: ItemInScopeType
)

/**
 * ItemType - type of elements in the scope
 * TypeNames are specifically passed as Strings so that A Scope is easily serialised
 */
interface Scope<ItemInScopeType> {

    /**
     * unqualified TypeName from the ScopeDefinition,
     * i.e., the identity of the ScopeDefinition
     */
    val forTypeName: QualifiedName

    val scopeIdentity: String
    val scopePath:List<String>

    /**
     * item.name -> item.type -> item
     */
    val items: Map<String, Map<QualifiedName, ItemInScopeType>>

    //TODO: don't want this here..see implementation
   // val scopeMap:Map<ItemType, Scope<ItemInScopeType>>


    /**
     * childScopeIdentityInThis -> child Scope
     */
    val childScopes: Map<String, Scope<ItemInScopeType>>

    val rootScope: Scope<ItemInScopeType>

    val isEmpty: Boolean

    fun contains(referableName: String, typeName: QualifiedName, conformsToFunc: (itemTypeName: QualifiedName, requiredTypeName: QualifiedName) -> Boolean): Boolean

    /**
     * find all items in this scope with the given <name>, return list of pairs (item,its-typeName)
     */
    fun findItemsNamed(name: String): Set<ItemInScope<ItemInScopeType>>

    /**
     * return List<Pair<referableName, item>>
     */
    fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>>

    fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>>

    /**
     * find all items with the given qualified name, return list of pairs (item,its-typeName)
     * if qualifiedName contains only one name, first try to find it
     */
    fun findItemsByQualifiedName(qualifiedName: List<String>): Set<ItemInScope<ItemInScopeType>>

    fun findItemsByQualifiedNameConformingTo(qualifiedName: List<String>, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>>

    fun getChildScopeOrNull(childScopeIdentityInThis: String): Scope<ItemInScopeType>?

    fun createOrGetChildScope(childScopeIdentityInThis: String, forTypeName: QualifiedName, item: ItemInScopeType): Scope<ItemInScopeType>

    /**
     * adds Pair(item, typeName) to this scope
     * return true if added, false if the pair is already in the scope
     */
    fun addToScope(referableName: String, qualifiedTypeName: QualifiedName, item: ItemInScopeType, replaceIfAlreadyExists:Boolean): Boolean

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String
}