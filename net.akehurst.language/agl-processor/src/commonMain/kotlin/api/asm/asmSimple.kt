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

package net.akehurst.language.api.asm

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
    fun getPropertyAsString(name: String): String? = _properties[name]?.value as String?
    fun getPropertyAsAsmElement(name: String): AsmElementSimple? = _properties[name]?.value as AsmElementSimple?
    fun getPropertyAsReference(name: String): AsmElementReference? = _properties[name]?.value as AsmElementReference?
    fun getPropertyAsList(name: String): List<Any> = _properties[name]?.value as List<Any>

    fun setPropertyAsString(name: String, value: String?) = setProperty(name, value, false)
    fun setPropertyAsListOfString(name: String, value: List<String>?) = setProperty(name, value, false)
    fun setPropertyAsAsmElement(name: String, value: AsmElementSimple?, isReference: Boolean) = setProperty(name, value, isReference)
    fun setProperty(name: String, value: Any?, isReference: Boolean) {
        if (isReference) {
            val ref = AsmElementReference(value as String, null)
            _properties[name] = AsmElementProperty(name, ref, true)
        } else {
            _properties[name] = AsmElementProperty(name, value, false)
        }
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
            if (it.isReference) {
                val ref = it.value as AsmElementReference
                if (null==ref.value) {
                    "${it.name} = <unresolved> &${ref.reference}"
                } else {
                    "${it.name} = &${ref.reference} : ${ref.value?.typeName}"
                }
            } else if (null == it.value) {
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

class AsmElementReference(
    val reference: String,
    var value: AsmElementSimple?
)

class AsmElementProperty(
    val name: String,
    val value: Any?,
    val isReference: Boolean
) {

    override fun toString(): String {
        return when (value) {
            is AsmElementSimple -> "$name = :${value.typeName}"
            is List<*> -> "$name = [...]"
            is String -> if (isReference) "$name = &${value}" else "$name = ${value}"
            else -> "$name = ${value}"
        }
    }
}

val AsmElementSimple.children: List<AsmElementSimple>
    get()
    = this.properties
        .flatMap { if (it.value is List<*>) it.value else listOf(it.value) }
        .filterIsInstance<AsmElementSimple>()