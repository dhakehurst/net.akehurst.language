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

package net.akehurst.language.typemodel.api

interface TypeModel {

    val namespace: String
    val name: String
    val rootTypeName: String?
    val imports: Set<TypeModel>

    val qualifiedName: String

    /**
     * TypeDefinition.name --> TypeDefinition
     */
    val allTypesByName: Map<String, TypeDefinition>

    val allTypes: Collection<TypeDefinition>

    val elementType: Set<ElementType>

    val primitiveType: Set<PrimitiveType>

    fun findTypeNamed(typeName: String): TypeDefinition?

    fun findOrCreateElementTypeNamed(typeName: String): ElementType

    fun findOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType

    fun createUnnamedSuperTypeType(subtypes: List<TypeUsage>): UnnamedSuperTypeType
}

sealed class TypeDefinition {
    companion object {
        const val maxDepth = 10
    }

    abstract val name: String
    open val qualifiedName: String = name
    open val typeParameters: List<String> = emptyList()

    /**
     * information about this type
     */
    var metaInfo = mutableMapOf<String, String>()

    abstract fun signature(context: TypeModel?, currentDepth: Int = 0): String

    fun typeUse(arguments: List<TypeUsage> = emptyList(), nullable: Boolean = false): TypeUsage = TypeUsage.ofType(this, arguments, nullable)

    fun asString(context: TypeModel): String = when (this) {
        is NothingType -> signature(context, 0)
        is PrimitiveType -> signature(context, 0)
        is AnyType -> signature(context, 0)
        //is StringType -> signature(context, 0)
        is UnnamedSuperTypeType -> signature(context, 0)
        is ListSimpleType -> signature(context, 0)
        is ListSeparatedType -> signature(context, 0)
        is TupleType -> signature(context, 0)
        is ElementType -> {
            val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
            val props = this.property.values.sortedBy { it.childIndex }
                .joinToString { it.name + ":" + it.typeUse.signature(context, 0) }
            "${name}${sups} { $props }"
        }
    }
}

object AnyType : TypeDefinition() {
    val use = TypeUsage.ofType(AnyType)
    val useNullable = TypeUsage.ofType(AnyType, emptyList(), true)

    override val name: String = "\$Any"
    override fun signature(context: TypeModel?, currentDepth: Int): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

object NothingType : TypeDefinition() {
    val use = TypeUsage.ofType(NothingType)
    val useNullable = TypeUsage.ofType(NothingType, emptyList(), true)

    override val name: String = "\$Nothing"
    override fun signature(context: TypeModel?, currentDepth: Int): String = name
    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other
    override fun toString(): String = name
}

class UnnamedSuperTypeType(
    val id: Int, // needs a number else can't implement equals without a recursive loop
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: List<TypeUsage>
) : TypeDefinition() {
    companion object {
        const val INSTANCE_NAME = "UnnamedSuperType"
    }

    override val name: String = INSTANCE_NAME
    override fun signature(context: TypeModel?, currentDepth: Int): String {
        return when {
            currentDepth >= maxDepth -> "..."
            else -> "? supertypeOf " + this.subtypes.sortedBy { it.signature(context, currentDepth + 1) }
                .joinToString(prefix = "(", postfix = ")", separator = " | ") { it.signature(context, currentDepth + 1) }
        }
    }

    override fun hashCode(): Int = id
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSuperTypeType -> other.id == this.id
        else -> false
    }

    override fun toString(): String = name
}

object ListSimpleType : TypeDefinition() {
    fun ofType(itemTypeUse: TypeUsage, nullable: Boolean = false) = TypeUsage(ListSimpleType, listOf(itemTypeUse), nullable)

    const val INSTANCE_NAME = "\$List"

    override val name: String = INSTANCE_NAME

    override val typeParameters = listOf("I")

    override fun signature(context: TypeModel?, currentDepth: Int): String = toString()

    override fun toString(): String = "${name}<I>"
}

object ListSeparatedType : TypeDefinition() {
    fun ofType(itemTypeUse: TypeUsage, sepTypeUse: TypeUsage, nullable: Boolean = false) = TypeUsage(ListSeparatedType, listOf(itemTypeUse, sepTypeUse), nullable)

    const val INSTANCE_NAME = "\$SList"

    override val name: String = INSTANCE_NAME
    override val typeParameters = listOf("I", "S")
    override fun signature(context: TypeModel?, currentDepth: Int): String = toString()
    override fun toString(): String = "${name}<I,S>"
}

class PrimitiveType(
    val typeModel: TypeModel,
    override val name: String
) : TypeDefinition() {
    val use = TypeUsage.ofType(this)
    val useNullable = TypeUsage.ofType(this, emptyList(), true)

    override val qualifiedName: String get() = "${typeModel.qualifiedName}.$name"

    override fun signature(context: TypeModel?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.typeModel -> name
        context.imports.contains(this.typeModel) -> name
        else -> qualifiedName
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PrimitiveType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName
}

sealed class StructuredRuleType : TypeDefinition() {
    abstract val property: MutableMap<String, PropertyDeclaration>
    abstract fun getPropertyByIndex(i: Int): PropertyDeclaration?

    /**
     * called from PropertyDeclaration constructor
     */
    abstract fun addProperty(propertyDeclaration: PropertyDeclaration)

    /**
     * append property at the next index
     */
    fun appendProperty(name: String, type: TypeUsage) {
        PropertyDeclaration(this, name, type, property.size)
    }
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
    val properties = mutableMapOf<Int, PropertyDeclaration>()

    private val nameTypePair get() = properties.values.map { Pair(it.name, it.typeUse) }

    override fun signature(context: TypeModel?, currentDepth: Int): String {
        return when {
            currentDepth >= maxDepth -> "..."
            else -> "${name}<${this.properties.values.joinToString { it.name + ":" + it.typeUse.signature(context, currentDepth + 1) }}>"
        }
    }

    override fun getPropertyByIndex(i: Int): PropertyDeclaration? = properties[i]

    override fun addProperty(propertyDeclaration: PropertyDeclaration) {
        this.property[propertyDeclaration.name] = propertyDeclaration
        this.properties[propertyDeclaration.childIndex] = propertyDeclaration
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when {
        other !is TupleType -> false
        this.nameTypePair != other.nameTypePair -> false
        else -> true
    }

    override fun toString(): String = "Tuple<${this.properties.values.joinToString { it.name + ":" + it.typeUse }}>"
}

data class ElementType(
    val typeModel: TypeModel,
    override val name: String
) : StructuredRuleType() {

    override val qualifiedName: String get() = "${typeModel.qualifiedName}.$name"

    override var typeParameters = mutableListOf<String>()

    val supertypes: Set<ElementType> = mutableSetOf<ElementType>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: MutableList<ElementType> = mutableListOf<ElementType>()
    override val property = mutableMapOf<String, PropertyDeclaration>()
    private val _propertyIndex = mutableMapOf<Int, PropertyDeclaration>()

    override fun signature(context: TypeModel?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.typeModel -> name
        context.imports.contains(this.typeModel) -> name
        else -> qualifiedName
    }

    fun addSuperType(type: ElementType) {
        (this.supertypes as MutableSet).add(type)
        (type.subtypes as MutableList).add(this)
    }

    override fun getPropertyByIndex(i: Int): PropertyDeclaration? = _propertyIndex[i]
    override fun addProperty(propertyDeclaration: PropertyDeclaration) {
        this.property[propertyDeclaration.name] = propertyDeclaration
        this._propertyIndex[propertyDeclaration.childIndex] = propertyDeclaration
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is ElementType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName
}

data class TypeUsage(
    val type: TypeDefinition,
    val arguments: List<TypeUsage>,
    val nullable: Boolean
) {
    companion object {
        fun ofType(type: TypeDefinition, arguments: List<TypeUsage> = emptyList(), nullable: Boolean = false) = TypeUsage(type, arguments, nullable)
    }

    val notNullable get() = TypeUsage.ofType(this.type, arguments, false)

    fun signature(context: TypeModel?, currentDepth: Int): String {
        return when {
            currentDepth >= TypeDefinition.maxDepth -> "..."
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
        owner.addProperty(this)
    }

    /**
     * information about this property
     */
    var metaInfo = mutableMapOf<String, String>()

    override fun toString(): String {
        val nullable = if (typeUse.nullable) "?" else ""
        return "${owner.name}.$name: ${typeUse.type.name}$nullable [$childIndex]"
    }
}

fun TypeModel.asString(): String {
    val types = this.allTypesByName.entries.sortedBy { it.key }
        .joinToString(prefix = "  ", separator = "\n  ") { it.value.asString(this) }
    val importstr = this.imports.joinToString(prefix = "  ", separator = "\n  ") { "import ${it.qualifiedName}.*" }
    val s = """
typemodel '$namespace.$name' {
$importstr
$types
}
    """.trimIndent()
    return s
}