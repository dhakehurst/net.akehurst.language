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

import net.akehurst.language.collections.indexOfOrNull
import net.akehurst.language.typemodel.api.*

class TypeModelSimple(
    name: String,
) : TypeModelSimpleAbstract(name) {
}

abstract class TypeModelSimpleAbstract(
    override val name: String,
) : TypeModel {

    override val AnyType: TypeDeclaration get() = SimpleTypeModelStdLib.AnyType.declaration //TODO: stdLib not necessarily part of model !
    override val NothingType: TypeDeclaration get() = SimpleTypeModelStdLib.NothingType.declaration //TODO: stdLib not necessarily part of model !

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
                val nsn = qualifiedName.substringBeforeLast(".")
                val tn = qualifiedName.substringAfterLast(".")
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

    abstract val typeOrNull: TypeDeclaration?

    override val resolvedProperty: Map<String, PropertyDeclaration>
        get() {
            val typeArgMap = createTypeArgMap()
            return declaration.allProperty.values.associate {
                val rp = it.resolved(typeArgMap)
                Pair(it.name, rp)
            }
        }

    override fun notNullable() = this.declaration.type(typeArguments, false)
    override fun nullable() = this.declaration.type(typeArguments, true)

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        return when {
            currentDepth >= TypeDeclarationSimpleAbstract.maxDepth -> "..."
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
                    typeArguments.isEmpty() -> declaration.signature(context, currentDepth + 1)
                    else -> declaration.name
                }
                return "${name}$args$n"
            }
        }
    }

    override fun conformsTo(other: TypeInstance): Boolean = when {
        other === this -> true // fast option
        this == SimpleTypeModelStdLib.NothingType -> false
        other == SimpleTypeModelStdLib.NothingType -> false
        other == SimpleTypeModelStdLib.AnyType -> true
        this.declaration.conformsTo(other.declaration).not() -> false
        this.typeArguments.size != other.typeArguments.size -> false
        else -> {
            var result = true
            for (i in this.typeArguments.indices) {
                if (this.typeArguments[i].conformsTo(other.typeArguments[i])) {
                    continue
                } else {
                    result = false
                    break
                }
            }
            result
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
        return "${typeName}$args$n"
    }

    protected fun createTypeArgMap(): Map<String, TypeInstance> {
        val typeArgMap = mutableMapOf<String, TypeInstance>()
        typeOrNull?.typeParameters?.forEachIndexed { index, it ->
            val tp = it
            val ta = this.typeArguments[index]
            typeArgMap[tp] = ta
        }
        return typeArgMap
    }
}

class TypeInstanceSimple(
    val contextQualifiedTypeName: String?,
    override val namespace: TypeNamespace,
    val qualifiedOrImportedTypeName: String,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    val context: TypeDeclaration? get() = contextQualifiedTypeName?.let { namespace.findTypeNamed(it) }

    override val typeName: String
        get() = context?.typeParameters?.indexOfOrNull(qualifiedOrImportedTypeName)?.let {
            typeArguments.getOrNull(it)?.declaration?.name
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.name
        ?: qualifiedOrImportedTypeName

    override val qualifiedTypeName: String
        get() = context?.typeParameters?.indexOfOrNull(qualifiedOrImportedTypeName)?.let {
            typeArguments.getOrNull(it)?.declaration?.qualifiedName
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.qualifiedName
        ?: qualifiedOrImportedTypeName

    override val typeOrNull: TypeDeclaration? by lazy {
        context?.typeParameters?.indexOfOrNull(qualifiedOrImportedTypeName)?.let {
            typeArguments.getOrNull(it)?.declaration
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)
    }

    override val declaration: TypeDeclaration
        get() = typeOrNull
            ?: error("Cannot resolve TypeDefinition '$qualifiedOrImportedTypeName', not found in namespace '${namespace.qualifiedName}'. Is an import needed?")


    override fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance {
        val selfResolved = resolvingTypeArguments[this.qualifiedOrImportedTypeName]
        return when {
            selfResolved != null -> selfResolved
            else -> {
                val thisTypeArgMap = createTypeArgMap()
                val resolvedTypeArgs = this.typeArguments.map {
                    it.resolved(thisTypeArgMap + resolvingTypeArguments)
                }
                TypeInstanceSimple(null, this.namespace, this.qualifiedOrImportedTypeName, resolvedTypeArgs, this.isNullable)
            }
        }
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
    override val declaration: TupleType,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: String get() = "TupleType"
    override val qualifiedTypeName: String get() = "TupleType"
    override val typeOrNull: TypeDeclaration get() = declaration

    override fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance {
        val thisTypeArgMap = createTypeArgMap()
        val resolvedTypeArgs = this.typeArguments.map {
            it.resolved(thisTypeArgMap + resolvingTypeArguments)
        }
        return TupleTypeInstance(this.namespace, this.declaration, resolvedTypeArgs, this.isNullable)
    }

    override fun hashCode(): Int = listOf(declaration, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.declaration != other.declaration -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }
}

class UnnamedSupertypeTypeInstance(
    override val namespace: TypeNamespace,
    override val declaration: UnnamedSupertypeType,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: String get() = "UnnamedSupertypeType"
    override val qualifiedTypeName: String get() = "UnnamedSupertypeType"

    override val typeOrNull: TypeDeclaration get() = declaration

    override fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance {
        val thisTypeArgMap = createTypeArgMap()
        val resolvedTypeArgs = this.typeArguments.map {
            it.resolved(thisTypeArgMap + resolvingTypeArguments)
        }
        return UnnamedSupertypeTypeInstance(this.namespace, this.declaration, resolvedTypeArgs, this.isNullable)
    }

    override fun hashCode(): Int = listOf(declaration, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.declaration != other.declaration -> false
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

    override val ownedTypesByName = mutableMapOf<String, TypeDeclaration>()

    override val ownedTypes: Collection<TypeDeclaration> get() = ownedTypesByName.values

    val ownedUnnamedSupertypeType = mutableListOf<UnnamedSupertypeType>()

    val ownedTupleTypes = mutableListOf<TupleType>()

    override val primitiveType: Set<PrimitiveType> get() = ownedTypesByName.values.filterIsInstance<PrimitiveType>().toSet()

    override val enumType: Set<EnumType> get() = ownedTypesByName.values.filterIsInstance<EnumType>().toSet()

    override val collectionType: Set<CollectionType> get() = ownedTypesByName.values.filterIsInstance<CollectionType>().toSet()

    override val elementType: Set<DataType> get() = ownedTypesByName.values.filterIsInstance<DataType>().toSet()

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
        if (ownedTypesByName.containsKey(decl.name)) {
            error("namespace '$qualifiedName' already contains a declaration named '${decl.name}', cannot add another")
        } else {
            when (decl) {
                is PrimitiveType -> ownedTypesByName[decl.name] = decl
                is EnumType -> ownedTypesByName[decl.name] = decl
                is UnnamedSupertypeType -> Unit
                is StructuredType -> when (decl) {
                    is TupleType -> Unit
                    is DataType -> ownedTypesByName[decl.name] = decl
                    is CollectionType -> ownedTypesByName[decl.name] = decl
                }

                else -> error("Cannot add declaration '$decl'")
            }
        }
    }

    override fun findOwnedTypeNamed(typeName: String): TypeDeclaration? = ownedTypesByName[typeName]

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

            this.qualifiedName -> findOwnedTypeNamed(tn)
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
            this.ownedTypesByName[typeName] = t
            t
        } else {
            existing as SpecialTypeSimple
        }
    }

    override fun findOwnedOrCreateSingletonTypeNamed(typeName: String): SingletonType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = SingletonTypeSimple(this, typeName)
            this.ownedTypesByName[typeName] = t
            t
        } else {
            existing as SingletonType
        }
    }

    override fun findOwnedOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = PrimitiveTypeSimple(this, typeName)
            this.ownedTypesByName[typeName] = t
            t
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOwnedOrCreateEnumTypeNamed(typeName: String, literals: List<String>): EnumType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = EnumTypeSimple(this, typeName, literals)
            this.ownedTypesByName[typeName] = t
            t
        } else {
            existing as EnumType
        }
    }

    override fun findOwnedOrCreateCollectionTypeNamed(typeName: String): CollectionType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = CollectionTypeSimple(this, typeName)
            this.ownedTypesByName[typeName] = t
            t
        } else {
            existing as CollectionType
        }
    }

    override fun findOwnedOrCreateDataTypeNamed(typeName: String): DataType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = DataTypeSimple(this, typeName)
            this.ownedTypesByName[typeName] = t
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
            ownedUnnamedSupertypeType.add(t)
            t
        } else {
            existing
        }
    }

    override fun createTupleType(): TupleType {
        val td = TupleTypeSimple(this, _nextTupleTypeTypeId++)
        ownedTupleTypes.add(td)
        return td
    }

    override fun createTypeInstance(context: TypeDeclaration?, qualifiedOrImportedTypeName: String, typeArguments: List<TypeInstance>, isNullable: Boolean): TypeInstance {
        val ns = qualifiedOrImportedTypeName.substringBeforeLast(delimiter = ".", missingDelimiterValue = "")
        val tn = qualifiedOrImportedTypeName.substringAfterLast(".")
        when (ns) {
            "" -> Unit
            else -> this._requiredNamespaces[ns] = null
        }
        return TypeInstanceSimple(context?.qualifiedName, this, qualifiedOrImportedTypeName, typeArguments, isNullable)
    }

    override fun createUnnamedSupertypeTypeInstance(declaration: UnnamedSupertypeType, typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return UnnamedSupertypeTypeInstance(this, declaration, typeArguments, nullable)
    }

    override fun createTupleTypeInstance(declaration: TupleType, typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return TupleTypeInstance(this, declaration, typeArguments, nullable)
    }

    override fun asString(): String {
        val types = this.ownedTypesByName.entries.sortedBy { it.key }
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

abstract class TypeDeclarationSimpleAbstract() : TypeDeclaration {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: String get() = "${namespace.qualifiedName}.$name"

    override val supertypes: List<TypeInstance> = mutableListOf()

    override val typeParameters: List<String> = mutableListOf() //make implementation mutable for serialisation

    // store properties by map(index) rather than list(index), because when constructing from grammar, not every index is used
    // public, so it can be serialised
    val propertyByIndex = mutableMapOf<Int, PropertyDeclaration>()
    override val property get() = propertyByIndex.values.toList() //should be in order because mutableMap is LinkedHashMap by default
    //protected val properties = mutableListOf<PropertyDeclaration>()

    override val method = mutableListOf<MethodDeclaration>()

    override val allSuperTypes: List<TypeInstance> get() = supertypes + supertypes.flatMap { (it.declaration as DataType).allSuperTypes }

    override val allProperty: Map<String, PropertyDeclaration> get() = property.associateBy { it.name }

    val allMethod: Map<String, MethodDeclaration> get() = method.associateBy { it.name }

    /**
     * information about this type
     */
    override var metaInfo = mutableMapOf<String, String>()

    override fun type(arguments: List<TypeInstance>, nullable: Boolean): TypeInstance =
        namespace.createTypeInstance(this, this.name, arguments, nullable)

    override fun conformsTo(other: TypeDeclaration): Boolean = when {
        other === this -> true // fast option
        this == SimpleTypeModelStdLib.NothingType.declaration -> false
        other == SimpleTypeModelStdLib.NothingType.declaration -> false
        other == SimpleTypeModelStdLib.AnyType.declaration -> true
        other == this -> true
        other is UnnamedSupertypeType -> other.subtypes.any { this.conformsTo(it.declaration) }
        else -> this.supertypes.any { it.declaration.conformsTo(other) }
    }

    override fun getPropertyByIndexOrNull(i: Int): PropertyDeclaration? = propertyByIndex[i]

    override fun findPropertyOrNull(name: String): PropertyDeclaration? =
        this.allProperty[name]

    override fun findMethodOrNull(name: String): MethodDeclaration? =
        this.allMethod[name]

    // --- mutable ---
    override fun addSupertype(qualifiedTypeName: String) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        //TODO: check if create loop of supertypes - pre namespace resolving!
        (this.supertypes as MutableList).add(ti)
        //(type.subtypes as MutableList).add(this) //TODO: can we somehow add the reverse!
    }

    /**
     * append a derived property, with the expression that derived it
     */
    override fun appendPropertyDerived(name: String, typeInstance: TypeInstance, description: String, expression: String) {
        val propIndex = property.size
        val prop = PropertyDeclarationDerived(this, name, typeInstance, description, expression, propIndex)
        this.addProperty(prop)
    }

    override fun appendPropertyPrimitive(name: String, typeInstance: TypeInstance, description: String) {
        val propIndex = property.size
        val prop = PropertyDeclarationPrimitive(this, name, typeInstance, description, propIndex)
        this.addProperty(prop)
    }

    override fun appendMethodPrimitive(
        name: String,
        parameters: List<ParameterDeclaration>,
        typeInstance: TypeInstance,
        description: String,
        body: (self: Any, arguments: List<Any>) -> Any
    ) {
        val method = MethodDeclarationPrimitive(this, name, parameters, description, body)
    }

    /**
     * append a method/function with the expression that should execute
     */
    override fun appendMethodDerived(name: String, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String) {
        val meth = MethodDeclarationDerived(this, name, parameters, description, body)
    }

    override fun asString(context: TypeNamespace): String = signature(context, 0)

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeDeclaration -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName

    protected fun addProperty(propertyDeclaration: PropertyDeclaration) {
        this.propertyByIndex[propertyDeclaration.index] = propertyDeclaration
        //this.property[propertyDeclaration.name] = propertyDeclaration
    }

    protected fun addMethod(methodDeclaration: MethodDeclaration) {
        this.method.add(methodDeclaration)
        //this.method[methodDeclaration.name] = methodDeclaration
    }
}

class SpecialTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : TypeDeclarationSimpleAbstract() {
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

class SingletonTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : TypeDeclarationSimpleAbstract(), SingletonType {
    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }

    override fun asString(context: TypeNamespace): String = "singleton ${signature(context)}"

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
) : TypeDeclarationSimpleAbstract(), PrimitiveType {

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
) : TypeDeclarationSimpleAbstract(), EnumType {
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
) : TypeDeclarationSimpleAbstract(), UnnamedSupertypeType {

    companion object {
        val NAME = "\$UnnamedSuperTypeType"
    }

    override val name: String = NAME

    override fun type(arguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createUnnamedSupertypeTypeInstance(this, arguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "? supertypeOf " + this.subtypes.sortedBy { it.signature(context, currentDepth + 1) }
            .joinToString(prefix = "(", postfix = ")", separator = " | ") { it.signature(context, currentDepth + 1) }
    }

    override fun conformsTo(other: TypeDeclaration): Boolean = when {
        this === other -> true
        other == SimpleTypeModelStdLib.NothingType.declaration -> false
        other == SimpleTypeModelStdLib.AnyType.declaration -> true
        other is UnnamedSupertypeType -> this.subtypes == other.subtypes
        else -> false
    }

    override fun asString(context: TypeNamespace): String = "unnamed ${signature(context)}"

    override fun hashCode(): Int = id
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSupertypeType -> other.id == this.id
        else -> false
    }

    override fun toString(): String = name
}

abstract class StructuredTypeSimpleAbstract : TypeDeclarationSimpleAbstract(), StructuredType {

    override fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration> =
        property.filter { it.characteristics.contains(chr) }

    /**
     * append property at the next index
     */
    override fun appendPropertyStored(name: String, typeInstance: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int): PropertyDeclaration {
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

    override val entries get() = property.map { Pair(it.name, it.typeInstance) }

    override fun type(typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createTupleTypeInstance(this, typeArguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "${name}<${this.property.joinToString { it.name + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
    }

    override fun conformsTo(other: TypeDeclaration): Boolean = when {
        this === other -> true
        other == SimpleTypeModelStdLib.NothingType.declaration -> false
        other == SimpleTypeModelStdLib.AnyType.declaration -> true
        other is TupleType -> other.entries.containsAll(this.entries) //TODO: this should check conformance of property types! - could cause recursive loop!
        else -> false
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

    override fun toString(): String = "Tuple<${this.property.joinToString { it.name + ":" + it.typeInstance }}>"
}

class DataTypeSimple(
    override val namespace: TypeNamespace,
    override val name: String
) : StructuredTypeSimpleAbstract(), DataType {

    override var typeParameters = mutableListOf<String>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    override val allProperty: Map<String, PropertyDeclaration>
        get() = supertypes.flatMap {
            (it.declaration as DataType).allProperty.values
        }.associateBy { it.name } + this.property.associateBy { it.name }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName
        context == this.namespace -> name
        context.isImported(this.namespace.qualifiedName) -> name
        else -> qualifiedName
    }

    override fun addSubtype(qualifiedTypeName: String) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        (this.subtypes as MutableList).add(ti) //TODO: can we somehow add the reverse!
    }

    override fun asString(context: TypeNamespace): String {
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val props = this.property
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
                        PropertyCharacteristic.PRIMITIVE -> "prm"
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

//    override val isArray: Boolean get() = name == "Array"
//    override val isList: Boolean get() = name == "List"
//    override val isSet: Boolean get() = name == "Set"
//    override val isMap: Boolean get() = name == "Map"

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
        this.characteristics,
        this.description
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
    override val description: String = "Stored property value."
}

/**
 * A Property whose value is computed using built-in computation,
 * it is 'Primitive' in the same sense that 'Primitive' types are based on built-in constructs.
 */
class PropertyDeclarationPrimitive(
    override val owner: TypeDeclaration,
    override val name: String,
    override val typeInstance: TypeInstance,
    override val description: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.MEMBER, PropertyCharacteristic.PRIMITIVE)

}

class PropertyDeclarationDerived(
    override val owner: TypeDeclaration,
    override val name: String,
    override val typeInstance: TypeInstance,
    override val description: String,
    val expression: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.MEMBER, PropertyCharacteristic.DERIVED)

}

class PropertyDeclarationResolved(
    override val owner: TypeDeclaration,
    override val name: String,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>,
    override val description: String
) : PropertyDeclarationAbstract() {
    override val index: Int get() = -1 // should never be included in owners list
}

class MethodDeclarationPrimitive(
    override val owner: TypeDeclaration,
    override val name: String,
    override val parameters: List<ParameterDeclaration>,
    override val description: String,
    val body: (self: Any, arguments: List<Any>) -> Any
) : MethodDeclaration {

}

class MethodDeclarationDerived(
    override val owner: TypeDeclaration,
    override val name: String,
    override val parameters: List<ParameterDeclaration>,
    override val description: String,
    val body: String
) : MethodDeclaration {

}

class ParameterDefinitionSimple(
    override val name: String,
    override val typeInstance: TypeInstance,
    override val defaultValue: String?
) : ParameterDeclaration {

    override fun hashCode(): Int = listOf(name).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PropertyDeclaration -> false
        this.name != other.name -> false
        else -> true
    }

    override fun toString(): String = "${name}: ${typeInstance}${if (null != defaultValue) " = $defaultValue" else ""}"
}