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

package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.api.language.reference.Scope
import net.akehurst.language.api.language.reference.ScopedItem
import net.akehurst.language.api.semanticAnalyser.SentenceContext

class ContextSimple() : SentenceContext<AsmPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    var rootScope = ScopeSimple<AsmPath>(null, ScopeSimple.ROOT_ID, CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

    val isEmpty: Boolean get() = rootScope.isEmpty

    fun asString(): String = "contextSimple scope §root ${rootScope.asString()}"

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextSimple -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextSimple"
}

class ScopeSimple<ItemType>(
    val parent: ScopeSimple<ItemType>?,
    val scopeIdentityInParent: String,
    override val forTypeName: String
) : Scope<ItemType> {

    companion object {
        const val ROOT_ID = ""
    }

    override val scopeIdentity: String = "${parent?.scopeIdentity ?: ""}/$scopeIdentityInParent"

    //should only be used for rootScope
    //TODO: I don't want to store the 'Items' in the scope!
    val scopeMap = mutableMapOf<ItemType, ScopeSimple<ItemType>>()

    private val _childScopes = mutableMapOf<String, ScopeSimple<ItemType>>()

    // referableName -> (typeName, item)
    private val _items: MutableMap<String, MutableMap<String, ItemType>> = mutableMapOf()

    override val rootScope: ScopeSimple<ItemType> by lazy {
        var s = this
        while (null != s.parent) s = s.parent!!
        s
    }

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    override val childScopes: Map<String, ScopeSimple<ItemType>> = _childScopes

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    /**
     * referableName -> typeName -> item
     */
    override val items: Map<String, Map<String, ItemType>> get() = _items

    val path: List<String> by lazy {
        if (null == parent) emptyList() else parent.path + scopeIdentityInParent
    }

    override val isEmpty: Boolean get() = items.isEmpty() && childScopes.isEmpty()

    override fun contains(referableName: String, typeName: String, conformsToFunc: (typeName1: String, typeName2: String) -> Boolean): Boolean =
        this.items[referableName]?.entries?.any { conformsToFunc.invoke(it.key, typeName) } ?: false

    override fun createOrGetChildScope(scopeIdentityInParent: String, forTypeName: String, item: ItemType): ScopeSimple<ItemType> {
        var child = this._childScopes[scopeIdentityInParent]
        if (null == child) {
            child = ScopeSimple(this, scopeIdentityInParent, forTypeName)
            this._childScopes[scopeIdentityInParent] = child
        }
        this.rootScope.scopeMap[item] = child
        return child
    }

    override fun addToScope(referableName: String, qualifiedTypeName: String, item: ItemType): Boolean {
        val map = this._items[referableName]
        return when (map) {
            null -> {
                val m = mutableMapOf(qualifiedTypeName to item)
                this._items[referableName] = m
                true
            }

            else -> when {
                map.containsKey(qualifiedTypeName) -> false
                else -> {
                    map[qualifiedTypeName] = item
                    true
                }
            }
        }
    }

    override fun findItemsNamed(name: String): Set<ScopedItem<ItemType>> =
        this.items[name]?.map { (typeName, item) -> ScopedItem(name, typeName, item) }?.toSet() ?: emptySet()

    override fun findItemsConformingTo(conformsToFunc: (itemTypeName: String) -> Boolean): List<ScopedItem<ItemType>> =
        items.entries.flatMap { (referableName, map) ->
            map.entries.filter { (typeName, _) ->
                conformsToFunc.invoke(typeName)
            }.map { (typeName, item) -> ScopedItem(referableName, typeName, item) }
        }

    override fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: String) -> Boolean): List<ScopedItem<ItemType>> =
        items[name]?.filter { (typeName, _) ->
            conformsToFunc.invoke(typeName)
        }?.map { (typeName, item) -> ScopedItem(name, typeName, item) } ?: emptyList()


    override fun findQualified(qualifiedName: List<String>): Set<ScopedItem<ItemType>> = when (qualifiedName.size) {
        0 -> emptySet()
        1 -> this.findItemsNamed(qualifiedName.first())
        else -> {
            val child = childScopes[qualifiedName.first()]
            child?.findQualified(qualifiedName.drop(1)) ?: emptySet()
        }
    }

    override fun findQualifiedConformingTo(qualifiedName: List<String>, conformsToFunc: (itemTypeName: String) -> Boolean): List<ScopedItem<ItemType>> = when (qualifiedName.size) {
        0 -> emptyList()
        1 -> this.findItemsNamedConformingTo(qualifiedName.first(), conformsToFunc)
        else -> {
            val child = childScopes[qualifiedName.first()]
            child?.findQualifiedConformingTo(qualifiedName.drop(1), conformsToFunc) ?: emptyList()
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
