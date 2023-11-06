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

import net.akehurst.language.agl.language.reference.CrossReferenceModelDefault
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.api.language.reference.Scope
import net.akehurst.language.api.semanticAnalyser.SentenceContext

class ContextSimple() : SentenceContext<AsmPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    override var rootScope = ScopeSimple<AsmPath>(null, "", CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME)

    fun asString(): String = "contextSimple scope §root ${rootScope.asString()}"

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextSimple -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextSimple"
}

class ScopeSimple<AsmElementIdType>(
    val parent: ScopeSimple<AsmElementIdType>?,
    val forReferenceInParent: String,
    override val forTypeName: String
) : Scope<AsmElementIdType> {

    //should only be used for rootScope
    val scopeMap = mutableMapOf<AsmElementIdType, ScopeSimple<AsmElementIdType>>()

    private val _childScopes = mutableMapOf<String, ScopeSimple<AsmElementIdType>>()

    // typeName -> referableName -> item
    private val _items: MutableMap<String, MutableMap<String, AsmElementIdType>> = mutableMapOf()

    override val rootScope: ScopeSimple<AsmElementIdType> by lazy {
        var s = this
        while (null != s.parent) s = s.parent!!
        s
    }

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    override val childScopes: Map<String, ScopeSimple<AsmElementIdType>> = _childScopes

    // accessor needed for serialisation which assumes mutableMap for deserialisation
    override val items: Map<String, Map<String, AsmElementIdType>> get() = _items

    val path: List<String> by lazy {
        if (null == parent) emptyList() else parent.path + forReferenceInParent
    }

    override fun createOrGetChildScope(forReferenceInParent: String, forTypeName: String, elementId: AsmElementIdType): ScopeSimple<AsmElementIdType> {
        var child = this._childScopes[forReferenceInParent]
        if (null == child) {
            child = ScopeSimple<AsmElementIdType>(this, forReferenceInParent, forTypeName)
            this._childScopes[forReferenceInParent] = child
        }
        this.rootScope.scopeMap[elementId] = child
        return child
    }

    override fun addToScope(referableName: String, typeName: String, asmElementId: AsmElementIdType) {
        var m = this._items[typeName]
        if (null == m) {
            m = mutableMapOf()
            this._items[typeName] = m
        }
        m[referableName] = asmElementId
    }

    override fun findOrNull(referableName: String, typeName: String): AsmElementIdType? = this.items[typeName]?.get(referableName)

    override fun isMissing(referableName: String, typeName: String): Boolean = null == this.findOrNull(referableName, typeName)

    override fun asString(currentIndent: String, indentIncrement: String): String {
        val scopeIndent = currentIndent + indentIncrement
        val content = items.entries.joinToString(separator = "\n") {
            val itemTypeIndent = scopeIndent + indentIncrement
            val itemContent = it.value.entries.joinToString(separator = "\n") {
                val scope = when {
                    this.childScopes.containsKey(it.key) -> {
                        val chScope = this.childScopes[it.key]!!
                        " ${chScope.asString(itemTypeIndent, indentIncrement)}"
                    }

                    else -> ""
                }
                "$itemTypeIndent${it.key} -> ${it.value.toString()}$scope"
            }

            "${scopeIndent}item ${it.key} {\n$itemContent\n$scopeIndent}"
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
