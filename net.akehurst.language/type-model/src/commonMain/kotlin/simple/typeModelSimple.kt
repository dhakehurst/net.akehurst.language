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

object SimpleTypeModelStdLib : TypeNamespaceAbstract(emptyList()) {

    override val qualifiedName: String = "std"

    //TODO: need some other kinds of type for these really
    val AnyType = super.findOrCreateSpecialTypeNamed("\$Any")
    val NothingType = super.findOrCreateSpecialTypeNamed("\$Nothing")
    //val TupleType = super.findOrCreateSpecialTypeNamed("\$Tuple")
    //val UnnamedSuperTypeType = super.findOrCreateSpecialTypeNamed("\$UnnamedSuperType")

    val String = super.findOwnedOrCreatePrimitiveTypeNamed("String").instance()
    val Boolean = super.findOwnedOrCreatePrimitiveTypeNamed("Boolean").instance()
    val Integer = super.findOwnedOrCreatePrimitiveTypeNamed("Integer").instance()
    val Real = super.findOwnedOrCreatePrimitiveTypeNamed("Real").instance()
    val Timestamp = super.findOwnedOrCreatePrimitiveTypeNamed("Timestamp").instance()

    val List = super.findOwnedOrCreateCollectionTypeNamed("List").also { (it.typeParameters as MutableList).add("E") }
    val ListSeparated = super.findOwnedOrCreateCollectionTypeNamed("ListSeparated").also { (it.typeParameters as MutableList).addAll(listOf("E", "I")) }
    val Set = super.findOwnedOrCreateCollectionTypeNamed("Set").also { (it.typeParameters as MutableList).add("E") }
    val OrderedSet = super.findOwnedOrCreateCollectionTypeNamed("OrderedSet").also { (it.typeParameters as MutableList).add("E") }
    val Map = super.findOwnedOrCreateCollectionTypeNamed("Map").also { (it.typeParameters as MutableList).addAll(listOf("K", "V")) }
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

    //store this separately to keep order of namespaces - important for lookup of types
    override val allNamespace: List<TypeNamespace> = mutableListOf<TypeNamespace>()

    override fun resolveImports() {
        allNamespace.forEach { it.resolveImports(this) }
    }

    fun addNamespace(ns: TypeNamespace) {
        if (namespace.containsKey(ns.qualifiedName)) {
            if (namespace[ns.qualifiedName] === ns) {
                //same object, no need to add it
            } else {
                error("TypeModel '${this.name}' already contains a namespace '${ns.qualifiedName}'")
            }
        } else {
            (namespace as MutableMap)[ns.qualifiedName] = ns
            (allNamespace as MutableList).add(ns)
        }
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

abstract class TypeInstanceAbstract() : TypeInstance {
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

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean

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

class TypeInstanceSimple(
    override val namespace: TypeNamespace,
    val qualifiedOrImportedTypeName: String,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val type: TypeDefinition by lazy {
        val ns = namespace ?: error("Cannot resolve TypeDefinition '$qualifiedOrImportedTypeName', namespace is not set")
        ns.findTypeNamed(qualifiedOrImportedTypeName)
            ?: error("Cannot resolve TypeDefinition '$qualifiedOrImportedTypeName', not found in namespace '${ns.qualifiedName}'. Is an import needed?")
    }

    override fun hashCode(): Int = listOf(qualifiedOrImportedTypeName, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.qualifiedOrImportedTypeName != other.qualifiedOrImportedTypeName -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }
}

class TupleTypeInstance(
    override val namespace: TypeNamespace,
    override val type: TupleType,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {
    override fun hashCode(): Int = listOf(type, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.type != other.type -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }
}

class UnnamedSuperTypeTypeInstance(
    override val namespace: TypeNamespace,
    override val type: UnnamedSuperTypeType,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {
    override fun hashCode(): Int = listOf(type, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.type != other.type -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }
}

class TypeNamespaceSimple(
    override val qualifiedName: String,
    imports: List<String>
) : TypeNamespaceAbstract(imports) {

}

abstract class TypeNamespaceAbstract(
    imports: List<String>
) : TypeNamespace {

    private var _nextUnnamedSuperTypeTypeId = 0
    private val _unnamedSuperTypes = hashMapOf<List<TypeInstance>, UnnamedSuperTypeType>()

    // qualified namespace name -> TypeNamespace
    private val _requiredNamespaces = mutableMapOf<String, TypeNamespace?>()

    abstract override val qualifiedName: String

    override val imports: List<String> = imports.toMutableList()

    override val allTypesByName = mutableMapOf<String, TypeDefinition>()

    override val allTypes: Collection<TypeDefinition> get() = allTypesByName.values

    override val primitiveType: Set<PrimitiveType> get() = allTypesByName.values.filterIsInstance<PrimitiveType>().toSet()

    override val enumType: Set<EnumType> get() = allTypesByName.values.filterIsInstance<EnumType>().toSet()

    override val collectionType: Set<CollectionType> get() = allTypesByName.values.filterIsInstance<CollectionType>().toSet()

    override val elementType: Set<DataType> get() = allTypesByName.values.filterIsInstance<DataType>().toSet()

    override fun resolveImports(model: TypeModel) {
        // check explicit imports
        this.imports.forEach {
            val ns = model.namespace[it] ?: error("import '$it' cannot be resolved in the TypeModel '${model.name}'")
            _requiredNamespaces[it] = ns
        }
        // check required namespaces
        _requiredNamespaces.keys.forEach {
            val ns = model.namespace[it] ?: error("namespace '$it' is required but cannot be resolved in the TypeModel '${model.name}'")
            _requiredNamespaces[it] = ns
        }
    }

    override fun isImported(qualifiedNamespaceName: String): Boolean = imports.contains(qualifiedNamespaceName)

    fun addImport(qualifiedName: String) {
        (this.imports as MutableList).add(qualifiedName)
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

    override fun findTypeNamed(qualifiedOrImportedTypeName: String): TypeDefinition? {
        val ns = qualifiedOrImportedTypeName.substringBeforeLast(delimiter = ".", missingDelimiterValue = "")
        val tn = qualifiedOrImportedTypeName.substringAfterLast(".")
        return when (ns) {
            "" -> {
                findOwnedTypeNamed(tn)
                    ?: imports.firstNotNullOfOrNull {
                        val tns = _requiredNamespaces[it]
                        //    ?: error("namespace '$it' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                        tns?.findOwnedTypeNamed(tn)
                    }
            }

            else -> {
                val tns = _requiredNamespaces[ns]
                    ?: error("namespace '$ns' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                tns.findOwnedTypeNamed(tn)
            }
        }
    }

    fun findOrCreateSpecialTypeNamed(typeName: String): SpecialTypeSimple {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = SpecialTypeSimple(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as SpecialTypeSimple
        }
    }

    override fun findOwnedOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = PrimitiveTypeSimple(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOwnedOrCreateCollectionTypeNamed(typeName: String): CollectionType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = CollectionTypeSimple(this, typeName)
            this.allTypesByName[typeName] = t
            t
        } else {
            existing as CollectionType
        }
    }

    override fun findOwnedOrCreateDataTypeNamed(typeName: String): DataType {
        val existing = findOwnedTypeNamed(typeName)
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

    override fun createTypeInstance(qualifiedOrImportedTypeName: String, typeArguments: List<TypeInstance>, isNullable: Boolean): TypeInstance {
        val ns = qualifiedOrImportedTypeName.substringBeforeLast(delimiter = ".", missingDelimiterValue = "")
        val tn = qualifiedOrImportedTypeName.substringAfterLast(".")
        when (ns) {
            "" -> Unit
            else -> this._requiredNamespaces[ns] = null
        }
        return TypeInstanceSimple(this, qualifiedOrImportedTypeName, typeArguments, isNullable)
    }

    override fun createTupleTypeInstance(type: TupleType, arguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return TupleTypeInstance(this, type, arguments, nullable)
    }

    override fun createUnnamedSuperTypeTypeInstance(type: UnnamedSuperTypeType, arguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return UnnamedSuperTypeTypeInstance(this, type, arguments, nullable)
    }

    override fun createTupleType(): TupleType {
        return TupleTypeSimple(this)
    }

    override fun asString(): String {
        val types = this.allTypesByName.entries.sortedBy { it.key }
            .joinToString(prefix = "  ", separator = "\n  ") { it.value.asString(this) }
        val importstr = this.imports.joinToString(prefix = "  ", separator = "\n  ") { "import ${it}.*" }
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

    override fun toString(): String = qualifiedName
}

abstract class TypeDefinitionSimpleAbstract() : TypeDefinition {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: String get() = "${namespace.qualifiedName}.$name"

    override val typeParameters: List<String> = mutableListOf() //make implementation mutable for serialisation

    /**
     * information about this type
     */
    override var metaInfo = mutableMapOf<String, String>()

    override fun instance(arguments: List<TypeInstance>, nullable: Boolean): TypeInstance =
        namespace.createTypeInstance(this.name, arguments, nullable)

    override fun asString(context: TypeNamespace): String = signature(context, 0)
}

class SpecialTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : TypeDefinitionSimpleAbstract() {
    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }


    override fun asString(context: TypeNamespace): String = "special ${signature(context)}"

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is SpecialTypeSimple -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName
}

class PrimitiveTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : TypeDefinitionSimpleAbstract(), PrimitiveType {

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }

    override fun asString(context: TypeNamespace): String = "primitive ${signature(context)}"

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
    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }

    override fun asString(context: TypeNamespace): String = "enum ${signature(context)}"

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
        val NAME = "\$UnnamedSuperTypeType"
    }

    override val name: String = NAME

    override fun instance(arguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createUnnamedSuperTypeTypeInstance(this, arguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "? supertypeOf " + this.subtypes.sortedBy { it.signature(context, currentDepth + 1) }
            .joinToString(prefix = "(", postfix = ")", separator = " | ") { it.signature(context, currentDepth + 1) }
    }

    override fun asString(context: TypeNamespace): String = "unnamed ${signature(context)}"

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
        val NAME = "\$TupleType"
    }

    constructor(namespace: TypeNamespace, init: TupleType.() -> Unit) : this(namespace) {
        this.init()
    }

    override val name: String = NAME

    override val entries get() = properties.values.map { Pair(it.name, it.typeInstance) }

    override fun instance(arguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createTupleTypeInstance(this, arguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "${name}<${this.properties.values.joinToString { it.name + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
    }

    override fun asString(context: TypeNamespace): String = "tuple ${signature(context)}"

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

    override val supertypes: List<TypeInstance> = mutableListOf()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    override val allSuperTypes: List<TypeInstance>
        get() = supertypes + supertypes.flatMap { (it.type as DataType).allSuperTypes }

    override val allProperty: Map<String, PropertyDeclaration>
        get() = supertypes.flatMap {
            (it.type as DataType).allProperty.values
        }.associateBy { it.name } + this.property

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }

    override fun addSupertype(qualifiedTypeName: String) {
        val ti = namespace.createTypeInstance(qualifiedTypeName, emptyList(), false)
        //TODO: check if create loop of supertypes - pre namespace resolving!
        (this.supertypes as MutableList).add(ti)
        //(type.subtypes as MutableList).add(this) //TODO: can we somehow add the reverse!
    }

    override fun addSubtype(qualifiedTypeName: String) {
        val ti = namespace.createTypeInstance(qualifiedTypeName, emptyList(), false)
        (this.subtypes as MutableList).add(ti) //TODO: can we somehow add the reverse!
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

    override val supertypes: Set<CollectionType> = mutableSetOf<CollectionType>()

    override val isArray: Boolean get() = name == "Array"
    override val isList: Boolean get() = name == "List"
    override val isSet: Boolean get() = name == "Set"
    override val isMap: Boolean get() = name == "Map"

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }

    override fun asString(context: TypeNamespace): String = "collection ${signature(context)}"
}

class PropertyDeclarationSimple(
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
