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

package net.akehurst.language.agl.asm

import net.akehurst.language.api.asm.*
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

class AsmPathSimple(
    override val value: String
) : AsmPath {

    companion object {
        const val SEPARATOR = "/"
        val EXTERNAL = AsmPathSimple("§external")
        val ROOT = AsmPathSimple(SEPARATOR)
    }

    override val segments: List<String> get() = this.value.split(SEPARATOR)

    override val parent: AsmPath?
        get() = when {
            ROOT == this -> null
            else -> AsmPathSimple(this.value.substringBeforeLast("/"))
        }

    override val isExternal: Boolean get() = EXTERNAL.value == this.value

    override operator fun plus(segment: String) = if (this == ROOT) AsmPathSimple("/$segment") else AsmPathSimple("$value/$segment")

    override fun hashCode(): Int = this.value.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is AsmPath -> false
        this.value != other.value -> false
        else -> true
    }

    override fun toString(): String = this.value
}

open class AsmSimple() : Asm {

    /*    companion object {
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
                else -> error("property value type not handled '${this::class.simpleName}'")
            }
        }*/

    private var _nextElementId = 0

    override val root: List<AsmValue> = mutableListOf()
    override val elementIndex = mutableMapOf<AsmPath, AsmStructure>()

    fun addRoot(root: AsmValue) {
        (this.root as MutableList).add(root)
    }

    fun removeRoot(root: Any) {
        (this.root as MutableList).remove(root)
    }

    fun createStructure(asmPath: AsmPath, typeName: String): AsmStructureSimple {
        val el = AsmStructureSimple(asmPath, typeName)// this, typeName)
        this.elementIndex[asmPath] = el
        return el
    }

    override fun traverseDepthFirst(callback: AsmTreeWalker) {
        fun traverse(owningProperty: AsmStructureProperty?, value: AsmValue) {
            when (value) {
                is AsmNothing -> callback.onNothing(owningProperty, value)
                is AsmPrimitive -> callback.onPrimitive(owningProperty, value)
                is AsmStructure -> {
                    callback.beforeStructure(owningProperty, value)
                    for (prop in value.propertyOrdered) {
                        callback.onProperty(value, prop)
                        val pv = prop.value
                        traverse(prop, pv)
                    }
                    callback.afterStructure(owningProperty, value)
                }

                is AsmList -> {
                    callback.beforeList(owningProperty, value)
                    value.elements.forEach { el -> traverse(owningProperty, el) }
                    callback.afterList(owningProperty, value)
                }

                is AsmListSeparated -> {
                    callback.beforeList(owningProperty, value)
                    value.elements.forEach { el -> traverse(owningProperty, el) }
                    callback.afterList(owningProperty, value)
                }

                else -> Unit
            }
        }
        this.root.forEach {
            callback.beforeRoot(it)
            traverse(null, it)
            callback.afterRoot(it)
        }
    }

    override fun asString(currentIndent: String, indentIncrement: String): String = this.root.joinToString(separator = "\n") {
        it.asString(indentIncrement, currentIndent)
    }

}

abstract class AsmValueAbstract() : AsmValue {
    override val typeName: String get() = qualifiedTypeName.split(".").last()
}

object AsmNothingSimple : AsmValueAbstract(), AsmNothing {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.NothingType.qualifiedTypeName
    override fun asString(currentIndent: String, indentIncrement: String): String = "Nothing"
    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmNothing -> false
        else -> true
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when (other) {
        !is AsmNothing -> false
        else -> true
    }

    override fun toString(): String = "Nothing"
}

class AsmPrimitiveSimple(
    override val qualifiedTypeName: String,
    override val value: Any
) : AsmValueAbstract(), AsmPrimitive {

    companion object {
        fun stdString(value: String) = AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, value)
        fun stdInteger(value: Int) = AsmPrimitiveSimple(SimpleTypeModelStdLib.Integer.qualifiedTypeName, value)
    }

    override fun asString(currentIndent: String, indentIncrement: String): String = "$value"
    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmPrimitive -> false
        other.value != this.value -> false
        else -> true
    }

    override fun hashCode(): Int = listOf(qualifiedTypeName, value).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is AsmPrimitive -> false
        this.qualifiedTypeName != other.qualifiedTypeName -> false
        this.value != other.value -> false
        else -> true
    }

    override fun toString(): String = "$qualifiedTypeName($value)"
}

val AsmPrimitive.isStdString get() = this.qualifiedTypeName == SimpleTypeModelStdLib.String.qualifiedTypeName

class AsmReferenceSimple(
    override val reference: String,
    override var value: AsmStructure?
) : AsmValueAbstract(), AsmReference {

    override val qualifiedTypeName: String
        get() = when (value) {
            null -> SimpleTypeModelStdLib.NothingType.qualifiedTypeName
            else -> value!!.qualifiedTypeName
        }

//    fun equalTo(other: AsmReferenceSimple): Boolean {
//        return when {
//            this.reference != other.reference -> false
//            else -> true
//        }
//    }

    override fun asString(currentIndent: String, indentIncrement: String): String = when (value) {
        null -> "<unresolved> &$reference"
        else -> "&{'${value!!.path.value}' : ${value!!.typeName}}"
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmReference -> false
        other.reference != this.reference -> false
        else -> true
    }

    override fun hashCode(): Int = reference.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is AsmReference -> false
        other.reference != this.reference -> false
        else -> true
    }

    override fun toString(): String = when (value) {
        null -> "<unresolved> &$reference"
        else -> "&{'${value!!.path.value}' : ${value!!.typeName}}"
    }
}

class AsmStructureSimple(
    override val path: AsmPath,
    override val qualifiedTypeName: String
) : AsmValueAbstract(), AsmStructure {

    private var _properties = mutableMapOf<String, AsmStructurePropertySimple>()

    override val property: Map<String, AsmStructureProperty> = _properties
    override val propertyOrdered
        get() = property.values.sortedWith { a, b ->
            val aIdx = a.index
            val bIdx = b.index
            when {
                aIdx > bIdx -> 1
                aIdx < bIdx -> -1
                else -> 0
            }
        }

    /**
     * 'contained' elements's. i.e.
     * value of non reference, AsmElementSimple type, properties
     */
    override val children: List<AsmStructureSimple>
        get() = this.property.values
            .filterNot { it.isReference }
            .flatMap { if (it.value is List<*>) it.value as List<*> else listOf(it.value) }
            .filterIsInstance<AsmStructureSimple>()

    override fun hasProperty(name: String): Boolean = property.containsKey(name)

    override fun getPropertyOrNull(name: String): AsmValue? = property[name]?.value
    fun getPropertyAsStringOrNull(name: String): String? = getPropertyOrNull(name) as String?
    fun getPropertyAsAsmElementOrNull(name: String): AsmStructureSimple? = getPropertyOrNull(name) as AsmStructureSimple?
    fun getPropertyAsReferenceOrNull(name: String): AsmReferenceSimple? = getPropertyOrNull(name) as AsmReferenceSimple?
    fun getPropertyAsListOrNull(name: String): List<Any>? = getPropertyOrNull(name) as List<Any>?

    override fun getProperty(name: String): AsmValue = property[name]?.value ?: error("Cannot find property '$name' in element type '$typeName' with path '$path' ")
    fun getPropertyAsString(name: String): String = (getProperty(name) as AsmPrimitive).value as String
    fun getPropertyAsAsmElement(name: String): AsmStructureSimple = getProperty(name) as AsmStructureSimple
    fun getPropertyAsReference(name: String): AsmReferenceSimple = getProperty(name) as AsmReferenceSimple
    fun getPropertyAsList(name: String): List<Any> = getProperty(name) as List<Any>
    fun getPropertyAsListOfElement(name: String): List<AsmStructureSimple> = getProperty(name) as List<AsmStructureSimple>

    override fun setProperty(name: String, value: AsmValue, childIndex: Int) {
        _properties[name] = AsmStructurePropertySimple(name, childIndex, value)
    }

    fun addAllProperty(value: List<AsmStructurePropertySimple>) {
        value.forEach { this._properties[it.name] = it }
    }

    override fun asString(currentIndent: String, indentIncrement: String): String {
        val newIndent = currentIndent + indentIncrement
        val propsStr = this.property.values.joinToString(separator = "\n$newIndent", prefix = "{\n$newIndent", postfix = "\n$currentIndent}") {
            if (it.isReference) {
                val ref = it.value as AsmReferenceSimple
                "${it.name} = $ref"
            } else {
                "${it.name} = ${it.value.asString(indentIncrement, newIndent)}"
            }
        }
        return ":$typeName $propsStr"
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmStructure -> false
        this.path != other.path -> false
        this.qualifiedTypeName != other.qualifiedTypeName -> false
        this.property.size != other.property.size -> false
        else -> {
            this.property.all { (k, v) ->
                val o = other.property[k]
                if (null == o) {
                    false
                } else {
                    v.equalTo(o)
                }
            }
        }
    }

    override fun hashCode(): Int = path.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is AsmStructureSimple -> this.path == other.path //&& this.asm == other.asm
        else -> false
    }

    override fun toString(): String = ":$typeName[${path.value}]"

}

class AsmStructurePropertySimple(
    override val name: String,
    override val index: Int,
    value: AsmValue
) : AsmStructureProperty {

    override var value: AsmValue = value; private set

    override val isReference: Boolean get() = this.value is AsmReferenceSimple

    override fun convertToReferenceTo(referredValue: AsmStructure?) {
        val v = this.value
        when {
            v is AsmNothing -> error("Cannot convert property '$this' a reference, it has value $AsmNothingSimple")
            v is AsmReferenceSimple -> v.value = referredValue
            v is AsmPrimitive && v.value is String -> {
                val ref = AsmReferenceSimple(v.value as String, referredValue)
                this.value = ref
            }

            v is AsmList && v.elements.all { (it is AsmPrimitive) && it.isStdString } -> {
                val refValue = v.elements.joinToString(separator = ".") { (it as AsmPrimitive).value as String }
                val ref = AsmReferenceSimple(refValue, referredValue)
                this.value = ref
            }

            else -> error("Cannot convert property '$this' a reference, it has value of type '${v::class.simpleName}'")
        }
    }

    override fun equalTo(other: AsmStructureProperty): Boolean {
        return when {
            this.name != other.name -> false
            this.isReference != other.isReference -> false
            else -> {
                val t = this.value
                val o = other.value
                if (this.isReference) {
                    if (t is AsmReferenceSimple && o is AsmReferenceSimple) {
                        t.equalTo(o)
                    } else {
                        error("Cannot compare property values: ${t} and ${o}")
                    }
                } else {
                    if (t is AsmStructureSimple && o is AsmStructureSimple) {
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
            is AsmStructureSimple -> "$name = :${v.typeName}"
            is AsmList -> "$name = [...]"
            is AsmPrimitive -> if (isReference) "$name = &${v}" else "$name = ${v}"
            else -> "$name = ${v}"
        }
    }
}

class AsmListSimple(
    override val elements: List<AsmValue>
) : AsmValueAbstract(), AsmList {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.List.qualifiedName

    override fun asString(currentIndent: String, indentIncrement: String): String =
        this.elements.joinToString { it.asString(currentIndent, indentIncrement) }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmList -> false
        other.elements.size != this.elements.size -> false
        else -> {
            (0..this.elements.size).all {
                this.elements[it].equalTo(other.elements[it])
            }
        }
    }

    override fun hashCode(): Int = elements.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is AsmList -> false
        else -> this.elements == other.elements
    }

    override fun toString(): String = elements.toString()
}

class AsmListSeparatedSimple(
    override val elements: ListSeparated<AsmValue, AsmValue, AsmValue>
) : AsmValueAbstract(), AsmListSeparated {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.ListSeparated.qualifiedName

    override fun asString(currentIndent: String, indentIncrement: String): String =
        this.elements.elements.joinToString { (it).asString(currentIndent, indentIncrement) }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmListSeparated -> false
        other.elements.size != this.elements.size -> false
        else -> {
            (0..this.elements.size).all {
                (this.elements[it]).equalTo(other.elements[it])
            }
        }
    }

    override fun hashCode(): Int = elements.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is AsmListSeparated -> false
        else -> this.elements == other.elements
    }

    override fun toString(): String = elements.toString()
}