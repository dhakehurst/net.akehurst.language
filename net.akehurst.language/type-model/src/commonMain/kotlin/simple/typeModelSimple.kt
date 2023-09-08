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

package net.akehurst.language.typemodel.simple

import net.akehurst.language.typemodel.api.*

object SimpleTypeModelStdLib : TypeNamespaceAbstract("std", emptyList()) {
    val AnyType: TypeDefinition = object : TypeDefinitionSimpleAbstract() {
        override val namespace: TypeNamespace get() = this@SimpleTypeModelStdLib
        override val name: String = "\$Any"
        override fun signature(context: TypeNamespace?, currentDepth: Int): String = name

        //TODO: fun instance(arguments: List<TypeInstance> = emptyList(), nullable: Boolean = false): TypeInstance = TypeInstance.ofType(this, arguments, nullable)
        override fun hashCode(): Int = name.hashCode()
        override fun equals(other: Any?): Boolean = this === other
        override fun toString(): String = name
    }
    val NothingType: TypeDefinition = object : TypeDefinitionSimpleAbstract() {
        override val namespace: TypeNamespace get() = this@SimpleTypeModelStdLib
        override val name: String = "\$Nothing"
        override fun signature(context: TypeNamespace?, currentDepth: Int): String = name
        override fun hashCode(): Int = name.hashCode()
        override fun equals(other: Any?): Boolean = this === other
        override fun toString(): String = name
    }

    val String = super.findOrCreatePrimitiveTypeNamed("String").instance()
    val Boolean = super.findOrCreatePrimitiveTypeNamed("Boolean").instance()
    val Integer = super.findOrCreatePrimitiveTypeNamed("Integer").instance()
    val Real = super.findOrCreatePrimitiveTypeNamed("Real").instance()
    val Timestamp = super.findOrCreatePrimitiveTypeNamed("Timestamp").instance()

    val List = super.findOrCreateCollectionTypeNamed("List").also { (it.typeParameters as MutableList).add("E") }
    val ListSeparated = super.findOrCreateCollectionTypeNamed("ListSeparated").also { (it.typeParameters as MutableList).addAll(listOf("E", "I")) }
    val Set = super.findOrCreateCollectionTypeNamed("Set").also { (it.typeParameters as MutableList).add("E") }
    val OrderedSet = super.findOrCreateCollectionTypeNamed("OrderedSet").also { (it.typeParameters as MutableList).add("E") }
    val Map = super.findOrCreateCollectionTypeNamed("Map").also { (it.typeParameters as MutableList).addAll(listOf("K", "V")) }
}

class TypeModelSimple(
    name: String,
) : TypeModelSimpleAbstract(name) {
}

abstract class TypeModelSimpleAbstract(
    override val name: String,
) : TypeModel {

    override val AnyType: TypeDefinition get() = SimpleTypeModelStdLib.AnyType //TODO: stdLib not necessarily part of model !
    override val NothingType: TypeDefinition get() = SimpleTypeModelStdLib.NothingType //TODO: stdLib not necessarily part of model !

    override val namespace: Map<String, TypeNamespace> = mutableMapOf<String, TypeNamespace>()

    override val allNamespace: List<TypeNamespace> = mutableListOf<TypeNamespace>()

    override fun resolveImports() {
        allNamespace.forEach { it.resolveImports(this) }
    }

    fun addNamespace(ns: TypeNamespace) {
        (namespace as MutableMap)[ns.qualifiedName] = ns
        (allNamespace as MutableList).add(ns)
    }

    override fun findFirstByNameOrNull(typeName: String): TypeDefinition? {
        for (ns in allNamespace) {
            val t = ns.findOwnedTypeNamed(typeName)
            if (null != t) {
                return t
            }
        }
        return null
    }

    override fun findByQualifiedNameOrNull(qualifiedName: String): TypeDefinition? {
        return when {
            qualifiedName.contains(".").not() -> findFirstByNameOrNull(qualifiedName)
            else -> {
                val nsn = qualifiedName.substringBefore(".")
                val tn = qualifiedName.substringAfter(".")
                namespace[nsn]?.findOwnedTypeNamed(tn)
            }
        }
    }

    override fun asString(): String {
        val ns = this.allNamespace
            .sortedBy { it.qualifiedName }
            .joinToString(separator = "\n") { it.asString() }
        return "typemodel '$name'\n$ns"
    }
}

class TypeInstanceSimple(
    var namespace: TypeNamespace?,
    optionalType: TypeDefinition?,
    optionalName: String?,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstance {

    override val type: TypeDefinition by lazy {
        optionalType
            ?: optionalName?.let {
                val ns = namespace ?: error("Cannot resolve TypeDefinition '$optionalName', namespace is not set")
                ns.findTypeNamed(optionalName) ?: error("Cannot resolve TypeDefinition '$optionalName', not found in namespace '${ns.qualifiedName}'. Is an import needed?")
            }
            ?: error("TypeDefinition '$optionalName' not found in context of namespace '${namespace?.qualifiedName}'")
    }

    override fun notNullable() = this.type.instance(typeArguments, false)
    override fun nullable() = this.type.instance(typeArguments, true)

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        return when {
            currentDepth >= TypeDefinitionSimpleAbstract.maxDepth -> "..."
            else -> {
                val args = when {
                    typeArguments.isEmpty() -> ""
                    else -> "<${typeArguments.joinToString { it.signature(context, currentDepth + 1) }}>"
                }
                val n = when (isNullable) {
                    true -> "?"
                    else -> ""
                }
                val name = when {
                    typeArguments.isEmpty() -> type.signature(context, currentDepth + 1)
                    else -> type.name
                }
                return "${name}$args$n"
            }
        }
    }

    override fun toString(): String {
        val args = when {
            typeArguments.isEmpty() -> ""
            else -> "<${typeArguments.joinToString { it.toString() }}>"
        }
        val n = when (isNullable) {
            true -> "?"
            else -> ""
        }
        return "${type.name}$args$n"
    }
}

class TypeNamespaceSimple(
    qualifiedName: String,
    imports: List<String>
) : TypeNamespaceAbstract(qualifiedName, imports) {

}

abstract class TypeNamespaceAbstract(
    override val qualifiedName: String,
    private var importsStr: List<String>
) : TypeNamespace {

    private var _nextUnnamedSuperTypeTypeId = 0
    private val _unnamedSuperTypes = hashMapOf<List<TypeInstance>, UnnamedSuperTypeType>()

    private var _imports: List<TypeNamespace>? = null

    //private var _nextTupleTypeId = 0
    //private val _unnamedTupleTypes = hashMapOf<List<TypeInstance>, TupleType>()

    override val imports get() = _imports ?: emptyList()// error("imported namespaces need to be resolved with a call to resolveImports() on the TypeModel")

    override val allTypesByName = mutableMapOf<String, TypeDefinition>()

    override val allTypes: Collection<TypeDefinition> get() = allTypesByName.values

    override val primitiveType: Set<PrimitiveType> get() = allTypesByName.values.filterIsInstance<PrimitiveType>().toSet()

    override val enumType: Set<EnumType> get() = allTypesByName.values.filterIsInstance<EnumType>().toSet()

    override val collectionType: Set<CollectionType> get() = allTypesByName.values.filterIsInstance<CollectionType>().toSet()

    override val elementType: Set<DataType> get() = allTypesByName.values.filterIsInstance<DataType>().toSet()

    override fun resolveImports(model: TypeModel) {
        this._imports = this.importsStr.map { model.namespace[it] ?: error("import $it cannot be resolved in type model '${model.name}'") }
    }

    fun addImport(qualifiedName: String) {
        this.importsStr += qualifiedName
    }

    fun addDeclaration(decl: TypeDefinition) {
        if (allTypesByName.containsKey(decl.name)) {
            error("namespace '$qualifiedName' already contains a declaration named '${decl.name}', cannot add another")
        } else {
            when (decl) {
                is PrimitiveType -> allTypesByName[decl.name] = decl
                is EnumType -> allTypesByName[decl.name] = decl
                is UnnamedSuperTypeType -> Unit
                is StructuredType -> when (decl) {
                    is TupleType -> Unit
                    is DataType -> allTypesByName[decl.name] = decl
                    is CollectionType -> allTypesByName[decl.name] = decl
                }

                else -> error("Cannot add declaration '$decl'")
            }
        }
    }

    override fun findOwnedTypeNamed(typeName: String): TypeDefinition? = allTypesByName[typeName]

    override fun findTypeNamed(typeName: String): TypeDefinition? = findOwnedTypeNamed(typeName) ?: imports.firstNotNullOfOrNull { it.findTypeNamed(typeName) }

    override fun findOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType {
        val existing = findTypeNamed(typeName) ?: imports.firstNotNullOfOrNull { it.findOrCreatePrimitiveTypeNamed(typeName) }
        return if (null == existing) {
            val t = PrimitiveTypeSimple(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOrCreateCollectionTypeNamed(typeName: String): CollectionType {
        val existing = findTypeNamed(typeName) ?: imports.firstNotNullOfOrNull { it.findOrCreateCollectionTypeNamed(typeName) }
        return if (null == existing) {
            val t = CollectionTypeSimple(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as CollectionType
        }
    }

    override fun findOrCreateDataTypeNamed(typeName: String): DataType {
        val existing = findTypeNamed(typeName) //?: imports.firstNotNullOfOrNull { it.findOrCreatePrimitiveTypeNamed(typeName) }
        return if (null == existing) {
            val t = DataTypeSimple(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as DataType
        }
    }

    override fun createUnnamedSuperTypeType(subtypes: List<TypeInstance>): UnnamedSuperTypeType {
        val existing = _unnamedSuperTypes[subtypes]
        return if (null == existing) {
            val t = UnnamedSuperTypeTypeSimple(this, _nextUnnamedSuperTypeTypeId++, subtypes)
            _unnamedSuperTypes[subtypes] = t
            t
        } else {
            existing
        }
    }

    override fun asString(): String {
        val types = this.allTypesByName.entries.sortedBy { it.key }
            .joinToString(prefix = "  ", separator = "\n  ") { it.value.asString(this) }
        val importstr = this.imports.joinToString(prefix = "  ", separator = "\n  ") { "import ${it.qualifiedName}.*" }
        val s = """
namespace '$qualifiedName' {
$importstr
$types
}
    """.trimIndent()
        return s
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeNamespace -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = "$qualifiedName"
}

abstract class TypeDefinitionSimpleAbstract() : TypeDefinition {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: String get() = "${namespace.qualifiedName}.$name"

    override val typeParameters: List<String> = emptyList()

    /**
     * information about this type
     */
    override var metaInfo = mutableMapOf<String, String>()

    override fun instance(arguments: List<TypeInstance>, nullable: Boolean): TypeInstance = TypeInstanceSimple(namespace, this, null, arguments, nullable)

    override fun asString(context: TypeNamespace): String = signature(context, 0)
}

class PrimitiveTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : TypeDefinitionSimpleAbstract(), PrimitiveType {

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        val core = when {
            null == context -> qualifiedName
            context == this.namespace -> name
            context.imports.contains(this.namespace) -> name
            else -> qualifiedName
        }
        return "primitive $core"
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PrimitiveType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName
}

class EnumTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String,
    override val literals: List<String>
) : TypeDefinitionSimpleAbstract(), EnumType {
    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        val core = when {
            null == context -> qualifiedName
            context == this.namespace -> name
            context.imports.contains(this.namespace) -> name
            else -> qualifiedName
        }
        return "enum $core"
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PrimitiveType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName
}

class UnnamedSuperTypeTypeSimple(
    override val namespace: TypeNamespace,
    override val id: Int, // needs a number else can't implement equals without a recursive loop
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: List<TypeInstance>
) : TypeDefinitionSimpleAbstract(), UnnamedSuperTypeType {
    companion object {
        const val INSTANCE_NAME = "UnnamedSuperType"
    }


    override val name: String = INSTANCE_NAME
    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        val core = when {
            currentDepth >= maxDepth -> "..."
            else -> "? supertypeOf " + this.subtypes.sortedBy { it.signature(context, currentDepth + 1) }
                .joinToString(prefix = "(", postfix = ")", separator = " | ") { it.signature(context, currentDepth + 1) }
        }
        return "unnamed $core"
    }

    override fun hashCode(): Int = id
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSuperTypeType -> other.id == this.id
        else -> false
    }

    override fun toString(): String = name
}

abstract class StructuredTypeSimpleAbstract : TypeDefinitionSimpleAbstract(), StructuredType {

    override val property = mutableMapOf<String, PropertyDeclaration>()
    override val properties = mutableMapOf<Int, PropertyDeclaration>()

    override fun getPropertyByIndex(i: Int): PropertyDeclaration? = properties[i]

    override fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration> =
        property.values.filter { it.characteristics.contains(chr) }

    /**
     * called from PropertyDeclaration constructor
     */
    private fun addProperty(propertyDeclaration: PropertyDeclaration) {
        this.property[propertyDeclaration.name] = propertyDeclaration
        this.properties[propertyDeclaration.index] = propertyDeclaration
    }

    /**
     * append property at the next index
     */
    override fun appendProperty(name: String, type: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int): PropertyDeclaration {
        val childIndex = if (index >= 0) index else property.size
        val pd = PropertyDeclarationSimple(this, name, type, characteristics, childIndex)
        this.addProperty(pd)
        return pd
    }
}

class TupleTypeSimple(
    override val namespace: TypeNamespace,
) : StructuredTypeSimpleAbstract(), TupleType {
    companion object {
        const val INSTANCE_NAME = "\$Tuple"
    }

    constructor(namespace: TypeNamespace, init: TupleType.() -> Unit) : this(namespace) {
        this.init()
    }

    override val name: String = INSTANCE_NAME

    override val entries get() = properties.values.map { Pair(it.name, it.typeInstance) }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        val core = when {
            currentDepth >= maxDepth -> "..."
            else -> "${name}<${this.properties.values.joinToString { it.name + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
        }
        return "tuple $core"
    }

    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = when {
        other !is TupleType -> false
        this.entries != other.entries -> false
        else -> true
    }

    override fun toString(): String = "Tuple<${this.properties.values.joinToString { it.name + ":" + it.typeInstance }}>"
}

class DataTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : StructuredTypeSimpleAbstract(), DataType {

    override var typeParameters = mutableListOf<String>()

    override val supertypes: List<DataType> = mutableListOf<DataType>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<DataType> = mutableListOf<DataType>()

    override val allSuperTypes: List<DataType>
        get() = supertypes + supertypes.flatMap { it.supertypes }

    override val allProperty: Map<String, PropertyDeclaration>
        get() = supertypes.flatMap {
            it.allProperty.values
        }.associateBy { it.name } + this.property

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        val core = when {
            null == context -> qualifiedName
            context == this.namespace -> name
            context.imports.contains(this.namespace) -> name
            else -> qualifiedName
        }
        return "datatype $core"
    }

    override fun addSuperType(type: DataType) {
        (this.supertypes as MutableList).add(type)
        (type.subtypes as MutableList).add(this)
    }

    override fun asString(context: TypeNamespace): String {
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val props = this.property.values.sortedBy { it.index }
            .joinToString(separator = "\n    ") {
                val psig = it.typeInstance.signature(context, 0)
                val chrs = it.characteristics.joinToString(prefix = "{", postfix = "}") {
                    when (it) {
                        PropertyCharacteristic.IDENTITY -> "val"
                        PropertyCharacteristic.REFERENCE -> "ref"
                        PropertyCharacteristic.COMPOSITE -> "cmp"
                        PropertyCharacteristic.CONSTRUCTOR -> "cns"
                        PropertyCharacteristic.MEMBER -> "var"
                    }
                }
                "${it.name}:$psig $chrs"
            }
        return "datatype ${name}${sups} {\n    $props\n  }"
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is DataType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName
}

class CollectionTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String,
    override var typeParameters: List<String> = mutableListOf<String>()
) : StructuredTypeSimpleAbstract(), CollectionType {

    override val supertypes: Set<DataType> = mutableSetOf<DataType>()

    override val isArray: Boolean get() = name == "Array"
    override val isList: Boolean get() = name == "List"
    override val isSet: Boolean get() = name == "Set"
    override val isMap: Boolean get() = name == "Map"

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        val core = when {
            null == context -> qualifiedName
            context == this.namespace -> name
            context.imports.contains(this.namespace) -> name
            else -> qualifiedName
        }
        return "collection $core"
    }

}

private class PropertyDeclarationSimple(
    override val owner: StructuredType,
    override val name: String,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>,
    override val index: Int // Important: indicates the child number in an SPPT, assists SimpleAST generation
) : PropertyDeclaration {

    /**
     * information about this property
     */
    override var metaInfo = mutableMapOf<String, String>()

    override val isComposite: Boolean get() = characteristics.contains(PropertyCharacteristic.COMPOSITE)
    override val isReference: Boolean get() = characteristics.contains(PropertyCharacteristic.REFERENCE)
    override val isConstructor: Boolean get() = characteristics.contains(PropertyCharacteristic.CONSTRUCTOR)
    override val isIdentity: Boolean get() = characteristics.contains(PropertyCharacteristic.IDENTITY)
    override val isMember: Boolean get() = characteristics.contains(PropertyCharacteristic.MEMBER)

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun toString(): String {
        val nullable = if (typeInstance.isNullable) "?" else ""
        val chrsStr = when {
            this.characteristics.isEmpty() -> ""
            else -> this.characteristics.joinToString(prefix = " {", postfix = "}")
        }
        return "${owner.name}.$name: ${typeInstance.type.name}$nullable [$index]$chrsStr"
    }
}
