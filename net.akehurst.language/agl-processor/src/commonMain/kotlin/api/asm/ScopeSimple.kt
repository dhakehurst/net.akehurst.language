/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.api.asm

class ScopeSimple<E>(
    val parent: ScopeSimple<E>?,
    val forTypeName:String
) {

    private val _children = mutableListOf<ScopeSimple<E>>()
    val children:List<ScopeSimple<E>> = this._children

    fun childScope(forTypeName:String): ScopeSimple<E> {
        val child = ScopeSimple<E>(this,forTypeName)
        this._children.add(child)
        return child
    }

    val items: MutableMap<String,E> = mutableMapOf()

    fun addToScope(name:String, item:E) {
        this.items[name]=item
    }

    fun findOrNull(name:String):E? = this.items[name]
}