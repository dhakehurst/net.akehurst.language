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

package net.akehurst.language.api.syntaxAnalyser

class AsmSimple {
    private var _nextElementId = 0

    val rootElements: List<AsmElementSimple> = mutableListOf()

    fun addRoot(root: AsmElementSimple) {
        (rootElements as MutableList).add(root)
    }

    fun createNonRootElement(typeName: String) = AsmElementSimple(_nextElementId++, this, typeName)

    fun createRootElement(typeName: String) = createNonRootElement(typeName).also { this.addRoot(it) }

    fun asString(indent: String, currentIndent: String = ""): String = this.rootElements.joinToString(separator = "\n") {
        it.asString(indent, currentIndent)
    }

}

class AsmElementSimple(
    val id: Int,
    val asm: AsmSimple,
    val typeName: String
) {
    private var _properties = mutableMapOf<String, AsmElementProperty>()

    val properties: List<AsmElementProperty> get() = _properties.values.toList()

    fun hasProperty(name: String): Boolean = _properties.containsKey(name)

    fun getProperty(name: String): Any? = _properties[name]

    fun getPropertyValue(name: String): Any? = _properties[name]?.value

    fun getPropertyAsAsmElement(name: String): AsmElementSimple = getPropertyValue(name) as AsmElementSimple

    fun getPropertyAsList(name: String): List<Any> = getPropertyValue(name) as List<Any>

    fun setProperty(name: String, value: Any?, isReference: Boolean) {
        _properties[name] = AsmElementProperty(this, name, value, isReference)
    }

    fun addAllProperty(value: List<AsmElementProperty>) {
        value.forEach { this._properties[it.name] = it }
    }

    private fun Any.asString(indent: String, currentIndent: String = ""): String = when (this) {
        is String -> "'$this'"
        is List<*> -> when (this.size) {
            0 -> "[]"
            1 -> "[${this[0]?.asString(indent, currentIndent)}]"
            else -> {
                val newIndent = currentIndent + indent
                this.joinToString(separator = "\n$newIndent", prefix = "[\n$newIndent", postfix = "\n$currentIndent]") { it?.asString(indent, newIndent) ?: "null" }
            }
        }
        is AsmElementSimple -> this.asString(indent, currentIndent)
        else -> error("property value type not handled '${this::class}'")
    }

    fun asString(indent: String, currentIndent: String = ""): String {
        val newIndent = currentIndent + indent
        val propsStr = this.properties.joinToString(separator = "\n$newIndent", prefix = "{\n$newIndent", postfix = "\n$currentIndent}") {
            if (null == it.value) {
                "${it.name} = null"
            } else {
                "${it.name} = ${it.value.asString(indent, newIndent)}"
            }
        }
        return ":$typeName $propsStr"
    }

    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean = when (other) {
        is AsmElementSimple -> this.id == other.id && this.asm == other.asm
        else -> false
    }

    override fun toString(): String = ":$typeName($id)"

}

class AsmElementProperty(
    val owner: AsmElementSimple,
    val name: String,
    val value: Any?,
    val isReference: Boolean
) {

    override fun toString(): String {
        return when (value) {
            is AsmElementSimple -> "$name = :${value.typeName}"
            is List<*> -> "$name = [...]"
            else -> "$name = ${value}"
        }
    }
}

val AsmElementSimple.children: List<AsmElementSimple>
    get()
    = this.properties
        .flatMap { if (it.value is List<*>) it.value else listOf(it.value) }
        .filterIsInstance<AsmElementSimple>()