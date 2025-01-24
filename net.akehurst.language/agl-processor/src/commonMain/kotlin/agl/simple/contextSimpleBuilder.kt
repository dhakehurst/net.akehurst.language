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

import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.scope.asm.ScopeSimple

fun contextAsmSimple(
    createScopedItem: CreateScopedItem<AsmStructure, Any> = {  ref, item -> item },
    resolveScopedItem: ResolveScopedItem< AsmStructure, Any> = {  ref -> ref as AsmStructure },
    init: ScopeBuilder<Any>.() -> Unit
): ContextAsmSimple {
    val context = ContextAsmSimple(createScopedItem, resolveScopedItem)
    val b = ScopeBuilder(context.rootScope, createScopedItem, resolveScopedItem)
    b.init()
    return context
}

@DslMarker
annotation class ContextSimpleDslMarker

@ContextSimpleDslMarker
class ScopeBuilder<ItemInScopeType : Any>(
    private val _scope: ScopeSimple<ItemInScopeType>,
    private val _createScopedItem: CreateScopedItem< AsmStructure, ItemInScopeType>,
    private val _resolveScopedItem: ResolveScopedItem< AsmStructure, ItemInScopeType>
) {

    fun item(id: String, qualifiedTypeName: String, itemInScope: ItemInScopeType) {
        _scope.addToScope(id, QualifiedName(qualifiedTypeName), itemInScope, false)
    }

    fun scope(forReferenceInParent: String, forTypeName: String, itemInScope: ItemInScopeType, init: ScopeBuilder<ItemInScopeType>.() -> Unit = {}) {
        // val path = AsmPathSimple(pathStr)
       // val itemInScope = _createScopedItem.invoke(forReferenceInParent, item)
        val chScope = _scope.createOrGetChildScope(forReferenceInParent, QualifiedName(forTypeName), itemInScope)
        val b = ScopeBuilder(chScope, _createScopedItem,_resolveScopedItem)
        b.init()
    }

    fun scopedItem(id: String, qualifiedTypeName: String, itemInScope: ItemInScopeType, init: ScopeBuilder<ItemInScopeType>.() -> Unit = {}) {
        //val itemInScope = _createScopedItem.invoke(id, item)
        _scope.addToScope(id, QualifiedName(qualifiedTypeName), itemInScope, false)
        val forTypeName = qualifiedTypeName.substringAfterLast(".")
        val chScope = _scope.createOrGetChildScope(id, QualifiedName(forTypeName), itemInScope)
        val b = ScopeBuilder(chScope, _createScopedItem,_resolveScopedItem)
        b.init()
    }

    fun build() {

    }

}