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

package net.akehurst.language.api.typeModel

import net.akehurst.language.agl.util.Debug

interface TypeModel {

    val types: Map<String, RuleType>

    fun findType(name: String): RuleType?
}

sealed class RuleType {
    abstract val name: String

    fun asString(): String = when (this) {
        is NothingType -> name
        is AnyType -> name
        is StringType -> name
        is UnnamedSuperTypeType -> "? supertypeOf " + this.subtypes.sortedBy { it.name }.joinToString(prefix = "(", postfix = ")") { it.name }
        is ListSimpleType -> "${name}<${this.elementType.asString()}>"
        is ListSeparatedType -> "${name}<${itemType.asString()}, ${separatorType.asString()}>"
        is TupleType -> "${name}<${this.properties.joinToString { it.name + ":" + it.type.asString() }}>"
        is ElementType -> {
            val sups = this.superType.sortedBy { it.name }.joinToString(prefix = " : ") { it.name }
            val props = this.property.values.sortedBy { it.name }.joinToString { it.name + ":" + it.type.asString() }
            "${name}${sups} { $props }"
        }
    }
}

object StringType : RuleType() {
    override val name: String = "\$String"
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object AnyType : RuleType() {
    override val name: String = "\$Any"
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object NothingType : RuleType() {
    override val name: String = "\$Nothing"
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

class UnnamedSuperTypeType(val subtypes: List<RuleType>) : RuleType() {
    companion object {
        const val INSTANCE_NAME = "UnnamedSuperType"
    }

    override val name: String = INSTANCE_NAME

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSuperTypeType -> other.subtypes == this.subtypes
        else -> false
    }

    override fun toString(): String = name
}

class ListSimpleType(val elementType: RuleType) : RuleType() {
    companion object {
        const val INSTANCE_NAME = "\$List"
    }

    override val name: String = INSTANCE_NAME

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is ListSimpleType -> other.elementType == this.elementType
        else -> false
    }

    override fun toString(): String = name
}

class ListSeparatedType(val itemType: RuleType, val separatorType: RuleType) : RuleType() {
    companion object {
        const val INSTANCE_NAME = "\$SList"
    }

    override val name: String = INSTANCE_NAME

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is ListSeparatedType -> other.itemType == this.itemType && other.separatorType == this.separatorType
        else -> false
    }

    override fun toString(): String = name
}

sealed class StructuredRuleType : RuleType() {
    abstract val property: MutableMap<String, PropertyDeclaration>
    abstract fun getPropertyByIndex(i: Int): PropertyDeclaration
    abstract fun appendProperty(name: String, propertyDeclaration: PropertyDeclaration)
}

class TupleType() : StructuredRuleType() {
    companion object {
        const val INSTANCE_NAME = "\$Tuple"
    }

    @DslMarker
    private annotation class TupleTypeConstructorMarker;

    constructor(init:TupleType.()->Unit) :this() {
        this.init()
    }

    override val name: String = INSTANCE_NAME
    override val property = mutableMapOf<String, PropertyDeclaration>()
    val properties = mutableListOf<PropertyDeclaration>()

    private val nameTypePair get() = properties.map { Pair(it.name, it.type) }

    override fun getPropertyByIndex(i: Int): PropertyDeclaration = properties[i]

    override fun appendProperty(name: String, propertyDeclaration: PropertyDeclaration) {
        if (Debug.CHECK) check(propertyDeclaration.owner == this)
        if (Debug.CHECK) check(this.property.containsKey(name).not())
        this.property[name] = propertyDeclaration
        this.properties.add(propertyDeclaration)
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when {
        other !is TupleType -> false
        this.nameTypePair != other.nameTypePair -> false
        else -> true
    }

    override fun toString(): String = "Tuple<${this.properties.joinToString { it.name + ":"+it.type.name }}>"
}

data class ElementType(override val name: String) : StructuredRuleType() {
    val superType: Set<ElementType> = mutableSetOf<ElementType>()
    val subType: Set<ElementType> = mutableSetOf<ElementType>()
    override val property = mutableMapOf<String, PropertyDeclaration>()
    private val _propertyIndex = mutableListOf<PropertyDeclaration>()

    fun addSuperType(type: ElementType) {
        (this.superType as MutableSet).add(type)
        (type.subType as MutableSet).add(this)
    }

    override fun getPropertyByIndex(i: Int): PropertyDeclaration = _propertyIndex[i]
    override fun appendProperty(name: String, propertyDeclaration: PropertyDeclaration) {
        if (Debug.CHECK) check(propertyDeclaration.owner == this)
        if (Debug.CHECK) check(this.property.containsKey(name).not())
        this.property[name] = propertyDeclaration
        this._propertyIndex.add(propertyDeclaration)
    }
}

data class PropertyDeclaration(
    val owner: StructuredRuleType,
    val name: String,
    val type: RuleType,
    val isNullable: Boolean,
    val childIndex: Int // to indicate the child number in an SPPT
) {
    init {
        owner.appendProperty(name, this)
    }

    override fun toString(): String {
        val nullable = if (isNullable) "?" else ""
        return "${owner.name}.$name: ${type.name} $nullable [$childIndex]"
    }
}

fun TypeModel.asString(): String {
    val typesSorted = this.types.values.sortedBy { it.name }
    return typesSorted.joinToString(separator = "\n") { it.asString() }
}