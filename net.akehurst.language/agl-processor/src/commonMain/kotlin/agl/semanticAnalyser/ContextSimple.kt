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
import net.akehurst.language.api.semanticAnalyser.SentenceContext

class ContextSimple() : SentenceContext<AsmPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    var rootScope = ScopeSimple<AsmPath>(null, "", CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

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
    val forReferenceInParent: String,
    override val forTypeName: String
) : Scope<ItemType> {

    //should only be used for rootScope
    val scopeMap = mutableMapOf<ItemType, ScopeSimple<ItemType>>()

    private val _childScopes = mutableMapOf<String, ScopeSimple<ItemType>>()

    // referableName -> (item, typeName)
    private val _items: MutableMap<String, MutableMap<String, ItemType>> = mutableMapOf()

    override val rootScope: ScopeSimple<ItemType> by lazy {
        var s = this
        while (null != s.parent) s = s.parent!!
        s
    }

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    override val childScopes: Map<String, ScopeSimple<ItemType>> = _childScopes

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    override val items: Map<String, Map<String, ItemType>> get() = _items

    val path: List<String> by lazy {
        if (null == parent) emptyList() else parent.path + forReferenceInParent
    }

    override fun contains(referableName: String, typeName: String, conformsToFunc: (typeName1: String, typeName2: String) -> Boolean): Boolean =
        this.items[referableName]?.entries?.any { conformsToFunc.invoke(it.key, typeName) } ?: false

    override fun createOrGetChildScope(forReferenceInParent: String, forTypeName: String, item: ItemType): ScopeSimple<ItemType> {
        var child = this._childScopes[forReferenceInParent]
        if (null == child) {
            child = ScopeSimple<ItemType>(this, forReferenceInParent, forTypeName)
            this._childScopes[forReferenceInParent] = child
        }
        this.rootScope.scopeMap[item] = child
        return child
    }

    override fun addToScope(referableName: String, typeName: String, item: ItemType): Boolean {
        val map = this._items[referableName]
        return when (map) {
            null -> {
                val m = mutableMapOf(typeName to item)
                this._items[referableName] = m
                true
            }

            else -> when {
                map.containsKey(typeName) -> false
                else -> {
                    map[typeName] = item
                    true
                }
            }
        }
    }

    override fun findItemsNamed(name: String): Set<Pair<ItemType, String>> =
        this.items[name]?.map { Pair(it.value, it.key) }?.toSet() ?: emptySet()

    override fun findItemsConformingTo(conformsToFunc: (itemTypeName: String) -> Boolean) =
        items.values.flatMap {
            it.filter {
                conformsToFunc.invoke(it.key)
            }.map { it.value }
        }

    override fun findItemsNamedConformingTo(name: String, conformsToFunc: (itemTypeName: String) -> Boolean): List<ItemType> =
        items[name]?.filter {
            conformsToFunc.invoke(it.key)
        }?.map { it.value } ?: emptyList()


    override fun findQualified(qualifiedName: List<String>): Set<Pair<ItemType, String>> = when (qualifiedName.size) {
        0 -> emptySet()
        1 -> this.findItemsNamed(qualifiedName.first())
        else -> {
            val child = childScopes[qualifiedName.first()]
            child?.findQualified(qualifiedName.drop(1)) ?: emptySet()
        }
    }

    override fun findQualifiedConformingTo(qualifiedName: List<String>, conformsToFunc: (itemTypeName: String) -> Boolean): List<ItemType> = when (qualifiedName.size) {
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

    override fun hashCode(): Int = arrayOf(parent, forReferenceInParent, forTypeName).contentHashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ScopeSimple<*> -> false
        parent != other.parent -> false
        forReferenceInParent != other.forReferenceInParent -> false
        forTypeName != other.forTypeName -> false
        else -> true
    }

    override fun toString(): String = when {
        null == parent -> "/$forTypeName"
        else -> "$parent/$forTypeName"
    }
}
