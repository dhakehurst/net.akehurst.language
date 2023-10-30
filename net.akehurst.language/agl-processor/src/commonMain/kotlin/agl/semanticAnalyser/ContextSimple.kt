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

import net.akehurst.language.agl.language.scopes.ScopeModelAgl
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementProperty
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.semanticAnalyser.*
import net.akehurst.language.typemodel.api.*

class ContextSimple() : SentenceContext<AsmElementPath> {

    /**
     * The items in the scope contain a ScopePath to an element in an AsmSimple model
     */
    override var rootScope = ScopeSimple<AsmElementPath>(null, "", ScopeModelAgl.ROOT_SCOPE_TYPE_NAME)

    fun asString(): String = "contextSimple scope §root ${rootScope.asString()}"

    override fun hashCode(): Int = rootScope.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is ContextSimple -> false
        this.rootScope != other.rootScope -> false
        else -> true
    }

    override fun toString(): String = "ContextSimple"
}

fun Expression.evaluateFor(self: Any?) = when (this) {
    is RootExpression -> this.evaluateFor(self)
    is Navigation -> this.evaluateFor(self)
    else -> error("Subtype of Expression not handled in 'evaluateFor'")
}

fun RootExpression.evaluateFor(self: Any?): String? = when {
    this.isNothing -> null
    this.isSelf -> when (self) {
        null -> null
        is String -> self
        else -> error("evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
    }

    else -> error("evaluateFor RootExpression not handled")
}

fun Navigation.evaluateFor(self: Any?) = when (self) {
    null -> error("Cannot navigate from 'null'")
    is AsmElementSimple -> {
        this.value.fold(self as Any?) { acc, it ->
            when (acc) {
                null -> null
                is AsmElementSimple -> acc.getPropertyOrNull(it)
                else -> error("Cannot evaluate $this on object of type '${acc::class.simpleName}'")
            }
        }
    }

    else -> error("evaluateFor Navigation from object of type '${self::class.simpleName}' not handled")
}

fun Navigation.propertyFor(root: AsmElementSimple?): AsmElementProperty {
    return when {
        null == root -> error("Cannot navigate '$this' from null value")
        else -> {
            val front = this.value.dropLast(1)
            var el = root
            for (pn in front) {
                val pv = el?.getProperty(pn)
                el = when (pv) {
                    null -> error("Cannot navigate '$pn' from null value")
                    is AsmElementSimple -> pv
                    else -> error("Cannot navigate '$pn' from value of type '${pv::class.simpleName}'")
                }
            }
            el?.properties?.get(this.value.last()) ?: error("Cannot navigate '$this' from null value")
        }
    }
}

fun Navigation.propertyDeclarationFor(root: TypeDeclaration?): PropertyDeclaration? {
    var type = root
    var pd: PropertyDeclaration? = null
    for (pn in this.value) {
        pd = when (type) {
            null -> null
            is DataType -> type.allProperty[pn]
            is TupleType -> type.property[pn]
            is CollectionType -> type.property[pn]
            is UnnamedSupertypeType -> type.property[pn]
            else -> error("subtype of TypeDefinition not handled: '${type::class.simpleName}'")
        }
        type = pd?.typeInstance?.type
    }
    return pd
}

fun Expression.createReferenceLocalToScope(scope: Scope<AsmElementPath>, element: AsmElementSimple) = when (this) {
    is RootExpression -> this.createReferenceLocalToScope(scope, element)
    is Navigation -> this.createReferenceLocalToScope(scope, element)
    else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
}

fun RootExpression.createReferenceLocalToScope(scope: Scope<AsmElementPath>, element: AsmElementSimple) =
    this.evaluateFor(element)

fun Navigation.createReferenceLocalToScope(scope: Scope<AsmElementPath>, element: AsmElementSimple): String? {
    val res = this.evaluateFor(element)
    return when (res) {
        null -> null
        is String -> res
        else -> error("Evaluation of navigation '$this' on '$element' should result in a String, but it does not!")
    }
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


//fun ScopeModelAgl.createReferenceFromRoot(scope: ScopeSimple<AsmElementPath>, element: AsmElementSimple): AsmElementPath {
//    return element.asmPath
//}

//fun ScopeModelAgl.resolveReference(asm:AsmSimple, rootScope: ScopeSimple<AsmElementPath>, reference: AsmElementPath): AsmElementSimple? {
//    return asm.index[reference]
//}