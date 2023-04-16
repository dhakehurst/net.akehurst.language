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

import net.akehurst.language.api.asm.AsmSimple.Companion.asStringAny
import net.akehurst.language.api.typemodel.PropertyDeclaration
import net.akehurst.language.api.typemodel.TypeModel

data class AsmElementPath(val value: String) {
    companion object {
        val ROOT = AsmElementPath("/")
    }

    operator fun plus(segment: String) = if (this == ROOT) AsmElementPath("/$segment") else AsmElementPath("$value/$segment")
}

open class AsmSimple() {

    companion object {
        internal fun Any.asStringAny(indent: String, currentIndent: String = ""): String = when (this) {
            is String -> "'$this'"
            is List<*> -> when (this.size) {
                0 -> "[]"
                1 -> "[${this[0]?.asStringAny(indent, currentIndent)}]"
                else -> {
                    val newIndent = currentIndent + indent
                    this.joinToString(separator = "\n$newIndent", prefix = "[\n$newIndent", postfix = "\n$currentIndent]") { it?.asStringAny(indent, newIndent) ?: "null" }
                }
            }

            is AsmElementSimple -> this.asString(indent, currentIndent)
            else -> error("property value type not handled '${this::class}'")
        }
    }

    private var _nextElementId = 0

    val rootElements: List<Any> = mutableListOf()
    val index = mutableMapOf<AsmElementPath, AsmElementSimple>()

    fun addRoot(root: Any) {
        (rootElements as MutableList).add(root)
    }

    fun createElement(asmPath: AsmElementPath, typeName: String) = AsmElementSimple(asmPath, this, typeName)

    fun asString(indent: String, currentIndent: String = ""): String = this.rootElements.joinToString(separator = "\n") {
       it.asStringAny(indent, currentIndent)
    }

}

class AsmElementSimple(
    val asmPath: AsmElementPath,
    val asm: AsmSimple,
    val typeName: String
) {
    private var _properties = mutableMapOf<String, AsmElementProperty>()

    val properties: Map<String, AsmElementProperty> = _properties

    init {
        this.asm.index[asmPath] = this
    }

    fun hasProperty(name: String): Boolean = properties.containsKey(name)

    fun getPropertyOrNull(name: String): Any? = properties[name]?.value
    fun getPropertyAsStringOrNull(name: String): String? = getPropertyOrNull(name) as String?
    fun getPropertyAsAsmElementOrNull(name: String): AsmElementSimple? = getPropertyOrNull(name) as AsmElementSimple?
    fun getPropertyAsReferenceOrNull(name: String): AsmElementReference? = getPropertyOrNull(name) as AsmElementReference?
    fun getPropertyAsListOrNull(name: String): List<Any>? = getPropertyOrNull(name) as List<Any>?

    fun getProperty(name: String): Any = properties[name]?.value ?: error("Cannot find property '$name' in element type '$typeName' with path '$asmPath' ")
    fun getPropertyAsString(name: String): String = getProperty(name) as String
    fun getPropertyAsAsmElement(name: String): AsmElementSimple = getProperty(name) as AsmElementSimple
    fun getPropertyAsReference(name: String): AsmElementReference = getProperty(name) as AsmElementReference
    fun getPropertyAsList(name: String): List<Any> = getProperty(name) as List<Any>
    fun getPropertyAsListOfElement(name: String): List<AsmElementSimple> = getProperty(name) as List<AsmElementSimple>
/*
    fun setPropertyAsString(name: String, value: String?) = setProperty(name, value, false)
    fun setPropertyAsListOfString(name: String, value: List<String>?) = setProperty(name, value, false)
    fun setPropertyAsAsmElement(name: String, value: AsmElementSimple?, isReference: Boolean) = setProperty(name, value, isReference)
*/
    fun setProperty(name: String, value: Any?, isReference: Boolean, childIndex: Int) {
        if (isReference) {
            val ref = AsmElementReference(value as String, null)
            _properties[name] = AsmElementProperty(name, childIndex, ref, true)
        } else {
            _properties[name] = AsmElementProperty(name, childIndex, value, false)
        }
    }

    fun setPropertyFromDeclaration(declaration: PropertyDeclaration, value: Any?, isReference: Boolean) {
        if (isReference) {
            val ref = AsmElementReference(value as String, null)
            _properties[declaration.name] = AsmElementProperty(declaration.name, declaration.childIndex, ref, true)
        } else {
            _properties[declaration.name] = AsmElementProperty(declaration.name, declaration.childIndex, value, false)
        }
    }

    fun addAllProperty(value: List<AsmElementProperty>) {
        value.forEach { this._properties[it.name] = it }
    }

    fun asString(indent: String, currentIndent: String = ""): String {
        val newIndent = currentIndent + indent
        val propsStr = this.properties.values.joinToString(separator = "\n$newIndent", prefix = "{\n$newIndent", postfix = "\n$currentIndent}") {
            if (it.isReference) {
                val ref = it.value as AsmElementReference
                if (null == ref.value) {
                    "${it.name} = <unresolved> &${ref.reference}"
                } else {
                    "${it.name} = &${ref.reference} : ${ref.value?.typeName}"
                }
            } else if (null == it.value) {
                "${it.name} = null"
            } else {
                "${it.name} = ${it.value.asStringAny(indent, newIndent)}"
            }
        }
        return ":$typeName $propsStr"
    }

    override fun hashCode(): Int = asmPath.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        is AsmElementSimple -> this.asmPath == other.asmPath && this.asm == other.asm
        else -> false
    }

    override fun toString(): String = ":$typeName($asmPath)"

}

class AsmElementReference(
    val reference: String,
    var value: AsmElementSimple?
) {
    override fun toString(): String {
        val resolved = if (null == value) "<unresolved> " else ""
        return "$resolved&$reference"
    }
}

class AsmElementProperty(
    val name: String,
    val childIndex: Int,
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
    = this.properties.values
        .flatMap { if (it.value is List<*>) it.value else listOf(it.value) }
        .filterIsInstance<AsmElementSimple>()


fun AsmElementSimple.equalTo(other: AsmElementSimple): Boolean {
    return when {
        this.asmPath != other.asmPath -> false
        this.typeName != other.typeName -> false
        this.properties.size != other.properties.size -> false
        else -> {
            this.properties.all { (k, v) ->
                val o = other.properties[k]
                if (null == o) {
                    false
                } else {
                    v.equalTo(o)
                }
            }
        }
    }
}

fun AsmElementProperty.equalTo(other: AsmElementProperty): Boolean {
    return when {
        this.name != other.name -> false
        this.isReference != other.isReference -> false
        else -> {
            val t = this.value
            val o = other.value
            if (this.isReference) {
                if (t is AsmElementReference && o is AsmElementReference) {
                    t.equalTo(o)
                } else {
                    error("Cannot compare property values: ${t} and ${o}")
                }
            } else {
                if (t is AsmElementSimple && o is AsmElementSimple) {
                    t.equalTo(o)
                } else {
                    t == o
                }
            }
        }
    }
}

fun AsmElementReference.equalTo(other: AsmElementReference): Boolean {
    return when {
        this.reference != other.reference -> false
        else -> true
    }
}

abstract class AsmSimpleTreeWalker {
    abstract fun root(root: AsmElementSimple)
    abstract fun beforeElement(element: AsmElementSimple)
    abstract fun afterElement(element: AsmElementSimple)
    abstract fun property(property: AsmElementProperty)
}

fun AsmSimple.traverseDepthFirst(callback: AsmSimpleTreeWalker) {
    fun traverse(element: Any?) {
        when (element) {
            is AsmElementSimple -> {
                callback.beforeElement(element)
                val props = element.properties.values.sortedWith { a, b ->
                    val aIdx = a.childIndex
                    val bIdx = b.childIndex
                    when {
                        aIdx > bIdx -> 1
                        aIdx < bIdx -> -1
                        else -> 0
                    }
                }
                for (prop in props) {
                    callback.property(prop)
                    val pv = prop.value
                    traverse(pv)
                }
                callback.afterElement(element)
            }

            is List<*> -> element.forEach { lv -> traverse(lv) }
        }
    }
    this.rootElements.forEach { traverse(it) }
}