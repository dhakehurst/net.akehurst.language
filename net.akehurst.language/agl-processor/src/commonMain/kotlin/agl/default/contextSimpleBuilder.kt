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

package net.akehurst.language.agl.default_

import net.akehurst.language.agl.asm.AsmPathSimple
import net.akehurst.language.agl.scope.ScopeSimple
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.base.api.QualifiedName

fun contextAsmDefault(init: ScopeBuilder.() -> Unit): ContextAsmDefault {
    val context = ContextAsmDefault()
    val b = ScopeBuilder(context.rootScope)
    b.init()
    return context
}

@DslMarker
annotation class ContextSimpleDslMarker

@ContextSimpleDslMarker
class ScopeBuilder(
    private val _scope: ScopeSimple<AsmPath>
) {

    fun item(id: String, qualifiedTypeName: String, pathStr: String) {
        val path = AsmPathSimple(pathStr)
        _scope.addToScope(id, QualifiedName(qualifiedTypeName), path)
    }

    fun scope(forReferenceInParent: String, forTypeName: String, pathStr: String, init: ScopeBuilder.() -> Unit = {}) {
        val path = AsmPathSimple(pathStr)
        val chScope = _scope.createOrGetChildScope(forReferenceInParent, QualifiedName(forTypeName), path)
        val b = ScopeBuilder(chScope)
        b.init()
    }

    fun scopedItem(id: String, qualifiedTypeName: String, pathStr: String, init: ScopeBuilder.() -> Unit = {}) {
        val path = AsmPathSimple(pathStr)
        _scope.addToScope(id, QualifiedName(qualifiedTypeName), path)
        val forTypeName = qualifiedTypeName.substringAfterLast(".")
        val chScope = _scope.createOrGetChildScope(id, QualifiedName(forTypeName), path)
        val b = ScopeBuilder(chScope)
        b.init()
    }

    fun build() {

    }

}