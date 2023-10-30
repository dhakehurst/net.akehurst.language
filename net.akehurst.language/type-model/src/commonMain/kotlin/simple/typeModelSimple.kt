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

class TypeModelSimple(
    name: String,
) : TypeModelSimpleAbstract(name) {
}

abstract class TypeModelSimpleAbstract(
    override val name: String,
) : TypeModel {

    override val AnyType: TypeDeclaration get() = SimpleTypeModelStdLib.AnyType //TODO: stdLib not necessarily part of model !
    override val NothingType: TypeDeclaration get() = SimpleTypeModelStdLib.NothingType //TODO: stdLib not necessarily part of model !

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

    override fun findOrCreateNamespace(qualifiedName: String, imports: List<String>): TypeNamespace {
        return if (namespace.containsKey(qualifiedName)) {
            namespace[qualifiedName]!!
        } else {
            val ns = TypeNamespaceSimple(qualifiedName, imports)
            addNamespace(ns)
            ns
        }
    }

    override fun findFirstByNameOrNull(typeName: String): TypeDeclaration? {
        for (ns in allNamespace) {
            val t = ns.findOwnedTypeNamed(typeName)
            if (null != t) {
                return t
            }
        }
        return null
    }

    override fun findByQualifiedNameOrNull(qualifiedName: String): TypeDeclaration? {
        return when {
            qualifiedName.contains(".").not() -> findFirstByNameOrNull(qualifiedName)
            else -> {
                val nsn = qualifiedName.substringBefore(".")
                val tn = qualifiedName.substringAfter(".")
                namespace[nsn]?.findOwnedTypeNamed(tn)
            }
        }
    }

    override fun hashCode(): Int = (this.namespace.values.toList() + this.name).hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is TypeModel -> false
        this.name != other.name -> false
        this.namespace != other.namespace -> false
        else -> true
    }

    override fun asString(): String {
        val ns = this.allNamespace
            .sortedBy { it.qualifiedName }
            .joinToString(separator = "\n") { it.asString() }
        return "typemodel '$name'\n$ns"
    }
}

abstract class TypeInstanceAbstract() : TypeInstance {

    override val resolvedProperty: Map<String, PropertyDeclaration>
        get() = type.property.values.associate {
            val rp = it.resolved(this.typeArguments)
            Pair(it.name, rp)
        }

    override fun notNullable() = this.type.instance(typeArguments, false)
    override fun nullable() = this.type.instance(typeArguments, true)

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        return when {
            currentDepth >= TypeDefinitionSimpleAbstract.maxDepth -> "..."
            else -> {
                val args = when {
                    typeArguments.isEmpty() -> ""
                    else -> "<${typeArguments.values.joinToString { it.signature(context, currentDepth + 1) }}>"
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
            else -> "<${typeArguments.values.joinToString { it.toString() }}>"
        }
        val n = when (isNullable) {
            true -> "?"
            else -> ""
        }
        return "${typeName}$args$n"
    }
}

class TypeInstanceSimple(
    val context: TypeDeclaration?,
    override val namespace: TypeNamespace,
    val qualifiedOrImportedTypeName: String,
    override val typeArguments: Map<String, TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: String
        get() = context?.typeParameters?.firstOrNull { it == qualifiedOrImportedTypeName }?.let {
            typeArguments[it]?.type?.name
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.name
        ?: qualifiedOrImportedTypeName

    override val qualifiedTypeName: String
        get() = context?.typeParameters?.firstOrNull { it == qualifiedOrImportedTypeName }?.let {
            typeArguments[it]?.type?.qualifiedName
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.qualifiedName
        ?: qualifiedOrImportedTypeName

    override val type: TypeDeclaration by lazy {
        context?.typeParameters?.firstOrNull { it == qualifiedOrImportedTypeName }?.let {
            typeArguments[it]?.type
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)
        ?: error("Cannot resolve TypeDefinition '$qualifiedOrImportedTypeName', not found in namespace '${namespace.qualifiedName}'. Is an import needed?")
    }

    override fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance = TypeInstanceSimple(
        null,
        this.namespace,
        this.qualifiedOrImportedTypeName,
        this.typeArguments.entries.associate { Pair(it.key, it.value.resolved(this.typeArguments + resolvingTypeArguments)) },
        this.isNullable
    )

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
    override val typeArguments: Map<String, TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: String get() = "TupleType"
    override val qualifiedTypeName: String get() = "TupleType"

    override fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance = TupleTypeInstance(
        this.namespace,
        this.type,
        this.typeArguments.entries.associate { Pair(it.key, it.value.resolved(this.typeArguments + resolvingTypeArguments)) },
        this.isNullable
    )

    override fun hashCode(): Int = listOf(type, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.type != other.type -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }
}

class UnnamedSupertypeTypeInstance(
    override val namespace: TypeNamespace,
    override val type: UnnamedSupertypeType,
    override val typeArguments: Map<String, TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: String get() = "UnnamedSupertypeType"
    override val qualifiedTypeName: String get() = "UnnamedSupertypeType"

    override fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance = UnnamedSupertypeTypeInstance(
        this.namespace,
        this.type,
        this.typeArguments.entries.associate { Pair(it.key, it.value.resolved(this.typeArguments + resolvingTypeArguments)) },
        this.isNullable
    )

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
    private val _unnamedSuperTypes = hashMapOf<List<TypeInstance>, UnnamedSupertypeType>()

    private var _nextTupleTypeTypeId = 0

    // qualified namespace name -> TypeNamespace
    private val _requiredNamespaces = mutableMapOf<String, TypeNamespace?>()

    abstract override val qualifiedName: String

    override val imports: List<String> = imports.toMutableList()

    override val allTypesByName = mutableMapOf<String, TypeDeclaration>()

    override val allTypes: Collection<TypeDeclaration> get() = allTypesByName.values

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

    override fun addImport(qualifiedName: String) {
        (this.imports as MutableList).add(qualifiedName)
    }

    fun addDeclaration(decl: TypeDeclaration) {
        if (allTypesByName.containsKey(decl.name)) {
            error("namespace '$qualifiedName' already contains a declaration named '${decl.name}', cannot add another")
        } else {
            when (decl) {
                is PrimitiveType -> allTypesByName[decl.name] = decl
                is EnumType -> allTypesByName[decl.name] = decl
                is UnnamedSupertypeType -> Unit
                is StructuredType -> when (decl) {
                    is TupleType -> Unit
                    is DataType -> allTypesByName[decl.name] = decl
                    is CollectionType -> allTypesByName[decl.name] = decl
                }

                else -> error("Cannot add declaration '$decl'")
            }
        }
    }

    override fun findOwnedTypeNamed(typeName: String): TypeDeclaration? = allTypesByName[typeName]

    override fun findTypeNamed(qualifiedOrImportedTypeName: String): TypeDeclaration? {
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

    override fun createUnnamedSupertypeType(subtypes: List<TypeInstance>): UnnamedSupertypeType {
        val existing = _unnamedSuperTypes[subtypes]
        return if (null == existing) {
            val t = UnnamedSupertypeTypeSimple(this, _nextUnnamedSuperTypeTypeId++, subtypes)
            _unnamedSuperTypes[subtypes] = t
            t
        } else {
            existing
        }
    }

    override fun createTupleType(): TupleType {
        return TupleTypeSimple(this, _nextTupleTypeTypeId++)
    }

    override fun createTypeInstance(context: TypeDeclaration?, qualifiedOrImportedTypeName: String, typeArguments: Map<String, TypeInstance>, isNullable: Boolean): TypeInstance {
        val ns = qualifiedOrImportedTypeName.substringBeforeLast(delimiter = ".", missingDelimiterValue = "")
        val tn = qualifiedOrImportedTypeName.substringAfterLast(".")
        when (ns) {
            "" -> Unit
            else -> this._requiredNamespaces[ns] = null
        }
        return TypeInstanceSimple(context, this, qualifiedOrImportedTypeName, typeArguments, isNullable)
    }

    override fun createUnnamedSupertypeTypeInstance(type: UnnamedSupertypeType, typeArguments: Map<String, TypeInstance>, nullable: Boolean): TypeInstance {
        return UnnamedSupertypeTypeInstance(this, type, typeArguments, nullable)
    }

    override fun createTupleTypeInstance(type: TupleType, typeArguments: Map<String, TypeInstance>, nullable: Boolean): TypeInstance {
        return TupleTypeInstance(this, type, typeArguments, nullable)
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

abstract class TypeDefinitionSimpleAbstract() : TypeDeclaration {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: String get() = "${namespace.qualifiedName}.$name"

    override val typeParameters: List<String> = mutableListOf() //make implementation mutable for serialisation

    override val property = mutableMapOf<String, PropertyDeclaration>()
    override val properties = mutableMapOf<Int, PropertyDeclaration>()

    override val method = mutableMapOf<String, MethodDeclaration>()

    /**
     * information about this type
     */
    override var metaInfo = mutableMapOf<String, String>()

    override fun instance(arguments: Map<String, TypeInstance>, nullable: Boolean): TypeInstance =
        namespace.createTypeInstance(this, this.name, arguments, nullable)

    /**
     * append a derived property, with the expression that derived it
     */
    override fun appendDerivedProperty(name: String, typeInstance: TypeInstance, expression: String) {
        val propIndex = property.size
        val prop = PropertyDeclarationDerived(this, name, typeInstance, expression, propIndex)
        this.addProperty(prop)
    }

    /**
     * append a method/function with the expression that should execute
     */
    override fun appendMethod(name: String, parameters: List<ParameterDefinition>, typeInstance: TypeInstance, body: String) {
        val meth = MethodDeclarationSimple(this, name, parameters, body)
    }

    override fun asString(context: TypeNamespace): String = signature(context, 0)

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeDeclaration -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName

    protected fun addProperty(propertyDeclaration: PropertyDeclaration) {
        this.property[propertyDeclaration.name] = propertyDeclaration
        this.properties[propertyDeclaration.index] = propertyDeclaration
    }

    protected fun addMethod(methodDeclaration: MethodDeclaration) {
        this.method[methodDeclaration.name] = methodDeclaration
    }
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

class UnnamedSupertypeTypeSimple(
    override val namespace: TypeNamespace,
    override val id: Int, // needs a number else can't implement equals without a recursive loop
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: List<TypeInstance>
) : TypeDefinitionSimpleAbstract(), UnnamedSupertypeType {

    companion object {
        val NAME = "\$UnnamedSuperTypeType"
    }

    override val name: String = NAME

    override fun instance(arguments: Map<String, TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createUnnamedSupertypeTypeInstance(this, arguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "? supertypeOf " + this.subtypes.sortedBy { it.signature(context, currentDepth + 1) }
            .joinToString(prefix = "(", postfix = ")", separator = " | ") { it.signature(context, currentDepth + 1) }
    }

    override fun asString(context: TypeNamespace): String = "unnamed ${signature(context)}"

    override fun hashCode(): Int = id
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSupertypeType -> other.id == this.id
        else -> false
    }

    override fun toString(): String = name
}

abstract class StructuredTypeSimpleAbstract : TypeDefinitionSimpleAbstract(), StructuredType {

    override fun getPropertyByIndex(i: Int): PropertyDeclaration? = properties[i]

    override fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration> =
        property.values.filter { it.characteristics.contains(chr) }

    /**
     * append property at the next index
     */
    override fun appendStoredProperty(name: String, typeInstance: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int): PropertyDeclaration {
        val propIndex = if (index >= 0) index else property.size
        val pd = PropertyDeclarationStored(this, name, typeInstance, characteristics, propIndex)
        this.addProperty(pd)
        return pd
    }

}

class TupleTypeSimple(
    override val namespace: TypeNamespace,
    val id: Int // must be public for serialisation
) : StructuredTypeSimpleAbstract(), TupleType {

    companion object {
        val NAME = "\$TupleType"
    }

    override val name: String = NAME

    override val entries get() = properties.values.map { Pair(it.name, it.typeInstance) }

    override fun instance(typeArguments: Map<String, TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createTupleTypeInstance(this, typeArguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "${name}<${this.properties.values.joinToString { it.name + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
    }

    override fun equalTo(other: TupleType): Boolean =
        this.entries == other.entries

    override fun asString(context: TypeNamespace): String = "tuple ${signature(context)}"

    override fun hashCode(): Int = this.id
    override fun equals(other: Any?): Boolean = when {
        other !is TupleTypeSimple -> false
        this.id != other.id -> false
        this.namespace != other.namespace -> false
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
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyMap(), false)
        //TODO: check if create loop of supertypes - pre namespace resolving!
        (this.supertypes as MutableList).add(ti)
        //(type.subtypes as MutableList).add(this) //TODO: can we somehow add the reverse!
    }

    override fun addSubtype(qualifiedTypeName: String) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyMap(), false)
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
                        PropertyCharacteristic.DERIVED -> "der"
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

abstract class PropertyDeclarationAbstract(

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
    override val isDerived: Boolean get() = characteristics.contains(PropertyCharacteristic.DERIVED)

    override fun resolved(typeArguments: Map<String, TypeInstance>): PropertyDeclaration = PropertyDeclarationResolved(
        this.owner,
        this.name,
        this.typeInstance.resolved(typeArguments),
        this.characteristics
    )

    override fun hashCode(): Int = listOf(owner, name).hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is PropertyDeclaration -> false
        this.name != other.name -> false
        this.owner != other.owner -> false
        else -> true
    }

    override fun toString(): String {
        val nullable = if (typeInstance.isNullable) "?" else ""
        val chrsStr = when {
            this.characteristics.isEmpty() -> ""
            else -> this.characteristics.joinToString(prefix = " {", postfix = "}")
        }
        return "${owner.name}.$name: ${typeInstance.typeName}$nullable [$index]$chrsStr"
    }
}

class PropertyDeclarationStored(
    override val owner: StructuredType,
    override val name: String,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>,
    override val index: Int // Important: indicates the child number in an SPPT, assists SimpleAST generation
) : PropertyDeclarationAbstract() {

}

class PropertyDeclarationDerived(
    override val owner: TypeDeclaration,
    override val name: String,
    override val typeInstance: TypeInstance,
    val expression: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.MEMBER, PropertyCharacteristic.DERIVED)

}

class PropertyDeclarationResolved(
    override val owner: TypeDeclaration,
    override val name: String,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>
) : PropertyDeclarationAbstract() {
    override val index: Int get() = -1 // should never be included in owners list
}

class MethodDeclarationSimple(
    override val owner: TypeDeclaration,
    override val name: String,
    override val parameters: List<ParameterDefinition>,
    override val body: String
) : MethodDeclaration {

}

class ParameterDefinitionSimple(
    override val name: String,
    override val typeInstance: TypeInstance,
    override val defaultValue: String?
) : ParameterDefinition {

    override fun hashCode(): Int = listOf(name).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PropertyDeclaration -> false
        this.name != other.name -> false
        else -> true
    }

    override fun toString(): String = "${name}: ${typeInstance}${if (null != defaultValue) " = $defaultValue" else ""}"
}