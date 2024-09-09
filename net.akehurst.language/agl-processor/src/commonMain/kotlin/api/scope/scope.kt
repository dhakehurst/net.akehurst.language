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

package net.akehurst.language.api.scope

import net.akehurst.language.agl.scope.ScopeSimple
import net.akehurst.language.api.language.base.QualifiedName

data class ScopedItem<ItemType>(
    val referableName: String,
    val qualifiedTypeName: QualifiedName,
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
    val forTypeName: QualifiedName

    val scopeIdentity: String

    /**
     * item.name -> item.type -> item
     */
    val items: Map<String, Map<QualifiedName, ItemType>>

    //TODO: don't want this here..see implementation
    val scopeMap:Map<ItemType, ScopeSimple<ItemType>>


    /**
     * childScopeIdentityInThis -> child Scope
     */
    val childScopes: Map<String, Scope<ItemType>>

    val rootScope: Scope<ItemType>

    val isEmpty: Boolean

    fun contains(referableName: String, typeName: QualifiedName, conformsToFunc: (typeName1: QualifiedName, typeName2: QualifiedName) -> Boolean): Boolean

    /**
     * find all items in this scope with the given <name>, return list of pairs (item,its-typeName)
     */
    fun findItemsNamed(name: String): Set<ScopedItem<ItemType>>

    /**
     * return List<Pair<referableName, item>>
     */
    fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ScopedItem<ItemType>>

    fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ScopedItem<ItemType>>

    /**
     * find all items with the given qualified name, return list of pairs (item,its-typeName)
     * if qualifiedName contains only one name, first try to find it
     */
    fun findItemsByQualifiedName(qualifiedName: List<String>): Set<ScopedItem<ItemType>>

    fun findItemsByQualifiedNameConformingTo(qualifiedName: List<String>, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ScopedItem<ItemType>>

    fun getChildScopeOrNull(childScopeIdentityInThis: String): Scope<ItemType>?

    fun createOrGetChildScope(childScopeIdentityInThis: String, forTypeName: QualifiedName, item: ItemType): Scope<ItemType>

    /**
     * adds Pair(item, typeName) to this scope
     * return true if added, false if the pair is already in the scope
     */
    fun addToScope(referableName: String, qualifiedTypeName: QualifiedName, item: ItemType): Boolean

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String
}