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

package net.akehurst.language.asm.simple

import net.akehurst.language.asm.api.*
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.asm.StdLibDefault

val PropertyName.asValueName get() = PropertyValueName(this.value)

class AsmPathSimple(
    override val value: String
) : AsmPath {

    companion object {
        const val SEPARATOR = "/"

        //val EXTERNAL = AsmPathSimple("§external")
        val ROOT = AsmPathSimple(SEPARATOR)
    }

    override val segments: List<String> get() = this.value.split(SEPARATOR)

    override val parent: AsmPath?
        get() = when {
            ROOT == this -> null
            else -> AsmPathSimple(this.value.substringBeforeLast("/"))
        }

    //override val isExternal: Boolean get() = EXTERNAL.value == this.value

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

    fun addRoot(root: AsmValue) = (this.root as MutableList).add(root)
    fun removeRoot(root: Any)= (this.root as MutableList).remove(root)

    fun createStructure(parsePath: String, typeName: QualifiedName): AsmStructureSimple {
        val el = AsmStructureSimple(typeName)
        el.parsePath = parsePath
        //this.elementIndex[asmPath] = el
        return el
    }

    override fun addToIndex(value: AsmStructure) {
        this.elementIndex[AsmPathSimple(value.parsePath.toString())] = value //FIXME: should use asmPath !
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

    override fun asString(indent: Indent): String = this.root.joinToString(separator = "\n") {
        it.asString(indent)
    }

}

abstract class AsmValueAbstract() : AsmValue {
    override val typeName get() = qualifiedTypeName.last
}

object AsmNothingSimple : AsmValueAbstract(), AsmNothing {
    override val qualifiedTypeName: QualifiedName get() = StdLibDefault.NothingType.qualifiedTypeName
    override fun asString(indent: Indent): String = $$"$nothing"
    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmNothing -> false
        else -> true
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when (other) {
        !is AsmNothing -> false
        else -> true
    }

    override fun toString(): String = $$"$nothing"
}

class AsmAnySimple(
    override val value: Any
) : AsmValueAbstract(), AsmAny {
    companion object {
        fun stdAny(value: Any) = AsmAnySimple(value)
    }

    override val qualifiedTypeName: QualifiedName get() = StdLibDefault.AnyType.qualifiedTypeName

    override fun asString(indent: Indent): String = "AsmAny($value)"
    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmAny -> false
        other.value != this.value -> false
        else -> true
    }

    override fun hashCode(): Int = listOf(qualifiedTypeName, value).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is AsmAny -> false
        this.value != other.value -> false
        else -> true
    }

    override fun toString(): String = "$qualifiedTypeName($value)"
}

class AsmPrimitiveSimple(
    override val qualifiedTypeName: QualifiedName,
    override val value: Any
) : AsmValueAbstract(), AsmPrimitive {

    companion object {
        fun stdString(value: String) = AsmPrimitiveSimple(StdLibDefault.String.qualifiedTypeName, value)
        fun stdBoolean(value: Boolean) = AsmPrimitiveSimple(StdLibDefault.Boolean.qualifiedTypeName, value)
        fun stdInteger(value: Long) = AsmPrimitiveSimple(StdLibDefault.Integer.qualifiedTypeName, value)
        fun stdReal(value: Double) = AsmPrimitiveSimple(StdLibDefault.Real.qualifiedTypeName, value)
    }

    override fun asString(indent: Indent): String = "'$value'"
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

val AsmValue.isStdString get() = this is AsmPrimitive && this.qualifiedTypeName == StdLibDefault.String.qualifiedTypeName
val AsmValue.isStdInteger get() = this is AsmPrimitive && this.qualifiedTypeName == StdLibDefault.Integer.qualifiedTypeName
val AsmValue.isNothing get() = this is AsmNothing

val AsmValue.raw: Any
    get() = when (this) {
        is AsmNothing -> Unit
        is AsmAny -> this.value
        is AsmPrimitive -> this.value
        is AsmList -> this.elements.map { it.raw }
        else -> {
            TODO()
        }
    }

class AsmReferenceSimple(
    override val reference: String,
    override var value: AsmStructure?
) : AsmValueAbstract(), AsmReference {

    override val qualifiedTypeName: QualifiedName
        get() = when (value) {
            null -> StdLibDefault.NothingType.qualifiedTypeName
            else -> value!!.qualifiedTypeName
        }

    override fun resolveAs(value: AsmStructure?) {
        this.value = value
    }

    override fun asString(indent: Indent): String = when (value) {
        null -> "<unresolved> &$reference"
        else -> "&{'${value!!.parsePath.toString()}' : ${value!!.typeName}}"
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
        else -> "&{'${value!!.semanticPath?.value ?: value!!.parsePath.toString()}' : ${value!!.typeName}}"
    }
}

class AsmStructureSimple(
    override val qualifiedTypeName: QualifiedName
) : AsmValueAbstract(), AsmStructure {

    private var _properties = mutableMapOf<PropertyValueName, AsmStructurePropertySimple>()

    override var parsePath: String = "??"
    override var semanticPath: AsmPath? = null

    override val property: Map<PropertyValueName, AsmStructureProperty> = _properties
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

    override fun hasProperty(name: PropertyValueName): Boolean = property.containsKey(name)

    fun getPropertyAsStringOrNull(name: PropertyValueName): String? = property[name]?.value as String?
    fun getPropertyAsAsmElementOrNull(name: PropertyValueName): AsmStructureSimple? = property[name]?.value as AsmStructureSimple?
    fun getPropertyAsReferenceOrNull(name: PropertyValueName): AsmReferenceSimple? = property[name]?.value as AsmReferenceSimple?
    fun getPropertyAsListOrNull(name: PropertyValueName): List<Any>? = property[name]?.value as List<Any>?

    override fun getPropertyOrNothing(name: PropertyValueName): AsmValue = property[name]?.value ?: AsmNothingSimple
    override fun getProperty(name: PropertyValueName): AsmValue = property[name]?.value ?: error("Cannot find property '$name' in element type '$typeName' with path '$parsePath' ")
    fun getPropertyAsString(name: PropertyValueName): String = (getProperty(name) as AsmPrimitive).value as String
    fun getPropertyAsAsmElement(name: PropertyValueName): AsmStructureSimple = getProperty(name) as AsmStructureSimple
    fun getPropertyAsReference(name: PropertyValueName): AsmReferenceSimple = getProperty(name) as AsmReferenceSimple
    fun getPropertyAsList(name: PropertyValueName): List<Any> = getProperty(name) as List<Any>
    fun getPropertyAsListOfElement(name: PropertyValueName): List<AsmStructureSimple> = getProperty(name) as List<AsmStructureSimple>

    override fun setProperty(name: PropertyValueName, value: AsmValue, childIndex: Int) {
        _properties[name] = AsmStructurePropertySimple(name, childIndex, value)
    }

    fun addAllProperty(value: List<AsmStructurePropertySimple>) {
        value.forEach { this._properties[it.name] = it }
    }

    override fun asString(indent: Indent): String {
        val propsStr = this.property.values.joinToString(separator = "\n") {
            if (it.isReference) {
                val ref = it.value as AsmReferenceSimple
                "${indent.inc}${it.name} = $ref"
            } else {
                "${indent.inc}${it.name} = ${it.value.asString(indent.inc)}"
            }
        }
        //return ":$typeName $propsStr"
        return ":$typeName {\n$propsStr\n$indent}"
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmStructure -> false
        this.qualifiedTypeName != other.qualifiedTypeName -> false
        this.parsePath != other.parsePath -> false
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

    override fun hashCode(): Int = parsePath.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is AsmStructureSimple -> this.parsePath == other.parsePath //&& this.asm == other.asm
        else -> false
    }

    override fun toString(): String = ":$typeName[${semanticPath?.value ?: parsePath.toString()}] { ${this.property.values.joinToString()}} }"

}

class AsmStructurePropertySimple(
    override val name: PropertyValueName,
    override val index: Int,
    value: AsmValue
) : AsmStructureProperty {

    companion object {
        const val TO_STRING_MAX_LEN = 30
    }

    override var value: AsmValue = value; private set

    override val isReference: Boolean get() = this.value is AsmReferenceSimple

    override fun convertToReferenceTo(referredValue: AsmStructure?) {
        val v = this.value
        when {
            v is AsmNothing -> error("Cannot convert property '$this' a reference, it has value $AsmNothingSimple")
            v is AsmReference -> v.resolveAs(referredValue)
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
            is AsmList -> {
                val elems = v.elements.joinToString()
                val elemsStr = when {
                    elems.length > TO_STRING_MAX_LEN -> elems.substring(0, TO_STRING_MAX_LEN) + "..."
                    else -> elems
                }
                "$name = [$elemsStr]"
            }

            is AsmPrimitive -> if (isReference) "$name = &${v}" else "$name = ${v}"
            else -> "$name = ${v}"
        }
    }
}

class AsmListSimple(
    override val elements: List<AsmValue>
) : AsmValueAbstract(), AsmList {
    override val qualifiedTypeName get() = StdLibDefault.List.qualifiedName

    override val isEmpty: Boolean get() = elements.isEmpty()
    override val isNotEmpty: Boolean get() = elements.isNotEmpty()

    override fun asString(indent: Indent): String = when {
        elements.isEmpty() -> "[]"
        1 == elements.size -> "[ ${elements[0].asString(indent.inc)} ]"
        else -> "[\n${this.elements.joinToString(separator = "\n") { "${indent.inc}${it.asString(indent.inc)}" }}\n$indent]"
    }

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
    override val qualifiedTypeName get() = StdLibDefault.ListSeparated.qualifiedName

    override val isEmpty: Boolean get() = elements.isEmpty()
    override val isNotEmpty: Boolean get() = elements.isNotEmpty()

    override fun asString(indent: Indent): String = when {
        elements.isEmpty() -> "[]"
        1 == elements.size -> "[ ${elements[0].asString(indent.inc)} ]"
        else -> "[\n${this.elements.joinToString(separator = "\n") { "${indent.inc}${it.asString(indent.inc)}" }}\n$indent]"
    }
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

class AsmLambdaSimple(
    val lambda: (it: AsmValue) -> AsmValue
) : AsmValueAbstract(), AsmLambda {

    override val qualifiedTypeName = StdLibDefault.Lambda.qualifiedTypeName

    override fun invoke(args: Map<String, AsmValue>): AsmValue {
        val it = args["it"]!!
        return this.lambda.invoke(it)
    }

    override fun equalTo(other: AsmValue): Boolean {
        return false
    }

    override fun asString(indent: Indent): String {
        return "{ <lambda expression> }" //TODO
    }
}