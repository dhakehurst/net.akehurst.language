/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.analyser

import kotlin.js.JsName

class AsmElementProperty(
        val name: String,
        val value: Any?
)

class AsmElementSimple(
        val typeName: String
) {
    private var _properties = mutableMapOf<String, AsmElementProperty>()

    val properties: List<AsmElementProperty> get() = _properties.values.toList()

    @JsName("hasProperty")
    fun hasProperty(name: String): Boolean = _properties.containsKey(name)

    @JsName("getPropertyValue")
    fun getPropertyValue(name: String): Any? = _properties[name]?.value

    @JsName("getPropertyAstItem")
    fun getPropertyAstItem(name: String): AsmElementSimple = getPropertyValue(name) as AsmElementSimple

    @JsName("setProperty")
    fun setProperty(name: String, value: Any?) {
        _properties[name] = AsmElementProperty(name, value)
    }
    @JsName("addAllProperty")
    fun addAllProperty(value: List<AsmElementProperty>) {
        value.forEach { this._properties[it.name] = it }
    }

    override fun toString(): String {
        return ":$typeName"
    }
}