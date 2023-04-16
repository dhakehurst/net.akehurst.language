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

package net.akehurst.language.api.typemodel

import net.akehurst.language.agl.util.Debug

interface TypeModel {

    val namespace: String
    val name: String

    /**
     * ruleName -> RuleType
     */
    val types: Map<String, RuleType>

    fun findTypeForRule(ruleName: String): RuleType?
}

sealed class RuleType {
    abstract val name: String

    abstract fun signature(context:TypeModel):String

    fun asString(context:TypeModel): String = when (this) {
        is NothingType -> signature(context)
        is AnyType -> signature(context)
        is StringType -> signature(context)
        is UnnamedSuperTypeType -> signature(context)
        is ListSimpleType -> signature(context)
        is ListSeparatedType -> signature(context)
        is TupleType -> signature(context)
        is ElementType -> {
            val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context) }.joinToString { it.signature(context) }
            val props = this.property.values.sortedBy { it.name }.joinToString { it.name + ":" + it.type.signature(context) }
            "${name}${sups} { $props }"
        }
    }
}

interface WithSubtypes {
    val subtypes: List<RuleType>
}

object StringType : RuleType() {
    override val name: String = "\$String"
    override fun signature(context:TypeModel):String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object AnyType : RuleType() {
    override val name: String = "\$Any"
    override fun signature(context:TypeModel):String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object NothingType : RuleType() {
    override val name: String = "\$Nothing"
    override fun signature(context:TypeModel):String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

class UnnamedSuperTypeType(
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: List<RuleType>
) : RuleType(), WithSubtypes {
    companion object {
        const val INSTANCE_NAME = "UnnamedSuperType"
    }

    override val name: String = INSTANCE_NAME
    override fun signature(context:TypeModel):String = "? supertypeOf " + this.subtypes.sortedBy { it.signature(context) }.joinToString(prefix = "(", postfix = ")") { it.signature(context) }

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSuperTypeType -> other.subtypes == this.subtypes
        else -> false
    }

    override fun toString(): String = name
}

class ListSimpleType() : RuleType() {
    companion object {
        const val INSTANCE_NAME = "\$List"
    }

    //usage should set this to something else,
    //but needs to not be a constructor param to avoid recursion when constructing typemodel
    var elementType: RuleType = NothingType

    override val name: String = INSTANCE_NAME
    override fun signature(context:TypeModel):String = "${name}<${this.elementType.signature(context)}>"

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is ListSimpleType -> other.elementType == this.elementType
        else -> false
    }

    override fun toString(): String = name
}

class ListSeparatedType() : RuleType() {
    companion object {
        const val INSTANCE_NAME = "\$SList"
    }
    //usage should set this to something else,
    //but needs to not be a constructor param to avoid recursion when constructing typemodel
    var itemType: RuleType = NothingType
    //usage should set this to something else,
    //but needs to not be a constructor param to avoid recursion when constructing typemodel
    var separatorType: RuleType = NothingType

    override val name: String = INSTANCE_NAME
    override fun signature(context:TypeModel):String = "${name}<${itemType.signature(context)}, ${separatorType.signature(context)}>"

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

    constructor(init: TupleType.() -> Unit) : this() {
        this.init()
    }

    override val name: String = INSTANCE_NAME

    override val property = mutableMapOf<String, PropertyDeclaration>()
    val properties = mutableListOf<PropertyDeclaration>()

    private val nameTypePair get() = properties.map { Pair(it.name, it.type) }

    override fun signature(context:TypeModel):String = "${name}<${this.properties.joinToString { it.name + ":" + it.type.signature(context) }}>"

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

    override fun toString(): String = "Tuple<${this.properties.joinToString { it.name + ":" + it.type.name }}>"
}

data class ElementType(
    val typeModel: TypeModel,
    override val name: String
) : StructuredRuleType(), WithSubtypes {

    val supertypes: Set<ElementType> = mutableSetOf<ElementType>()
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<ElementType> = mutableListOf<ElementType>()
    override val property = mutableMapOf<String, PropertyDeclaration>()
    private val _propertyIndex = mutableListOf<PropertyDeclaration>()

    override fun signature(context:TypeModel):String = if(context==typeModel) name else "${typeModel.name}.$name"

    fun addSuperType(type: ElementType) {
        (this.supertypes as MutableSet).add(type)
        (type.subtypes as MutableList).add(this)
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
    val typesSorted = this.types.entries.sortedBy { it.key }
    val types = typesSorted.joinToString(separator = "\n") { it.key + "->" + it.value.asString(this) }
    val s= """
    typemodel '$namespace.$name' {
    $types
    }
    """.trimIndent()
    return s
}