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

import net.akehurst.language.agl.language.base.NamespaceAbstract
import net.akehurst.language.api.language.base.*
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.collections.indexOfOrNull
import net.akehurst.language.typemodel.api.*

class TypeModelSimple(
    name: SimpleName,
) : TypeModelSimpleAbstract(name) {
    companion object {
        fun fromString(typeModelStr: String): ProcessResult<TypeModel> {
            TODO()
//            val proc = Agl.registry.agl.typeModel.processor ?: error("TypeModel language not found!")
//            return proc.process(
//                sentence = typeModelStr,
//                options = Agl.options { semanticAnalysis { context(context) } }
//            )
        }
    }

}

abstract class TypeModelSimpleAbstract(
    override val name: SimpleName,
) : TypeModel {

    override val AnyType: TypeDeclaration get() = SimpleTypeModelStdLib.AnyType.declaration //TODO: stdLib not necessarily part of model !
    override val NothingType: TypeDeclaration get() = SimpleTypeModelStdLib.NothingType.declaration //TODO: stdLib not necessarily part of model !

    private val _namespace: Map<QualifiedName, TypeNamespace> = linkedMapOf()
    override val namespace: List<TypeNamespace> get() = _namespace.values.toList()

    //store this separately to keep order of namespaces - important for lookup of types
    override val allNamespace: List<TypeNamespace> = mutableListOf<TypeNamespace>()

    override fun resolveImports() {
        allNamespace.forEach { it.resolveImports(this) }
    }

    fun addNamespace(ns: TypeNamespace) {
        if (_namespace.containsKey(ns.qualifiedName)) {
            if (_namespace[ns.qualifiedName] === ns) {
                //same object, no need to add it
            } else {
                error("TypeModel '${this.name}' already contains a namespace '${ns.qualifiedName}'")
            }
        } else {
            (_namespace as MutableMap)[ns.qualifiedName] = ns
            (allNamespace as MutableList).add(ns)
        }
    }

    override fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TypeNamespace {
        return if (_namespace.containsKey(qualifiedName)) {
            _namespace[qualifiedName]!!
        } else {
            val ns = TypeNamespaceSimple(qualifiedName, imports)
            addNamespace(ns)
            ns
        }
    }

    override fun findFirstByPossiblyQualifiedOrNull(typeName: PossiblyQualifiedName): TypeDeclaration? {
        return when (typeName) {
            is QualifiedName -> findNamespaceOrNull(typeName.front)?.findOwnedDefinitionOrNull(typeName.last)
            is SimpleName -> findFirstByNameOrNull(typeName)
            else -> error("Unsupported")
        }
    }

    override fun findFirstByNameOrNull(typeName: SimpleName): TypeDeclaration? {
        for (ns in allNamespace) {
            val t = ns.findOwnedTypeNamed(typeName)
            if (null != t) {
                return t
            }
        }
        return null
    }

    override fun findByQualifiedNameOrNull(qualifiedName: QualifiedName): TypeDeclaration? {
        val nsn = qualifiedName.front
        val tn = qualifiedName.last
        return _namespace[nsn]?.findOwnedTypeNamed(tn)
    }

    override fun addAllNamespace(namespaces: Iterable<TypeNamespace>) {
        namespaces.forEach { this.addNamespace(it) }
        this.resolveImports()
    }

    // --- DefinitionBlock ---
    override val allDefinitions: List<TypeDeclaration> get() = _namespace.values.flatMap { it.definition }

    override val isEmpty: Boolean get() = _namespace.isEmpty()

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): TypeNamespace? = _namespace[qualifiedName]

    // -- Formatable ---
    override fun asString(indent: Indent): String {
        val ns = this.allNamespace
            .sortedBy { it.qualifiedName.value }
            .joinToString(separator = "\n") { it.asString() }
        return "typemodel '$name'\n$ns"
    }

    // --- Any ---
    override fun hashCode(): Int = this.name.hashCode()

    override fun equals(other: Any?): Boolean = when {
        other !is TypeModel -> false
        this.name != other.name -> false
        this.namespace != other.namespace -> false
        else -> true
    }
}

abstract class TypeInstanceAbstract() : TypeInstance {

    abstract val typeOrNull: TypeDeclaration?

    override val resolvedProperty: Map<PropertyName, PropertyDeclaration>
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

    protected fun createTypeArgMap(): Map<SimpleName, TypeInstance> {
        val typeArgMap = mutableMapOf<SimpleName, TypeInstance>()
        typeOrNull?.typeParameters?.forEachIndexed { index, it ->
            val tp = it
            val ta = this.typeArguments[index]
            typeArgMap[tp] = ta
        }
        return typeArgMap
    }
}

class TypeInstanceSimple(
    val contextQualifiedTypeName: QualifiedName?,
    override val namespace: TypeNamespace,
    val qualifiedOrImportedTypeName: PossiblyQualifiedName,
    override val typeArguments: List<TypeInstance>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    val context: TypeDeclaration? get() = contextQualifiedTypeName?.let { namespace.findTypeNamed(it) }

    override val typeName: SimpleName
        get() = context?.typeParameters?.indexOfOrNull(qualifiedOrImportedTypeName)?.let {
            typeArguments.getOrNull(it)?.declaration?.name
        }
            ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.name
            ?: qualifiedOrImportedTypeName.simpleName

    override val qualifiedTypeName: QualifiedName
        get() = context?.typeParameters?.indexOfOrNull(qualifiedOrImportedTypeName)?.let {
            typeArguments.getOrNull(it)?.declaration?.qualifiedName
                ?: QualifiedName(qualifiedOrImportedTypeName.value) //TODO: not sure if this is always correct result!
        }
            ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.qualifiedName
            ?: if (qualifiedOrImportedTypeName is QualifiedName) qualifiedOrImportedTypeName else error("Not a QualifiedName")

    override val typeOrNull: TypeDeclaration? by lazy {
        context?.typeParameters?.indexOfOrNull(qualifiedOrImportedTypeName)?.let {
            typeArguments.getOrNull(it)?.declaration
        } ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)
    }

    override val declaration: TypeDeclaration
        get() = typeOrNull
            ?: error("Cannot resolve TypeDefinition '$qualifiedOrImportedTypeName', not found in namespace '${namespace.qualifiedName}'. Is an import needed?")


    override fun resolved(resolvingTypeArguments: Map<SimpleName, TypeInstance>): TypeInstance {
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

    override val typeName: SimpleName get() = TupleType.NAME.last
    override val qualifiedTypeName: QualifiedName get() = TupleType.NAME
    override val typeOrNull: TypeDeclaration get() = declaration

    override fun resolved(resolvingTypeArguments: Map<SimpleName, TypeInstance>): TypeInstance {
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

    override val typeName: SimpleName get() = UnnamedSupertypeType.NAME.last
    override val qualifiedTypeName: QualifiedName get() = UnnamedSupertypeType.NAME

    override val typeOrNull: TypeDeclaration get() = declaration

    override fun resolved(resolvingTypeArguments: Map<SimpleName, TypeInstance>): TypeInstance {
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
    qualifiedName: QualifiedName,
    imports: List<Import>
) : TypeNamespaceAbstract(qualifiedName, imports) {

}

abstract class TypeNamespaceAbstract(
    qualifiedName: QualifiedName,
    imports: List<Import>
) : TypeNamespace, NamespaceAbstract<TypeDeclaration>(qualifiedName) {

    private var _nextUnnamedSuperTypeTypeId = 0
    private val _unnamedSuperTypes = hashMapOf<List<TypeInstance>, UnnamedSupertypeType>()

    private var _nextTupleTypeTypeId = 0

    // qualified namespace name -> TypeNamespace
    private val _requiredNamespaces = mutableMapOf<QualifiedName, TypeNamespace?>()

    override val import: List<Import> = imports.toMutableList()

    override val ownedTypesByName get() = super.definitionByName

    override val ownedTypes: Collection<TypeDeclaration> get() = ownedTypesByName.values

    val ownedUnnamedSupertypeType = mutableListOf<UnnamedSupertypeType>()

    val ownedTupleTypes = mutableListOf<TupleType>()

    override val primitiveType: Set<PrimitiveType> get() = ownedTypesByName.values.filterIsInstance<PrimitiveType>().toSet()
    override val enumType: Set<EnumType> get() = ownedTypesByName.values.filterIsInstance<EnumType>().toSet()
    override val collectionType: Set<CollectionType> get() = ownedTypesByName.values.filterIsInstance<CollectionType>().toSet()
    override val valueType: Set<ValueType> get() = ownedTypesByName.values.filterIsInstance<ValueType>().toSet()
    override val interfaceType: Set<InterfaceType> get() = ownedTypesByName.values.filterIsInstance<InterfaceType>().toSet()
    override val dataType: Set<DataType> get() = ownedTypesByName.values.filterIsInstance<DataType>().toSet()

    //override fun resolveImports(model: Model<Namespace<TypeDeclaration>, TypeDeclaration>) {
    override fun resolveImports(model: TypeModel) {
        // check explicit imports
        this.import.forEach {
            val ns = model.findNamespaceOrNull(it.asQualifiedName) ?: error("import '$it' cannot be resolved in the TypeModel '${model.name}'")
            _requiredNamespaces[it.asQualifiedName] = ns
        }
        // check required namespaces
        // _requiredNamespaces.keys.forEach {
        //     val ns = model.findNamespaceOrNull(it) ?: error("namespace '$it' is required but cannot be resolved in the TypeModel '${model.name}'")
        //     _requiredNamespaces[it] = ns
        // }
    }

    override fun isImported(qualifiedNamespaceName: QualifiedName): Boolean = import.contains(Import(qualifiedNamespaceName.value))

    override fun addImport(import: Import) {
        (this.import as MutableList).add(import)
    }

    fun addDeclaration(decl: TypeDeclaration) {
        if (ownedTypesByName.containsKey(decl.name)) {
            error("namespace '$qualifiedName' already contains a declaration named '${decl.name}', cannot add another")
        } else {
            when (decl) {
                is PrimitiveType -> addDefinition(decl)
                is EnumType -> addDefinition(decl)
                is UnnamedSupertypeType -> Unit
                is StructuredType -> when (decl) {
                    is TupleType -> Unit
                    is DataType -> addDefinition(decl)
                    is CollectionType -> addDefinition(decl)
                }

                else -> error("Cannot add declaration '$decl'")
            }
        }
    }

    override fun findOwnedTypeNamed(typeName: SimpleName): TypeDeclaration? = ownedTypesByName[typeName]

    override fun findTypeNamed(qualifiedOrImportedTypeName: PossiblyQualifiedName): TypeDeclaration? {
        return when (qualifiedOrImportedTypeName) {
            is QualifiedName -> {
                val qn = qualifiedOrImportedTypeName
                val ns = qn.front
                val tn = qn.last
                when (ns) {
                    this.qualifiedName -> findOwnedTypeNamed(tn)
                    else -> {
                        val tns = _requiredNamespaces[ns]
                            ?: error("namespace '$ns' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                        tns.findOwnedTypeNamed(tn)
                    }
                }
            }

            is SimpleName -> {
                val tn = qualifiedOrImportedTypeName
                findOwnedTypeNamed(tn)
                    ?: import.firstNotNullOfOrNull {
                        val tns = _requiredNamespaces[it.asQualifiedName]
                        //    ?: error("namespace '$it' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                        tns?.findOwnedTypeNamed(tn)
                    }
            }

            else -> error("Unsupported")
        }
    }

    fun findOrCreateSpecialTypeNamed(typeName: SimpleName): SpecialTypeSimple {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = SpecialTypeSimple(this, typeName)
            addDefinition(t)
            t
        } else {
            existing as SpecialTypeSimple
        }
    }

    override fun findOwnedOrCreateSingletonTypeNamed(typeName: SimpleName): SingletonType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = SingletonTypeSimple(this, typeName)
            addDefinition(t)
            t
        } else {
            existing as SingletonType
        }
    }

    override fun findOwnedOrCreatePrimitiveTypeNamed(typeName: SimpleName): PrimitiveType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = PrimitiveTypeSimple(this, typeName)
            addDefinition(t)
            t
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOwnedOrCreateEnumTypeNamed(typeName: SimpleName, literals: List<String>): EnumType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = EnumTypeSimple(this, typeName, literals)
            addDefinition(t)
            t
        } else {
            existing as EnumType
        }
    }

    override fun findOwnedOrCreateCollectionTypeNamed(typeName: SimpleName): CollectionType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = CollectionTypeSimple(this, typeName)
            addDefinition(t)
            t
        } else {
            existing as CollectionType
        }
    }

    override fun findOwnedOrCreateValueTypeNamed(typeName: SimpleName): ValueType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t =ValueTypeSimple(this, typeName)
            addDefinition(t)
            t
        } else {
            existing as ValueType
        }
    }

    override fun findOwnedOrCreateInterfaceTypeNamed(typeName: SimpleName): InterfaceType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = InterfaceTypeSimple(this, typeName)
            addDefinition(t)
            t
        } else {
            existing as InterfaceType
        }
    }

    override fun findOwnedOrCreateDataTypeNamed(typeName: SimpleName): DataType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            val t = DataTypeSimple(this, typeName)
            addDefinition(t)
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
        ownedTupleTypes.add(td) //FIXME: don't think this is needed
        return td
    }

    override fun createTypeInstance(
        context: TypeDeclaration?,
        qualifiedOrImportedTypeName: PossiblyQualifiedName,
        typeArguments: List<TypeInstance>,
        isNullable: Boolean
    ): TypeInstance {
        //when (qualifiedOrImportedTypeName) {
        //    is QualifiedName -> this._requiredNamespaces[qualifiedOrImportedTypeName.front] = null
        //    is SimpleName -> Unit
        //    else -> error("Unsupported")
        //}
        return TypeInstanceSimple(context?.qualifiedName, this, qualifiedOrImportedTypeName, typeArguments, isNullable)
    }

    override fun createUnnamedSupertypeTypeInstance(declaration: UnnamedSupertypeType, typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return UnnamedSupertypeTypeInstance(this, declaration, typeArguments, nullable)
    }

    override fun createTupleTypeInstance(declaration: TupleType, typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return TupleTypeInstance(this, declaration, typeArguments, nullable)
    }

    override val definition: List<TypeDeclaration>
        get() = TODO("not implemented")

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val types = this.ownedTypesByName.entries.sortedBy { it.key.value }
            .joinToString(prefix = "  ", separator = "\n  ") { it.value.asStringInContext(this) }
        val importstr = this.import.joinToString(prefix = "  ", separator = "\n  ") { "import ${it}.*" }
        val s = """
namespace '$qualifiedName' {
$importstr
$types
}
    """.trimIndent()
        return s
    }

    // --- Any ---
    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeNamespace -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName.value
}

abstract class TypeDeclarationSimpleAbstract() : TypeDeclaration {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val supertypes: List<TypeInstance> = mutableListOf()

    override val typeParameters: List<SimpleName> = mutableListOf() //make implementation mutable for serialisation

    // store properties by map(index) rather than list(index), because when constructing from grammar, not every index is used
    // public, so it can be serialised
    val propertyByIndex = mutableMapOf<Int, PropertyDeclaration>()
    override val property get() = propertyByIndex.values.toList() //should be in order because mutableMap is LinkedHashMap by default
    //protected val properties = mutableListOf<PropertyDeclaration>()

    override val method = mutableListOf<MethodDeclaration>()

    override val allSuperTypes: List<TypeInstance> get() = supertypes + supertypes.flatMap { (it.declaration as DataType).allSuperTypes }

    override val allProperty: Map<PropertyName, PropertyDeclaration> get() = property.associateBy { it.name }

    val allMethod: Map<MethodName, MethodDeclaration> get() = method.associateBy { it.name }

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

    override fun findPropertyOrNull(name: PropertyName): PropertyDeclaration? =
        this.allProperty[name]

    override fun findMethodOrNull(name: MethodName): MethodDeclaration? =
        this.allMethod[name]

    // --- mutable ---
    override fun addSupertype(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        //TODO: check if create loop of supertypes - pre namespace resolving!
        (this.supertypes as MutableList).add(ti)
        //(type.subtypes as MutableList).add(this) //TODO: can we somehow add the reverse!
    }

    /**
     * append a derived property, with the expression that derived it
     */
    override fun appendPropertyDerived(name: PropertyName, typeInstance: TypeInstance, description: String, expression: String) {
        val propIndex = property.size
        val prop = PropertyDeclarationDerived(this, name, typeInstance, description, expression, propIndex)
        this.addProperty(prop)
    }

    override fun appendPropertyPrimitive(name: PropertyName, typeInstance: TypeInstance, description: String) {
        val propIndex = property.size
        val prop = PropertyDeclarationPrimitive(this, name, typeInstance, description, propIndex)
        this.addProperty(prop)
    }

    override fun appendMethodPrimitive(
        name: MethodName,
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
    override fun appendMethodDerived(name: MethodName, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String) {
        val meth = MethodDeclarationDerived(this, name, parameters, description, body)
    }

    override fun asStringInContext(context: TypeNamespace): String = signature(context, 0)

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        TODO("not implemented")
    }

    // --- Any ---
    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeDeclaration -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName.value

    // --- Implementation

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
    override val name: SimpleName
) : TypeDeclarationSimpleAbstract() {
    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String = "special ${signature(context)}"

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is SpecialTypeSimple -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName.value
}

class SingletonTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : TypeDeclarationSimpleAbstract(), SingletonType {
    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String = "singleton ${signature(context)}"

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is SpecialTypeSimple -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName.value
}

class PrimitiveTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : TypeDeclarationSimpleAbstract(), PrimitiveType {

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String = "primitive ${signature(context)}"

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PrimitiveType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName.value
}

class EnumTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName,
    override val literals: List<String>
) : TypeDeclarationSimpleAbstract(), EnumType {
    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String = "enum ${signature(context)}"

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is PrimitiveType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName.value
}

class UnnamedSupertypeTypeSimple(
    override val namespace: TypeNamespace,
    override val id: Int, // needs a number else can't implement equals without a recursive loop
    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: List<TypeInstance>
) : TypeDeclarationSimpleAbstract(), UnnamedSupertypeType {


    override val name = UnnamedSupertypeType.NAME.last

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

    override fun asStringInContext(context: TypeNamespace): String = "unnamed ${signature(context)}"

    override fun hashCode(): Int = id
    override fun equals(other: Any?): Boolean = when (other) {
        is UnnamedSupertypeType -> other.id == this.id
        else -> false
    }

    override fun toString(): String = name.value
}

abstract class StructuredTypeSimpleAbstract : TypeDeclarationSimpleAbstract(), StructuredType {

    override fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration> =
        property.filter { it.characteristics.contains(chr) }

    /**
     * append property, if index < 0 then use next property number
     */
    override fun appendPropertyStored(name: PropertyName, typeInstance: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int): PropertyDeclaration {
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

    override val name = TupleType.NAME.last

    override val entries get() = property.map { Pair(it.name, it.typeInstance) }

    override fun type(typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance {
        return namespace.createTupleTypeInstance(this, typeArguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "${name}<${this.property.joinToString { it.name.value + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
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

    override fun asStringInContext(context: TypeNamespace): String = "tuple ${signature(context)}"

    override fun hashCode(): Int = this.id
    override fun equals(other: Any?): Boolean = when {
        other !is TupleTypeSimple -> false
        this.id != other.id -> false
        this.namespace != other.namespace -> false
        else -> true
    }

    override fun toString(): String = "Tuple<${this.property.joinToString { it.name.value + ":" + it.typeInstance }}>"
}

class ValueTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : StructuredTypeSimpleAbstract(), ValueType {

    //override var typeParameters = mutableListOf<SimpleName>()

    override val allProperty: Map<PropertyName, PropertyDeclaration>
        get() = supertypes.flatMap {
            (it.declaration as DataType).allProperty.values
        }.associateBy { it.name } + this.property.associateBy { it.name }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String {
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
        return "value ${name}${sups} {\n    $props\n  }"
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is ValueType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName.value
}

class InterfaceTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : StructuredTypeSimpleAbstract(), InterfaceType {

    override var typeParameters = mutableListOf<SimpleName>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    override val allProperty: Map<PropertyName, PropertyDeclaration>
        get() = supertypes.flatMap {
            (it.declaration as DataType).allProperty.values
        }.associateBy { it.name } + this.property.associateBy { it.name }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun addSubtype(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        (this.subtypes as MutableList).add(ti) //TODO: can we somehow add the reverse!
    }

    override fun asStringInContext(context: TypeNamespace): String {
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
        return "interface ${name}${sups} {\n    $props\n  }"
    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is InterfaceType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }

    override fun toString(): String = qualifiedName.value
}

class DataTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : StructuredTypeSimpleAbstract(), DataType {

    override var typeParameters = mutableListOf<SimpleName>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    override val allProperty: Map<PropertyName, PropertyDeclaration>
        get() = supertypes.flatMap {
            (it.declaration as DataType).allProperty.values
        }.associateBy { it.name } + this.property.associateBy { it.name }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun addSubtype(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        (this.subtypes as MutableList).add(ti) //TODO: can we somehow add the reverse!
    }

    override fun asStringInContext(context: TypeNamespace): String {
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

    override fun toString(): String = qualifiedName.value
}

class CollectionTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName,
    override var typeParameters: List<SimpleName> = mutableListOf()
) : StructuredTypeSimpleAbstract(), CollectionType {

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String = "collection ${signature(context)}"
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

    override fun resolved(typeArguments: Map<SimpleName, TypeInstance>): PropertyDeclaration = PropertyDeclarationResolved(
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
    override val name: PropertyName,
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
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val description: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.MEMBER, PropertyCharacteristic.PRIMITIVE)

}

class PropertyDeclarationDerived(
    override val owner: TypeDeclaration,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val description: String,
    val expression: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.MEMBER, PropertyCharacteristic.DERIVED)

}

class PropertyDeclarationResolved(
    override val owner: TypeDeclaration,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>,
    override val description: String
) : PropertyDeclarationAbstract() {
    override val index: Int get() = -1 // should never be included in owners list
}

class MethodDeclarationPrimitive(
    override val owner: TypeDeclaration,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val description: String,
    val body: (self: Any, arguments: List<Any>) -> Any
) : MethodDeclaration {

}

class MethodDeclarationDerived(
    override val owner: TypeDeclaration,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val description: String,
    val body: String
) : MethodDeclaration {

}

class ParameterDefinitionSimple(
    override val name: net.akehurst.language.typemodel.api.ParameterName,
    override val typeInstance: TypeInstance,
    override val defaultValue: String?
) : ParameterDeclaration {

    override fun hashCode(): Int = listOf(name).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is ParameterDeclaration -> false
        this.name != other.name -> false
        else -> true
    }

    override fun toString(): String = "${name}: ${typeInstance}${if (null != defaultValue) " = $defaultValue" else ""}"
}