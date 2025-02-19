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

package net.akehurst.language.scope.asm

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.scope.api.ItemInScope
import net.akehurst.language.scope.api.Scope

class ScopeSimple<ItemInScopeType>(
    val parent: ScopeSimple<ItemInScopeType>?,
    val scopeIdentityInParent: String,
    override val forTypeName: QualifiedName
) : Scope<ItemInScopeType> {

    companion object {
        const val ROOT_ID = ""
    }

    override val scopeIdentity: String = "${parent?.scopeIdentity ?: ""}/$scopeIdentityInParent"

    override val scopePath: List<String> by lazy {
        if (null == parent) emptyList() else parent.scopePath + scopeIdentityInParent
    }

    //should only be used for rootScope
   // override val scopeMap = mutableMapOf<ItemType, ScopeSimple<ItemType>>()

    private val _childScopes = mutableMapOf<String, ScopeSimple<ItemInScopeType>>()

    // referableName -> (typeName, item)
    private val _items: MutableMap<String, MutableMap<QualifiedName, ItemInScopeType>> = mutableMapOf()

    override val rootScope: ScopeSimple<ItemInScopeType> by lazy {
        var s = this
        while (null != s.parent) s = s.parent!!
        s
    }

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    override val childScopes: Map<String, ScopeSimple<ItemInScopeType>> = _childScopes

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    /**
     * referableName -> typeName -> item
     */
    override val items: Map<String, Map<QualifiedName, ItemInScopeType>> get() = _items

    override val isEmpty: Boolean get() = items.isEmpty() && childScopes.isEmpty()

    override fun contains(referableName: String, typeName: QualifiedName, conformsToFunc: (itemTypeName: QualifiedName, requiredTypeName: QualifiedName) -> Boolean): Boolean =
        this.items[referableName]?.entries?.any { conformsToFunc.invoke(it.key, typeName) } ?: false

    override fun getChildScopeOrNull(childScopeIdentityInThis: String): Scope<ItemInScopeType>? {
        return this._childScopes[childScopeIdentityInThis]
    }

    override fun createOrGetChildScope(childScopeIdentityInThis: String, forTypeName: QualifiedName, itemInScope: ItemInScopeType): ScopeSimple<ItemInScopeType> {
        var child = this._childScopes[childScopeIdentityInThis]
        if (null == child) {
            child = ScopeSimple(this, childScopeIdentityInThis, forTypeName)
            this._childScopes[childScopeIdentityInThis] = child
        }
     //   this.rootScope.scopeMap[item] = child
        return child
    }

    override fun addToScope(referableName: String, qualifiedTypeName: QualifiedName, itemInScope: ItemInScopeType, replaceIfAlreadyExists:Boolean): Boolean {
        val map = this._items[referableName]
        return when (map) {
            null -> {
                val m = mutableMapOf(qualifiedTypeName to itemInScope)
                this._items[referableName] = m
                true
            }

            else -> when {
                replaceIfAlreadyExists.not() && map.containsKey(qualifiedTypeName) -> false
                else -> {
                    map[qualifiedTypeName] = itemInScope
                    true
                }
            }
        }
    }

    override fun findItemsNamed(name: String): Set<ItemInScope<ItemInScopeType>> =
        this.items[name]?.map { (typeName, item) -> ItemInScope(name, typeName, item) }?.toSet() ?: emptySet()

    override fun findItemsConformingTo(conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> =
        items.entries.flatMap { (referableName, map) ->
            map.entries.filter { (typeName, _) ->
                conformsToFunc.invoke(typeName)
            }.map { (typeName, item) -> ItemInScope(referableName, typeName, item) }
        }

    override fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> =
        items[name]?.filter { (typeName, _) ->
            conformsToFunc.invoke(typeName)
        }?.map { (typeName, item) -> ItemInScope(name, typeName, item) }
            ?: this.parent?.findItemsNamedConformingTo(name, conformsToFunc)
            ?: emptyList()

    override fun findItemsByQualifiedName(qualifiedName: List<String>): Set<ItemInScope<ItemInScopeType>> = when (qualifiedName.size) {
        0 -> emptySet()
        1 -> this.findItemsNamed(qualifiedName.first())
        else -> {
            val child = childScopes[qualifiedName.first()]
            child?.findItemsByQualifiedName(qualifiedName.drop(1)) ?: emptySet()
        }
    }

    override fun findItemsByQualifiedNameConformingTo(qualifiedName: List<String>, conformsToFunc: (itemTypeName: QualifiedName) -> Boolean): List<ItemInScope<ItemInScopeType>> =
        when (qualifiedName.size) {
            0 -> emptyList()
            1 -> this.findItemsNamedConformingTo(qualifiedName.first(), conformsToFunc)
            else -> {
                val child = childScopes[qualifiedName.first()]
                child?.findItemsByQualifiedNameConformingTo(qualifiedName.drop(1), conformsToFunc) ?: emptyList()
            }
        }

    override fun asString(currentIndent: String, indentIncrement: String): String {
        val scopeIndent = currentIndent + indentIncrement
        val content = items.entries.joinToString(separator = "\n") { me ->
            val itemName = me.key
            val itemTypeMap = me.value
            val itemTypeIndent = scopeIndent + indentIncrement
            val itemContent = itemTypeMap.entries.joinToString(separator = "\n") {
                val item = it.value
                val itemType = it.key
                val scope = when {
                    this.childScopes.containsKey(itemName) -> {
                        val chScope = this.childScopes[itemName]!!
                        " ${chScope.asString(itemTypeIndent, indentIncrement)}"
                    }

                    else -> ""
                }
                "$itemTypeIndent${itemName}: $itemType -> $item$scope"
            }

            "${scopeIndent}item $itemName {\n$itemContent\n$scopeIndent}"
        }
        return when {
            items.entries.isEmpty() -> "{ }"
            else -> "{\n$content\n$currentIndent}"
        }
    }

    override fun hashCode(): Int = arrayOf(parent, scopeIdentityInParent, scopeIdentity).contentHashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ScopeSimple<*> -> false
        parent != other.parent -> false
        scopeIdentityInParent != other.scopeIdentityInParent -> false
        scopeIdentity != other.scopeIdentity -> false
        else -> true
    }

    override fun toString(): String = when {
        null == parent -> "/$scopeIdentity"
        else -> "$parent/$scopeIdentity"
    }
}
