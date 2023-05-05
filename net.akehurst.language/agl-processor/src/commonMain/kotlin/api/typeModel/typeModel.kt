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
     * grammarRuleName -> TypeUsage
     */
    val allRuleNameToType: Map<String, TypeUsage>

    /**
     * RuleType.name --> RuleType
     */
    val allTypesByName: Map<String, RuleType>

    fun findTypeUsageForRule(ruleName: String): TypeUsage?
    fun findTypeNamed(typeName: String): RuleType?
}

sealed class RuleType {
    companion object {
        const val maxDepth = 5
    }

    abstract val name: String
    open val qualifiedName: String = name
    open val typeParameters: List<String> = emptyList()

    abstract fun signature(context: TypeModel?, currentDepth: Int = 0): String

    fun asString(context: TypeModel): String = when (this) {
        is NothingType -> signature(context, 0)
        is AnyType -> signature(context, 0)
        is StringType -> signature(context, 0)
        is UnnamedSuperTypeType -> signature(context, 0)
        is ListSimpleType -> signature(context, 0)
        is ListSeparatedType -> signature(context, 0)
        is TupleType -> signature(context, 0)
        is ElementType -> {
            val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
            val props = this.property.values.sortedBy { it.childIndex }.joinToString { it.name + ":" + it.typeUse.signature(context, 0) }
            "${name}${sups} { $props }"
        }
    }
}

object StringType : RuleType() {
    val use = TypeUsage.ofType(StringType)
    val useNullable = TypeUsage.ofType(StringType, emptyList(), true)

    override val name: String = "\$String"
    override fun signature(context: TypeModel?, currentDepth: Int): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object AnyType : RuleType() {
    val use = TypeUsage.ofType(StringType)
    val useNullable = TypeUsage.ofType(StringType, emptyList(), true)

    override val name: String = "\$Any"
    override fun signature(context: TypeModel?, currentDepth: Int): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object NothingType : RuleType() {
    val use = TypeUsage.ofType(NothingType)
    val useNullable = TypeUsage.ofType(NothingType, emptyList(), true)

    override val name: String = "\$Nothing"
    override fun signature(context: TypeModel?, currentDepth: Int): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

class UnnamedSuperTypeType(
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: List<TypeUsage>,
    val consumeNode: Boolean
) : RuleType() {
    companion object {
        const val INSTANCE_NAME = "UnnamedSuperType"
    }

    override val name: String = INSTANCE_NAME
    override fun signature(context: TypeModel?, currentDepth: Int): String {
        return when {
            currentDepth >= maxDepth -> "..."
            else -> "? supertypeOf " + this.subtypes.sortedBy { it.signature(context, currentDepth + 1) }
                .joinToString(prefix = "(", postfix = ")") { it.signature(context, currentDepth + 1) }
        }
    }

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSuperTypeType -> other.subtypes == this.subtypes
        else -> false
    }

    override fun toString(): String = name
}

object ListSimpleType : RuleType() {
    fun ofType(itemTypeUse: TypeUsage, nullable: Boolean = false) = TypeUsage(ListSimpleType, listOf(itemTypeUse), nullable)

    const val INSTANCE_NAME = "\$List"

    override val name: String = INSTANCE_NAME

    override val typeParameters = listOf("I")

    override fun signature(context: TypeModel?, currentDepth: Int): String = toString()

    override fun toString(): String = "${name}<I>"
}

object ListSeparatedType : RuleType() {
    fun ofType(itemTypeUse: TypeUsage, sepTypeUse: TypeUsage, nullable: Boolean = false) = TypeUsage(ListSeparatedType, listOf(itemTypeUse, sepTypeUse), nullable)

    const val INSTANCE_NAME = "\$SList"

    override val name: String = INSTANCE_NAME
    override val typeParameters = listOf("I", "S")
    override fun signature(context: TypeModel?, currentDepth: Int): String = toString()
    override fun toString(): String = "${name}<I,S>"
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

    private val nameTypePair get() = properties.map { Pair(it.name, it.typeUse) }

    override fun signature(context: TypeModel?, currentDepth: Int): String {
        return when {
            currentDepth >= maxDepth -> "..."
            else -> "${name}<${this.properties.joinToString { it.name + ":" + it.typeUse.signature(context, currentDepth + 1) }}>"
        }
    }

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

    override fun toString(): String = "Tuple<${this.properties.joinToString { it.name + ":" + it.typeUse }}>"
}

data class ElementType(
    val typeModel: TypeModel,
    override val name: String
) : StructuredRuleType() {

    override val qualifiedName: String = typeModel.name + "." + name

    val supertypes: Set<ElementType> = mutableSetOf<ElementType>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: MutableList<ElementType> = mutableListOf<ElementType>()
    override val property = mutableMapOf<String, PropertyDeclaration>()
    private val _propertyIndex = mutableListOf<PropertyDeclaration>()

    override fun signature(context: TypeModel?, currentDepth: Int): String = if (context == typeModel) name else "${typeModel.name}.$name"

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

data class TypeUsage(
    val type: RuleType,
    val arguments: List<TypeUsage>,
    val nullable: Boolean
) {
    companion object {
        fun ofType(type: RuleType, arguments: List<TypeUsage> = emptyList(), nullable: Boolean = false) = TypeUsage(type, arguments, nullable)
    }

    init {
        if (Debug.CHECK) check(type.typeParameters.size == arguments.size)
    }

    val notNullable get() = TypeUsage.ofType(this.type, arguments, false)

    fun signature(context: TypeModel?, currentDepth: Int): String {
        return when {
            currentDepth >= RuleType.maxDepth -> "..."
            else -> {
                val args = when {
                    arguments.isEmpty() -> ""
                    else -> "<${arguments.joinToString { it.signature(context, currentDepth + 1) }}>"
                }
                val n = when (nullable) {
                    true -> "?"
                    else -> ""
                }
                val name = when {
                    arguments.isEmpty() -> type.signature(context, currentDepth + 1)
                    else -> type.name
                }
                return "${name}$args$n"
            }
        }
    }

    override fun toString(): String {
        val args = when {
            arguments.isEmpty() -> ""
            else -> "<${arguments.joinToString { it.toString() }}>"
        }
        val n = when (nullable) {
            true -> "?"
            else -> ""
        }
        return "${type.name}$args$n"
    }
}

data class PropertyDeclaration(
    val owner: StructuredRuleType,
    val name: String,
    val typeUse: TypeUsage,
    val childIndex: Int // to indicate the child number in an SPPT
) {
    init {
        owner.appendProperty(name, this)
    }

    override fun toString(): String {
        val nullable = if (typeUse.nullable) "?" else ""
        return "${owner.name}.$name: ${typeUse.type.name}$nullable [$childIndex]"
    }
}

fun TypeModel.asString(): String {
    val rules = this.allRuleNameToType.entries.sortedBy { it.key }
    val ruleToType = rules.joinToString(separator = "\n") { it.key + "->" + it.value.signature(this, 0) }
    val types = this.allTypesByName.entries.sortedBy { it.key }.joinToString(separator = "\n") { it.value.asString(this) }
    val s = """
    typemodel '$namespace.$name' {
    $ruleToType
    
    $types
    }
    """.trimIndent()
    return s
}