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
import net.akehurst.language.typemodel.api.PropertyDeclaration

data class AsmElementPath(val value: String) {
    companion object {
        val ROOT = AsmElementPath("/")
    }

    operator fun plus(segment: String) = if (this == ROOT) AsmElementPath("/$segment") else AsmElementPath("$value/$segment")

    val parent: AsmElementPath?
        get() = when {
            ROOT == this -> null
            else -> AsmElementPath(this.value.substringBeforeLast("/"))
        }
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

            is AsmElementSimple -> this.asString(currentIndent, indent)
            else -> error("property value type not handled '${this::class}'")
        }
    }

    private var _nextElementId = 0

    val rootElements: List<Any> = mutableListOf()
    val elementIndex = mutableMapOf<AsmElementPath, AsmElementSimple>()

    fun addRoot(root: Any) {
        (rootElements as MutableList).add(root)
    }

    fun removeRoot(root: Any) {
        (rootElements as MutableList).remove(root)
    }

    fun createElement(asmPath: AsmElementPath, typeName: String): AsmElementSimple {
        val el = AsmElementSimple(asmPath, typeName)// this, typeName)
        this.elementIndex[asmPath] = el
        return el
    }

    fun traverseDepthFirst(callback: AsmSimpleTreeWalker) {
        fun traverse(propertyName: String?, element: Any?) {
            when (element) {
                is AsmElementSimple -> {
                    callback.beforeElement(propertyName, element)
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
                        callback.property(element, prop)
                        val pv = prop.value
                        traverse(prop.name, pv)
                    }
                    callback.afterElement(propertyName, element)
                }

                is List<*> -> element.forEach { lv -> traverse(propertyName, lv) }
                else -> Unit
            }
        }
        this.rootElements.forEach {
            traverse(null, it)
        }
    }

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String = this.rootElements.joinToString(separator = "\n") {
        it.asStringAny(indentIncrement, currentIndent)
    }

}

class AsmElementSimple(
    val asmPath: AsmElementPath,
    val typeName: String
) {

    private var _properties = mutableMapOf<String, AsmElementProperty>()

    val properties: Map<String, AsmElementProperty> = _properties
    val propertiesOrdered get() = properties.values.sortedBy { it.childIndex }

    /**
     * 'contained' elements's. i.e.
     * value of non reference, AsmElementSimple type, properties
     */
    val children: List<AsmElementSimple>
        get() = this.properties.values
            .filterNot { it.isReference }
            .flatMap { if (it.value is List<*>) it.value as List<*> else listOf(it.value) }
            .filterIsInstance<AsmElementSimple>()

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

    fun setProperty(name: String, value: Any?, childIndex: Int) {
        _properties[name] = AsmElementProperty(name, childIndex, value)
    }

    fun setPropertyFromDeclaration(declaration: PropertyDeclaration, value: Any?) {
        setProperty(declaration.name, value, declaration.index)
    }

    fun addAllProperty(value: List<AsmElementProperty>) {
        value.forEach { this._properties[it.name] = it }
    }

    fun equalTo(other: AsmElementSimple): Boolean {
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

    fun asString(currentIndent: String = "", indentIncrement: String = "  "): String {
        val newIndent = currentIndent + indentIncrement
        val propsStr = this.properties.values.joinToString(separator = "\n$newIndent", prefix = "{\n$newIndent", postfix = "\n$currentIndent}") {
            if (it.isReference) {
                val ref = it.value as AsmElementReference
                //if (null == ref.value) {
                //    "${it.name} = <unresolved> &${ref.reference}"
                //} else {
                //    "${it.name} = &${ref.reference} : ${ref.value?.typeName}"
                //}
                "${it.name} = $ref"
            } else if (null == it.value) {
                "${it.name} = null"
            } else {
                "${it.name} = ${it.value!!.asStringAny(indentIncrement, newIndent)}"
            }
        }
        return ":$typeName $propsStr"
    }

    override fun hashCode(): Int = asmPath.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        is AsmElementSimple -> this.asmPath == other.asmPath //&& this.asm == other.asm
        else -> false
    }

    override fun toString(): String = ":$typeName([${asmPath.value}])"

}

class AsmElementReference(
    val reference: String,
    var value: AsmElementSimple?
) {
    fun equalTo(other: AsmElementReference): Boolean {
        return when {
            this.reference != other.reference -> false
            else -> true
        }
    }

    override fun toString(): String = when (value) {
        null -> "<unresolved> &$reference"
        else -> "&{'${value!!.asmPath.value}' : ${value!!.typeName}}"
    }
}

class AsmElementProperty(
    val name: String,
    val childIndex: Int,
    value: Any?
) {
    var value: Any? = value; private set
    val isReference: Boolean get() = this.value is AsmElementReference

    fun convertToReferenceTo(referredValue: AsmElementSimple?) {
        val v = this.value
        when (v) {
            null -> error("Cannot convert property '$this' a reference, it has value null")
            is AsmElementReference -> v.value = referredValue
            is String -> {
                val ref = AsmElementReference(v, referredValue)
                this.value = ref
            }

            else -> error("Cannot convert property '$this' a reference, it has value of type '${v::class.simpleName}'")
        }
    }

    fun equalTo(other: AsmElementProperty): Boolean {
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

    override fun toString(): String {
        val v = this.value
        return when (v) {
            is AsmElementSimple -> "$name = :${v.typeName}"
            is List<*> -> "$name = [...]"
            is String -> if (isReference) "$name = &${v}" else "$name = ${v}"
            else -> "$name = ${v}"
        }
    }
}

interface AsmSimpleTreeWalker {
    fun root(root: AsmElementSimple)
    fun beforeElement(propertyName: String?, element: AsmElementSimple)
    fun afterElement(propertyName: String?, element: AsmElementSimple)
    fun property(element: AsmElementSimple, property: AsmElementProperty)
}
