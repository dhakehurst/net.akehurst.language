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

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.AnyExt.asString
import net.akehurst.language.asm.simple.AnyExt.equalTo
import net.akehurst.language.base.api.Formatable
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.asm.StdLibDefault

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

open class AsmSimple(
    val objectGraph: ObjectGraphAccessorMutator,
) : Asm {

    companion object {
        fun traverseDepthFirst(roots: List<Any>, walker: AsmTreeWalker) {
            fun traverse(owningProperty: AsmStructureProperty?, value: Any?) {
                when (value) {
                    null -> walker.onNothing(owningProperty, Unit)
                    is Collection<*> -> {
                        walker.beforeList(owningProperty, value)
                        value.forEach { el ->
                            traverse(owningProperty, el)
                        }
                        walker.afterList(owningProperty, value)
                    }

                    is AsmNothing -> walker.onNothing(owningProperty, Unit)
                    is AsmPrimitive -> walker.onPrimitive(owningProperty, value)
                    is AsmStructure -> {
                        walker.beforeStructure(owningProperty, value)
                        for (prop in value.propertyOrdered) {
                            walker.onProperty(value, prop)
                            val pv = prop.value
                            traverse(prop, pv)
                        }
                        walker.afterStructure(owningProperty, value)
                    }

                    is AsmList -> {
                        walker.beforeList(owningProperty, value.elements)
                        value.elements.forEach { el -> traverse(owningProperty, el) }
                        walker.afterList(owningProperty, value.elements)
                    }

                    is AsmListSeparated -> {
                        walker.beforeList(owningProperty, value.elements)
                        value.elements.forEach { el -> traverse(owningProperty, el) }
                        walker.afterList(owningProperty, value.elements)
                    }

                    else -> Unit
                }
            }
            roots.forEach {
                walker.beforeRoot(it)
                traverse(null, it)
                walker.afterRoot(it)
            }
        }
    }

    override val root: List<Any> = mutableListOf()
    override val elementIndex = mutableMapOf<AsmPath, AsmStructure>()

    fun addRoot(root: Any) = (this.root as MutableList).add(root)
    fun removeRoot(root: Any) = (this.root as MutableList).remove(root)

    fun createStructure(parsePath: String, typeName: QualifiedName): AsmStructureSimple {
        val obj = objectGraph.createStructureValue(typeName, emptyMap())
        val el = obj.self as AsmStructureSimple
        el.parsePath = parsePath
        //this.elementIndex[asmPath] = el
        return el
    }

    override fun addToIndex(value: AsmStructure) {
        this.elementIndex[AsmPathSimple(value.parsePath.toString())] = value //FIXME: should use asmPath !
    }

    override fun traverseDepthFirst(walker: AsmTreeWalker) = traverseDepthFirst(this.root, walker)

    override fun asString(indent: Indent): String = this.root.joinToString(separator = "\n") {
        it.asString(indent)
    }

}

abstract class AsmValueAbstract() : AsmValue {
    override val typeName get() = qualifiedTypeName.last
}

@Deprecated("Use Unit instead")
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
     //   fun stdAny(value: Any) = AsmAnySimple(value)
    }

    override val qualifiedTypeName: QualifiedName get() = StdLibDefault.AnyType.qualifiedTypeName

    override fun asString(indent: Indent): String = value.asString(indent)

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

@Deprecated("Use normal kotlin primitive value instead")
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

    override fun asString(indent: Indent): String = value.asString(indent)
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

val Any.raw: Any
    get() = when (this) {
        is AsmNothing -> Unit
        is AsmAny -> this.value
        is AsmPrimitive -> this.value
        is AsmReference -> this.value ?: Unit
        is AsmListSeparated -> this.elements.map { it.raw }.toSeparatedList()
        is AsmListSimple -> this.elements.map { it.raw }
//        is AsmStructure -> this.property.values
//            .sortedBy { it.index }
//            .associate { pv -> Pair(pv.name.value, pv.value.raw) }
        is AsmStructure -> this
        is AsmLambda -> TODO()
        else -> this //error("Unknown subtype of AsmValue '${this::class.simpleName}'")
    }

object AnyExt {
    fun Any.asString(indent: Indent = Indent()): String = when (this) {
        is Unit -> AsmNothingSimple.asString(indent)
        is String -> "'$this'"
        is AsmValue -> this.asString(indent)
        is TypedObject -> this.self.asString(indent)
        is Formatable -> this.asString(indent)
        is Collection<*> -> when {
            isEmpty() -> "[]"
            1 == size -> "[ ${this.first()?.asString(indent)} ]"
            else -> {
                "[\n${this.joinToString(separator = "\n") { "${indent.inc}${it?.asString(indent.inc)}" }}\n$indent]"
            }
        }

        else -> this.toString()
    }

    val Any.toAsmSimple: AsmValue
        get() = when (this) {
            Unit -> AsmNothingSimple
            is AsmValue -> this
            is String -> AsmPrimitiveSimple(StdLibDefault.String.qualifiedTypeName, this)
            is Boolean -> AsmPrimitiveSimple(StdLibDefault.Boolean.qualifiedTypeName, this)
            is Int -> AsmPrimitiveSimple(StdLibDefault.Integer.qualifiedTypeName, this.toLong())
            is Long -> AsmPrimitiveSimple(StdLibDefault.Integer.qualifiedTypeName, this)
            is Float -> AsmPrimitiveSimple(StdLibDefault.Real.qualifiedTypeName, this.toDouble())
            is Double -> AsmPrimitiveSimple(StdLibDefault.Real.qualifiedTypeName, this)
            is ListSeparated<*, *, *> -> AsmListSeparatedSimple(this.map { it?.toAsmSimple ?: Unit }.toSeparatedList())
            is List<*> -> AsmListSimple(this.map { it?.toAsmSimple ?: Unit })
            else -> error("Type cannot be converted to AsmValue '${this::class.simpleName}'")
        }

    fun Any.equalTo(other: Any): Boolean {
        return when {
            this is AsmReferenceSimple && other is AsmReferenceSimple -> this.equalTo(other)
            this is AsmStructureSimple && other is AsmStructureSimple -> this.equalTo(other)
            else -> this == other
        }
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
        else -> "&{'${value!!.qualifiedName(".") ?: value!!.syntaxAnalyserPath?.value ?: value!!.parsePath}' : ${value!!.typeName}}"
    }
}

class AsmStructureSimple(
    override val qualifiedTypeName: QualifiedName
) : AsmValueAbstract(), AsmStructure {

    private var _properties = mutableMapOf<PropertyValueName, AsmStructureProperty>()

    override var parsePath: String = "??"
    override var syntaxAnalyserPath: AsmPath? = null //TODO: not sure if still need this
    override var semanticQualifiedPath: List<String>? = null; private set

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


    override fun qualifiedName(separator: String): String? =
        semanticQualifiedPath?.joinToString(separator)

    override fun setSemanticQualifiedPath(segments: List<String>) {
        semanticQualifiedPath = segments
    }

    override fun hasProperty(name: PropertyValueName): Boolean = property.containsKey(name)

    fun getPropertyAsReferenceOrNull(name: PropertyValueName): AsmReferenceSimple? = property[name]?.value as AsmReferenceSimple?
    fun getPropertyAsListOrNull(name: PropertyValueName): List<Any>? = property[name]?.value as List<Any>?

    override fun getProperty(name: PropertyValueName): Any = property[name]?.value ?: error("Cannot find property '$name' in element type '$typeName' with path '$parsePath' ")
    override fun getPropertyOrNothing(name: PropertyValueName): Any = property[name]?.value ?: Unit
    override fun getPropertyOrNull(name: PropertyValueName): Any? = property[name]?.value

    fun getPropertyAsReference(name: PropertyValueName): AsmReferenceSimple = getProperty(name) as AsmReferenceSimple
    fun getPropertyAsList(name: PropertyValueName): List<Any> = getProperty(name) as List<Any>
    fun getPropertyAsListOfElement(name: PropertyValueName): List<AsmStructureSimple> = getProperty(name) as List<AsmStructureSimple>

    override fun setProperty(name: PropertyValueName, value: Any, childIndex: Int) {
        _properties[name] = AsmStructurePropertySimple(name, childIndex, value)
    }

    fun addAllProperty(value: List<AsmStructureProperty>) {
        value.forEach { this._properties[it.name] = it }
    }

    override fun asString(indent: Indent): String {
        return when {
            this.property.isEmpty() -> ":$typeName { }"
            else -> {
                val propsStr = this.property.values.joinToString(separator = "\n") {
                    if (it.isReference) {
                        val ref = it.value as AsmReferenceSimple
                        "${indent.inc}${it.name} = $ref"
                    } else {
                        "${indent.inc}${it.name} = ${it.value.asString(indent.inc)}"
                    }
                }
                ":$typeName {\n$propsStr\n$indent}"
            }
        }
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmStructure -> false
        this.qualifiedTypeName != other.qualifiedTypeName -> false
        //this.parsePath != other.parsePath -> false
        else -> when {
            null != this.semanticQualifiedPath && null != other.semanticQualifiedPath -> this.semanticQualifiedPath == other.semanticQualifiedPath
            else -> when {
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
        }

    }

    override fun hashCode(): Int = semanticQualifiedPath?.hashCode() ?: parsePath.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is AsmStructureSimple -> when {
            null != this.semanticQualifiedPath && null != other.semanticQualifiedPath -> this.semanticQualifiedPath == other.semanticQualifiedPath
            else -> this.parsePath == other.parsePath //&& this.asm == other.asm
        }

        else -> false
    }

    override fun toString(): String = ":$typeName[${qualifiedName(".") ?: syntaxAnalyserPath?.value ?: parsePath}]"

}

class AsmStructurePropertySimple(
    override val name: PropertyValueName,
    override val index: Int,
    value: Any
) : AsmStructureProperty {

    companion object {
        const val TO_STRING_MAX_LEN = 30
    }

    override var value: Any = value; private set

    override val isReference: Boolean get() = this.value is AsmReferenceSimple

    override fun convertToReferenceTo(referredValue: AsmStructure?) {
        val v = this.value
        when {
            Unit == v -> error("Cannot convert property '$this' a reference, it has value $AsmNothingSimple")
            v is String -> {
                val ref = AsmReferenceSimple(v, referredValue)
                this.value = ref
            }

            v is List<*> && v.all { it is String } -> {
                val refValue = v.joinToString(separator = ".") { it as String }
                val ref = AsmReferenceSimple(refValue, referredValue)
                this.value = ref
            }

            // Deprecated
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
                    t.equalTo(o)
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

@Deprecated("use kotlin Set")
class AsmSetSimple(
    override val elements: Set<AsmValue>
) : AsmValueAbstract(), AsmSet {
    override val qualifiedTypeName get() = StdLibDefault.List.qualifiedName

    override val isEmpty: Boolean get() = elements.isEmpty()
    override val isNotEmpty: Boolean get() = elements.isNotEmpty()

    override fun asString(indent: Indent): String = when {
        elements.isEmpty() -> "[]"
        1 == elements.size -> "[ ${elements.first().asString(indent)} ]"
        else -> "[\n${this.elements.joinToString(separator = "\n") { "${indent.inc}${it.asString(indent.inc)}" }}\n$indent]"
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmSet -> false
        other.elements.size != this.elements.size -> false
        else -> {
            this.elements.all { tEl ->
                other.elements.any { othEl -> tEl.equalTo(othEl) }
            }
        }
    }

    override fun hashCode(): Int = elements.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is AsmList -> false
        else -> this.elements == other.elements //TODO: should use equalTo on elements !
    }

    override fun toString(): String = "Set(${elements.joinToString()})"
}

@Deprecated("use kotlin List")
class AsmListSimple(
    override val elements: List<Any>
) : AsmValueAbstract(), AsmList {
    override val qualifiedTypeName get() = StdLibDefault.List.qualifiedName

    override val isEmpty: Boolean get() = elements.isEmpty()
    override val isNotEmpty: Boolean get() = elements.isNotEmpty()

    override fun asString(indent: Indent): String = when {
        elements.isEmpty() -> "[]"
        1 == elements.size -> "[ ${elements[0].asString(indent)} ]"
        else -> "[\n${this.elements.joinToString(separator = "\n") { "${indent.inc}${it.asString(indent.inc)}" }}\n$indent]"
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmList -> false
        other.elements.size != this.elements.size -> false
        else -> {
            (0 until this.elements.size).all {
                this.elements[it].equalTo(other.elements[it])
            }
        }
    }

    override fun hashCode(): Int = elements.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is AsmList -> false
        else -> this.elements == other.elements
    }

    override fun toString(): String = "List(${elements.joinToString()})"
}

@Deprecated("use nak ListSeparated")
class AsmListSeparatedSimple(
    override val elements: ListSeparated<Any, Any, Any>
) : AsmValueAbstract(), AsmListSeparated {
    override val qualifiedTypeName get() = StdLibDefault.ListSeparated.qualifiedName

    override val isEmpty: Boolean get() = elements.isEmpty()
    override val isNotEmpty: Boolean get() = elements.isNotEmpty()

    override fun asString(indent: Indent): String = when {
        elements.isEmpty() -> "[]"
        1 == elements.size -> "[ ${elements[0].asString(indent)} ]"
        else -> "[\n${this.elements.joinToString(separator = "\n") { "${indent.inc}${it.asString(indent.inc)}" }}\n$indent]"
    }

    override fun equalTo(other: AsmValue): Boolean = when {
        other !is AsmListSeparated -> false
        other.elements.size != this.elements.size -> false
        else -> {
            (0 until this.elements.size).all {
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

@Deprecated("use kotlin lambda")
class AsmLambdaSimple(
    val lambda: (it: Any) -> Any
) : AsmValueAbstract(), AsmLambda {

    override val qualifiedTypeName = StdLibDefault.Lambda.qualifiedTypeName

    override fun invoke(args: Map<String, Any>): Any {
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
