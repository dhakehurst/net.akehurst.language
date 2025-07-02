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

package net.akehurst.language.agl.simple

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.scope.asm.ScopeSimple

//fun contextAsmSimple(
//    createScopedItem: CreateScopedItem<Any, Any> = { referableName, item, location -> Pair(location?.sentenceIdentity, item) },
//    resolveScopedItem: ResolveScopedItem<Any, Any> = { itemInScope -> (itemInScope as Pair<*, *>).second as AsmStructure },
//    sentenceId:Any? = null,
//    init: ScopeBuilder<Any>.() -> Unit = {}
//): ContextWithScope<Any,Any> {
//    val context = ContextWithScope(createScopedItem, resolveScopedItem)
//    val scope = context.newScopeForSentence(sentenceId) as ScopeSimple
//    val b = ScopeBuilder(scope, context.createScopedItem, context.resolveScopedItem)
//    b.init()
//    return context
//}

//fun contextAsmSimpleWithAsmPath(
//    map: MutableMap<String, Any> = mutableMapOf(),
//    sentenceId:Any? = null,
//    init: ScopeBuilder<Any>.() -> Unit = {}
//): ContextWithScope<Any,Any> {
//    val context = ContextWithScope<Any,Any>(
//        { referableName, item, location -> val path = "/${referableName.joinToString("/")}"; map[path] = item; path },
//        { itemInScope -> map[itemInScope] }
//    )
//    val scope = context.newScopeForSentence(sentenceId) as ScopeSimple
//    val b = ScopeBuilder(scope, context.createScopedItem, context.resolveScopedItem)
//    b.init()
//    return context
//}

fun contextAsmSimple(
//    createScopedItem: CreateScopedItem<Any, Any> = { referableName, item, location -> Pair(location?.sentenceIdentity, item) },
//    resolveScopedItem: ResolveScopedItem<Any, Any> = { itemInScope -> (itemInScope as Pair<*, *>).second as AsmStructure },
    createScopedItem: CreateScopedItem<Any, Any> = CreateScopedItemDefault(),
    resolveScopedItem: ResolveScopedItem<Any, Any> = ResolveScopedItemDefault(),
    init: ContextBuilder.() -> Unit = {}
): ContextWithScope<Any,Any> {
    val b = ContextBuilder(createScopedItem,resolveScopedItem)
    b.init()
    return b.build()
}

fun contextAsmSimpleWithAsmPath(
    map: MutableMap<String, Any> = mutableMapOf(),
    init: ContextBuilder.() -> Unit = {}
): ContextWithScope<Any,Any> {
    val createScopedItem:CreateScopedItem<Any,Any> = CreateScopedItem { referableName, item, location -> val path = "/${referableName.joinToString("/")}"; map[path] = item; path }
    val resolveScopedItem: ResolveScopedItem<Any,Any> =  ResolveScopedItem { itemInScope -> map[itemInScope] }
    val b = ContextBuilder(createScopedItem, resolveScopedItem)
    b.init()
    return b.build()
}

@DslMarker
annotation class ContextSimpleDslMarker

@ContextSimpleDslMarker
class ContextBuilder(
    createScopedItem: CreateScopedItem<Any, Any>,
    resolveScopedItem: ResolveScopedItem<Any, Any>,
) {

    private val _context = ContextWithScope(createScopedItem, resolveScopedItem)

    fun forSentence(id:Any?,init: ScopeBuilder<Any>.() -> Unit = {}) {
        val scope = _context.newScopeForSentence(id) as ScopeSimple
        val b = ScopeBuilder(scope, _context.createScopedItem, _context.resolveScopedItem)
        b.init()
    }

    fun build(): ContextWithScope<Any, Any>  = _context
}

@ContextSimpleDslMarker
class ScopeBuilder<ItemInScopeType : Any>(
    private val _scope: ScopeSimple<ItemInScopeType>,
    private val _createScopedItem: CreateScopedItem<Any, ItemInScopeType>,
    private val _resolveScopedItem: ResolveScopedItem<Any, ItemInScopeType>
) {

    fun item(id: String, qualifiedTypeName: String, location: Any?, itemInScope: ItemInScopeType) {
        _scope.addToScope(id, QualifiedName(qualifiedTypeName), location, itemInScope, false)
    }

    fun scope(forReferenceInParent: String, forTypeName: String, init: ScopeBuilder<ItemInScopeType>.() -> Unit = {}) {
        // val path = AsmPathSimple(pathStr)
        // val itemInScope = _createScopedItem.invoke(forReferenceInParent, item)
        val chScope = _scope.createOrGetChildScope(forReferenceInParent, QualifiedName(forTypeName))
        val b = ScopeBuilder(chScope, _createScopedItem, _resolveScopedItem)
        b.init()
    }

    fun scopedItem(id: String, qualifiedTypeName: String, location: Any?, itemInScope: ItemInScopeType, init: ScopeBuilder<ItemInScopeType>.() -> Unit = {}) {
        //val itemInScope = _createScopedItem.invoke(id, item)
        _scope.addToScope(id, QualifiedName(qualifiedTypeName), location, itemInScope, false)
        val forTypeName = qualifiedTypeName.substringAfterLast(".")
        val chScope = _scope.createOrGetChildScope(id, QualifiedName(forTypeName))
        val b = ScopeBuilder(chScope, _createScopedItem, _resolveScopedItem)
        b.init()
    }

    fun build() {

    }

}