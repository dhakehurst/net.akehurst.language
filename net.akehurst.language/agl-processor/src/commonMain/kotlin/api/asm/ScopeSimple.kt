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

    private val _children = mutableMapOf<String,ScopeSimple<E>>()

    // typeName -> referableName -> item
    private val _items: MutableMap<String,MutableMap<String,E>> = mutableMapOf()

    fun childScope(scopeFor:String): ScopeSimple<E> {
        var child = this._children[scopeFor]
        if (null==child) {
            child = ScopeSimple<E>(this, forTypeName)
            this._children[scopeFor] = child
        }
        return child
    }

    fun addToScope(referableName:String, typeName:String, item:E) {
        var m = this._items[typeName]
        if (null==m) {
            m = mutableMapOf()
            this._items[typeName] = m
        }
        m[referableName]=item
    }

    fun findOrNull(referableName:String, typeName:String):E? = this._items[typeName]?.get(referableName)

    fun isMissing(referableName:String, typeName:String):Boolean = null==this.findOrNull(referableName,typeName)
}