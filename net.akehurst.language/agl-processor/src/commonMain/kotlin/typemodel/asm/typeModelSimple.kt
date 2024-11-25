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

package net.akehurst.language.typemodel.asm

import net.akehurst.language.agl.TypeModelString
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.util.cached

class TypeModelSimple(
    override val name: SimpleName,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
) : TypeModelSimpleAbstract() {

    companion object {
        fun fromString(typeModelStr: TypeModelString): ProcessResult<TypeModel> {
            TODO()
//            val proc = Agl.registry.agl.typeModel.processor ?: error("TypeModel language not found!")
//            return proc.process(
//                sentence = typeModelStr,
//                options = Agl.options { semanticAnalysis { context(context) } }
//            )
        }
    }

}

abstract class TypeModelSimpleAbstract() : TypeModel {

    override val AnyType: TypeDeclaration get() = SimpleTypeModelStdLib.AnyType.declaration //TODO: stdLib not necessarily part of model !
    override val NothingType: TypeDeclaration get() = SimpleTypeModelStdLib.NothingType.declaration //TODO: stdLib not necessarily part of model !

    // stored in namespace so deserialisation works
    private val _namespace = cached {
        namespace.associateBy { it.qualifiedName }
    }

    //store this separately to keep order of namespaces - important for lookup of types
    override val namespace: List<TypeNamespace> = mutableListOf<TypeNamespace>()

    override fun resolveImports() {
        namespace.forEach { it.resolveImports(this as Model<Namespace<TypeDeclaration>, TypeDeclaration>) } //TODO
    }

    fun addNamespace(ns: TypeNamespace) {
        if (_namespace.value.containsKey(ns.qualifiedName)) {
            if (_namespace.value[ns.qualifiedName] === ns) {
                //same object, no need to add it
            } else {
                error("TypeModel '${this.name}' already contains a namespace '${ns.qualifiedName}'")
            }
        } else {
            (namespace as MutableList).add(ns)
            _namespace.reset()
        }
    }

    override fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TypeNamespace {
        return if (_namespace.value.containsKey(qualifiedName)) {
            _namespace.value[qualifiedName]!!
        } else {
            val ns = TypeNamespaceSimple(qualifiedName = qualifiedName, import = imports)
            addNamespace(ns)
            ns
        }
    }

    override fun findFirstByPossiblyQualifiedOrNull(typeName: PossiblyQualifiedName): TypeDeclaration? {
        return when (typeName) {
            is QualifiedName -> findNamespaceOrNull(typeName.front)?.findOwnedTypeNamed(typeName.last)
            is SimpleName -> findFirstByNameOrNull(typeName)
            else -> error("Unsupported")
        }
    }

    override fun findFirstByNameOrNull(typeName: SimpleName): TypeDeclaration? {
        for (ns in namespace) {
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
        return _namespace.value[nsn]?.findOwnedTypeNamed(tn)
    }

    override fun addAllNamespaceAndResolveImports(namespaces: Iterable<TypeNamespace>) {
        namespaces.forEach { this.addNamespace(it) }
        this.resolveImports()
    }

    // --- DefinitionBlock ---
    override val allDefinitions: List<TypeDeclaration> get() = namespace.flatMap { it.definition }

    override val isEmpty: Boolean get() = namespace.isEmpty()

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): TypeNamespace? = _namespace.value[qualifiedName]

    override fun findDefinitionOrNullByQualifiedName(qualifiedName: QualifiedName): TypeDeclaration? {
        TODO("not implemented")
    }

    override fun resolveReference(reference: DefinitionReference<TypeDeclaration>): TypeDeclaration? {
        TODO("not implemented")
    }

    // -- Formatable ---
    override fun asString(indent: Indent): String {
        val ns = this.namespace
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

    override val allResolvedProperty: Map<PropertyName, PropertyDeclarationResolved>
        get() {
            val typeArgMap = createTypeArgMap()
            val superProps = declaration.supertypes.map {
                val resIt = it.resolved(typeArgMap)
                resIt.allResolvedProperty
            }.fold(mapOf<PropertyName, PropertyDeclarationResolved>()) { acc, it -> acc + it }
            val ownResolveProps = declaration.property.associate {
                val rp = it.resolved(typeArgMap)
                Pair(it.name, rp)
            }
            return superProps + ownResolveProps
        }

    override val allResolvedMethod: Map<MethodName, MethodDeclarationResolved>
        get() {
            val typeArgMap = createTypeArgMap()
            val superProps = declaration.supertypes.map {
                val resIt = it.resolved(typeArgMap)
                resIt.allResolvedMethod
            }.fold(mapOf<MethodName, MethodDeclarationResolved>()) { acc, it -> acc + it }
            val ownResolveProps = declaration.method.associate {
                val rp = it.resolved(typeArgMap)
                Pair(it.name, rp)
            }
            return superProps + ownResolveProps
        }

    override val asTypeArgument: TypeArgument get() = TypeArgumentSimple(this)

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
                    typeArguments.isEmpty() -> declarationOrNull?.signature(context, currentDepth + 1) ?: this.typeName.value
                    else -> declaration.name.value
                }
                return "${name}$args$n"
            }
        }
    }

    override fun conformsTo(other: TypeInstance): Boolean = when {
        other === this -> true // fast option
        this == SimpleTypeModelStdLib.NothingType -> other.isNullable
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

    protected open fun createTypeArgMap(): Map<TypeParameter, TypeInstance> {
        val typeArgMap = mutableMapOf<TypeParameter, TypeInstance>()
        declarationOrNull?.typeParameters?.forEachIndexed { index, it ->
            val tp = it
            // maybe the type arg has not been explicitly provided, in which case provide AnyType
            val ta = this.typeArguments.getOrNull(index) ?: SimpleTypeModelStdLib.AnyType.asTypeArgument
            typeArgMap[tp] = when (ta) {
                is TypeArgument -> ta.type
                else -> error("Unsupported")
            }
        }
        return typeArgMap
    }
}

class TypeParameterReference(
    val context: TypeDeclaration,
    val typeParameterName: SimpleName,
    override val isNullable: Boolean = false
) : TypeInstanceAbstract(), TypeInstance {

    override val namespace: TypeNamespace get() = context.namespace

    override val qualifiedTypeName: QualifiedName get() = context.qualifiedName.append(typeParameterName)

    override val typeName: SimpleName get() = typeParameterName

    override val typeArguments: List<TypeArgument> = emptyList()

    override val declarationOrNull: TypeDeclaration? = null

    override val declaration: TypeDeclaration get() = error("TypeParameterReference does not have a declaration")

    override fun conformsTo(other: TypeInstance): Boolean {
        TODO("not implemented")
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = "&${typeParameterName.value}"

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance =
        context.typeParameters.firstOrNull { it.name == typeParameterName }
            ?.let { resolvingTypeArguments[it] }
            ?: error("Cannot resolve TypeArgument named '$typeParameterName' in context of '${context.qualifiedName}'")

    override fun possiblyQualifiedNameInContext(context: TypeNamespace): PossiblyQualifiedName = typeParameterName

    override fun hashCode(): Int = listOf(context, typeParameterName).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeParameterReference -> false
        this.context != other.context -> false
        this.typeParameterName != other.typeParameterName -> false
        else -> true
    }

    override fun toString(): String = "&${typeParameterName.value}"
}

data class TypeArgumentSimple(
    override val type: TypeInstance
) : TypeArgument {
    override fun conformsTo(other: TypeArgument): Boolean {
        return this.type.conformsTo(other.type)
    }

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance {
        return type.resolved(resolvingTypeArguments)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        return type.signature(context, currentDepth)
    }

    override fun toString(): String = signature(null, 0)
}

class TypeInstanceSimple(
    val contextQualifiedTypeName: QualifiedName?, //TODO: not really needed i think
    override val namespace: TypeNamespace,
    val qualifiedOrImportedTypeName: PossiblyQualifiedName,
    override val typeArguments: List<TypeArgument>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    companion object {
        fun combineTypeArgMap(first: Map<TypeParameter, TypeInstance>, second: Map<TypeParameter, TypeInstance>): Map<TypeParameter, TypeInstance> {
            val result = second.toMutableMap() //clone it
            first.forEach { (k, v) ->
                when {
                    result.containsKey(k) -> Unit // overriden by second
                    v is TypeParameterReference -> when {
                        result.any { it.key.name ==  v.typeParameterName } -> result[k] = result[TypeParameterSimple(v.typeParameterName)]!!
                    }
                    else -> result[k] = v
                }
            }
            return result
        }
    }

    val context: TypeDeclaration?
        get() = contextQualifiedTypeName?.let {
            namespace.findTypeNamed(it)
        }

    override val typeName: SimpleName
        get() = declarationOrNull?.name
            ?: qualifiedOrImportedTypeName.simpleName

    override val qualifiedTypeName: QualifiedName
        get() = declarationOrNull?.qualifiedName
            ?: when (qualifiedOrImportedTypeName) {
                is QualifiedName -> qualifiedOrImportedTypeName
                is SimpleName -> context?.namespace?.qualifiedName?.append(qualifiedOrImportedTypeName)
                else -> error("Unsupported")
            }
            ?: error("Cannot construct a Qualified name for '$qualifiedOrImportedTypeName' in context of '$contextQualifiedTypeName'")

    override val declarationOrNull: TypeDeclaration? get() = namespace.findTypeNamed(qualifiedOrImportedTypeName)

    override val declaration: TypeDeclaration
        get() = declarationOrNull
            ?: error("Cannot resolve TypeDefinition '$qualifiedOrImportedTypeName', not found in namespace '${namespace.qualifiedName}'. Is an import needed?")

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance {
        //val selfAsTypeParameter = TypeParameterSimple(this.qualifiedOrImportedTypeName)
        //val selfResolved = resolvingTypeArguments[selfAsTypeParameter]
        return when {
            // selfResolved != null -> selfResolved
            else -> {
                val thisTypeArgMap = combineTypeArgMap(createTypeArgMap(), resolvingTypeArguments)
                val resolvedTypeArgs = this.typeArguments.map {
                    val ti = it.resolved(thisTypeArgMap)// + resolvingTypeArguments)
                    TypeArgumentSimple(ti)
                }
                TypeInstanceSimple(contextQualifiedTypeName, this.namespace, this.qualifiedOrImportedTypeName, resolvedTypeArgs, this.isNullable)
            }
        }
    }

    override fun possiblyQualifiedNameInContext(context: TypeNamespace): PossiblyQualifiedName = when {
        this.declaration.namespace == context -> this.typeName
        else -> this.qualifiedTypeName
    }

    override fun hashCode(): Int = listOf(qualifiedOrImportedTypeName, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> when {
            null == this.contextQualifiedTypeName || null == other.contextQualifiedTypeName -> this.qualifiedOrImportedTypeName == other.qualifiedOrImportedTypeName
            this.contextQualifiedTypeName == other.contextQualifiedTypeName && this.qualifiedOrImportedTypeName == other.qualifiedOrImportedTypeName -> true
            else -> this.qualifiedTypeName == other.qualifiedTypeName
        }
    }
}

class TupleTypeInstanceSimple(
    override val namespace: TypeNamespace,
    override val typeArguments: List<TypeArgumentNamed>,
    override val isNullable: Boolean
) : TypeInstanceAbstract(), TupleTypeInstance {

    override val typeName: SimpleName get() = declaration.name
    override val qualifiedTypeName: QualifiedName get() = declaration.qualifiedName
    override val declarationOrNull: TypeDeclaration get() = declaration
    override val declaration: TypeDeclaration = SimpleTypeModelStdLib.TupleType

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance {
        val thisTypeArgMap = createTypeArgMap()
        val resolvedTypeArgs = this.typeArguments.map {
            val ti = it.resolved(thisTypeArgMap + resolvingTypeArguments)
            TypeArgumentNamedSimple(it.name, ti)
        }
        return TupleTypeInstanceSimple(this.namespace, resolvedTypeArgs, this.isNullable)
    }

    override fun createTypeArgMap(): Map<TypeParameter, TypeInstance> = emptyMap()

    override fun possiblyQualifiedNameInContext(context: TypeNamespace): PossiblyQualifiedName = when {
        this.declaration.namespace == context -> this.typeName
        else -> this.qualifiedTypeName
    }

    override fun hashCode(): Int = listOf(declaration, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.declaration != other.declaration -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }

    override fun toString(): String {
        val args = typeArguments.joinToString(separator = ", ") { "${it.name}: ${it.type.signature(null, 0)}" }
        return "${typeName}<$args>"
    }
}

class UnnamedSupertypeTypeInstance(
    override val namespace: TypeNamespace,
    override val declaration: UnnamedSupertypeType,
    override val typeArguments: List<TypeArgument>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: SimpleName get() = UnnamedSupertypeType.NAME.last
    override val qualifiedTypeName: QualifiedName get() = UnnamedSupertypeType.NAME

    override val declarationOrNull: TypeDeclaration get() = declaration

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance {
        val thisTypeArgMap = createTypeArgMap()
        val resolvedTypeArgs = this.typeArguments.map {
            val ti = it.resolved(thisTypeArgMap + resolvingTypeArguments)
            TypeArgumentSimple(ti)
        }
        return UnnamedSupertypeTypeInstance(this.namespace, this.declaration, resolvedTypeArgs, this.isNullable)
    }

    override fun possiblyQualifiedNameInContext(context: TypeNamespace): PossiblyQualifiedName = when {
        this.declaration.namespace == context -> this.typeName
        else -> this.qualifiedTypeName
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
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : TypeNamespaceAbstract(options,import) {

}

abstract class TypeNamespaceAbstract(
    options: OptionHolder,
    import: List<Import>
) : TypeNamespace, NamespaceAbstract<TypeDeclaration>(options, import) {

    private var _nextUnnamedSuperTypeTypeId = 0
    private val _unnamedSuperTypes = hashMapOf<List<TypeInstance>, UnnamedSupertypeType>()

    private var _nextTupleTypeTypeId = 0

    // qualified namespace name -> TypeNamespace
    //private val _requiredNamespaces = mutableMapOf<QualifiedName, TypeNamespace?>()

    override val import: List<Import> = import.toMutableList()

    override val ownedTypesByName get() = super.definitionByName

    override val ownedTypes: Collection<TypeDeclaration> get() = ownedTypesByName.values

    val ownedUnnamedSupertypeType = mutableListOf<UnnamedSupertypeType>()

    val ownedTupleTypes = mutableListOf<TupleType>()

    override val singletonType: Set<SingletonType> get() = ownedTypesByName.values.filterIsInstance<SingletonType>().toSet()
    override val primitiveType: Set<PrimitiveType> get() = ownedTypesByName.values.filterIsInstance<PrimitiveType>().toSet()
    override val valueType: Set<ValueType> get() = ownedTypesByName.values.filterIsInstance<ValueType>().toSet()
    override val enumType: Set<EnumType> get() = ownedTypesByName.values.filterIsInstance<EnumType>().toSet()
    override val collectionType: Set<CollectionType> get() = ownedTypesByName.values.filterIsInstance<CollectionType>().toSet()
    override val interfaceType: Set<InterfaceType> get() = ownedTypesByName.values.filterIsInstance<InterfaceType>().toSet()
    override val dataType: Set<DataType> get() = ownedTypesByName.values.filterIsInstance<DataType>().toSet()

    //override fun resolveImports(model: Model<Namespace<TypeDeclaration>, TypeDeclaration>) {
    // override fun resolveImports(model: TypeModel) {
    // check explicit imports
    //     this.import.forEach {
    //         val ns = model.findNamespaceOrNull(it.asQualifiedName)
    //             ?: error("import '$it' cannot be resolved in the TypeModel '${model.name}'")
    //         _requiredNamespaces[it.asQualifiedName] = ns
    //     }
    // check required namespaces
    // _requiredNamespaces.keys.forEach {
    //     val ns = model.findNamespaceOrNull(it) ?: error("namespace '$it' is required but cannot be resolved in the TypeModel '${model.name}'")
    //     _requiredNamespaces[it] = ns
    // }
    //}

    override fun isImported(qualifiedNamespaceName: QualifiedName): Boolean = import.contains(Import(qualifiedNamespaceName.value))

    override fun addImport(value: Import) {
        if (this.import.contains(value)) {
            // do not repeat imports
        } else {
            (this.import as MutableList).add(value)
        }
    }

    fun addDeclaration(decl: TypeDeclaration) {
        if (ownedTypesByName.containsKey(decl.name)) {
            error("namespace '$qualifiedName' already contains a declaration named '${decl.name}', cannot add another")
        } else {
            when (decl) {
                is TupleType -> addDefinition(decl)
                is PrimitiveType -> addDefinition(decl)
                is EnumType -> addDefinition(decl)
                is UnnamedSupertypeType -> Unit
                is StructuredType -> when (decl) {
                    is DataType -> addDefinition(decl)
                    is CollectionType -> addDefinition(decl)
                }

                else -> error("Cannot add declaration '$decl'")
            }
        }
    }

    override fun findOwnedTypeNamed(typeName: SimpleName): TypeDeclaration? =
        ownedTypesByName[typeName]
    /*?: when {
        typeName.value.startsWith(TupleType.NAME.value + "-") -> {
            typeName.let {
                val idStr = typeName.value.substringAfter(TupleType.NAME.value + "-")
                val id = idStr.toInt()
                ownedTupleTypes[id]
                //?: error("Cannot find TupleType '$typeName' in namespace '$qualifiedName'")
            }
        }

        else -> null
    }
*/

    override fun findTypeNamed(qualifiedOrImportedTypeName: PossiblyQualifiedName): TypeDeclaration? {
        return when (qualifiedOrImportedTypeName) {
            is QualifiedName -> {
                val qn = qualifiedOrImportedTypeName
                val ns = qn.front
                val tn = qn.last
                when (ns) {
                    this.qualifiedName -> findOwnedTypeNamed(tn)
                    else -> {
                        val tns = _importedNamespaces[ns]
                        //?: error("namespace '$ns' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                        (tns as TypeNamespace?)?.findOwnedTypeNamed(tn)
                    }
                }
            }

            is SimpleName -> {
                val tn = qualifiedOrImportedTypeName
                findOwnedTypeNamed(tn)
                    ?: import.firstNotNullOfOrNull {
                        val tns = _importedNamespaces[it.asQualifiedName]
                        //    ?: error("namespace '$it' not resolved in namespace '$qualifiedName', have you called resolveImports() on the TypeModel and does it contain the required namespace?")
                        (tns as TypeNamespace?)?.findOwnedTypeNamed(tn)
                    }
            }

            else -> error("Unsupported")
        }
    }

    override fun findTupleTypeWithIdOrNull(id: Int): TupleType? = ownedTupleTypes.getOrNull(id)

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
            val t = ValueTypeSimple(this, typeName)
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
        return SimpleTypeModelStdLib.TupleType
        //val td = TupleTypeSimple(this, _nextTupleTypeTypeId++)
        //ownedTupleTypes.add(td) //FIXME: don't think this is needed
        //return td
    }

    override fun createTypeInstance(
        context: TypeDeclaration?,
        qualifiedOrImportedTypeName: PossiblyQualifiedName,
        typeArguments: List<TypeArgument>,
        isNullable: Boolean
    ): TypeInstance {
        //when (qualifiedOrImportedTypeName) {
        //    is QualifiedName -> this._requiredNamespaces[qualifiedOrImportedTypeName.front] = null
        //    is SimpleName -> Unit
        //    else -> error("Unsupported")
        //}
        return TypeInstanceSimple(context?.qualifiedName, this, qualifiedOrImportedTypeName, typeArguments, isNullable)
    }

    override fun createUnnamedSupertypeTypeInstance(declaration: UnnamedSupertypeType, typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance {
        return UnnamedSupertypeTypeInstance(this, declaration, typeArguments, nullable)
    }

    override fun createTupleTypeInstance(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance {
        return TupleTypeInstanceSimple(this, typeArguments, nullable)
    }

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val types = this.ownedTypesByName.entries.sortedBy { it.key.value }
            .joinToString(prefix = "  ", separator = "\n  ") { it.value.asStringInContext(this) }
        val importstr = this.import.joinToString(prefix = "  ", separator = "\n  ") { "import ${it}.*" }
        val s = """namespace '$qualifiedName' {
$importstr
$types
}""".trimIndent()
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

class TypeParameterSimple(
    override val name: SimpleName
) : TypeParameter {
    override fun hashCode(): Int =name.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeParameter -> false
        other.name != this.name -> false
        else -> true
    }
    override fun toString(): String = name.value
}

object TypeParameterMultiple : TypeParameter {
    override val name = SimpleName("...")
}

abstract class TypeDeclarationSimpleAbstract(
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap())
) : TypeDeclaration {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val supertypes: List<TypeInstance> = mutableListOf()

    override val typeParameters: List<TypeParameter> = mutableListOf() //make implementation mutable for serialisation

    // store properties by map(index) rather than list(index), because when constructing from grammar, not every index is used
    // public, so it can be serialised
    val propertyByIndex = mutableMapOf<Int, PropertyDeclaration>()

    override val property get() = propertyByIndex.values.toList() //should be in order because mutableMap is LinkedHashMap by default
    //protected val properties = mutableListOf<PropertyDeclaration>()

    override val method = mutableListOf<MethodDeclaration>()

    override val allSuperTypes: List<TypeInstance> get() = supertypes + supertypes.flatMap { (it.declaration as DataType).allSuperTypes }

    override val allProperty: Map<PropertyName, PropertyDeclaration>
        get() = supertypes.flatMap {
            it.declaration.allProperty.values
        }.associateBy { it.name } + this.property.associateBy { it.name }

    override val allMethod: Map<MethodName, MethodDeclaration> get() =supertypes.flatMap {
        it.declaration.allMethod.values
    }.associateBy { it.name } + this.method.associateBy { it.name }

    /**
     * information about this type
     */
    override var metaInfo = mutableMapOf<String, String>()

    override fun type(typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance =
        namespace.createTypeInstance(this, this.name, typeArguments, nullable)

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
    override fun addTypeParameter(name: TypeParameter) {
        (this.typeParameters as MutableList).add(name)
    }

    override fun addSupertype_dep(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        addSupertype(ti)
    }

    override fun addSupertype(typeInstance: TypeInstance) {
        // cannot add the reverse (typeInstance.declaration.addSubtype(this)
        // because this is a TypeDeclaration and we do not know the TypeArgs
        if (this.supertypes.contains(typeInstance)) {
            //do not add it again
        } else {
            //TODO: check if create loop of supertypes - pre namespace resolving!
            (this.supertypes as MutableList).add(typeInstance)
        }
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
        description: String
    ) {
        val m = MethodDeclarationPrimitiveSimple(this, name, parameters, typeInstance, description)
        this.addMethod(m)
    }

    /**
     * append a method/function with the expression that should execute
     */
    override fun appendMethodDerived(name: MethodName, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String) {
        val m = MethodDeclarationDerivedSimple(this, name, parameters, typeInstance, description, body)
        this.addMethod(m)
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

    override fun type(typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance {
        return namespace.createUnnamedSupertypeTypeInstance(this, typeArguments, nullable)
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
        val pd = PropertyDeclarationStored(this, name, typeInstance, characteristics + PropertyCharacteristic.STORED, propIndex)
        this.addProperty(pd)
        return pd
    }

}

class TypeArgumentNamedSimple(
    override val name: PropertyName,
    override val type: TypeInstance,
) : TypeArgumentNamed {
    override fun conformsTo(other: TypeArgument): Boolean = when {
        other is TypeArgumentNamed -> this.name == other.name && this.type.conformsTo(other.type)
        else -> this.type.conformsTo(other.type)
    }

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance {
        return type.resolved(resolvingTypeArguments)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String {
        return "${name.value}:${type.signature(context, currentDepth)}"
    }

    override fun toString(): String = "$name: ${type.signature(null, 0)}"
}

class TupleTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
    //val id: Int // must be public for serialisation
) : TypeDeclarationSimpleAbstract(), TupleType {

    override val typeParameters = listOf(TypeParameterMultiple)

    //override val entries get() = property.map { Pair(it.name, it.typeInstance) }

    override fun type(typeArguments: List<TypeArgument>, nullable: Boolean): TupleTypeInstance {
        return typeTuple(typeArguments.map { it as TypeArgumentNamed }, nullable)
    }

    override fun typeTuple(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance {
        return namespace.createTupleTypeInstance(typeArguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "${name}<${this.property.joinToString { it.name.value + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
    }

    override fun conformsTo(other: TypeDeclaration): Boolean = when {
        this === other -> true
        other == SimpleTypeModelStdLib.NothingType.declaration -> false
        other == SimpleTypeModelStdLib.AnyType.declaration -> true
        other is TupleType -> other.typeParameters.containsAll(this.typeParameters) //TODO: this should check conformance of property types! - could cause recursive loop!
        else -> false
    }

    override fun equalTo(other: TupleType): Boolean =
        this.typeParameters == other.typeParameters

    override fun asStringInContext(context: TypeNamespace): String = "tuple ${signature(context)}"

    //override fun hashCode(): Int = this.id
    //override fun equals(other: Any?): Boolean = when {
    //    other !is TupleTypeSimple -> false
    //    this.id != other.id -> false
    //    this.namespace != other.namespace -> false
    //    else -> true
    //}

    //override fun toString(): String = "${TupleType.NAME.value}-$id<${this.property.joinToString { it.name.value + ":" + it.typeInstance }}>"
}

class ValueTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : StructuredTypeSimpleAbstract(), ValueType {

    override val constructors: List<ConstructorDeclaration> = mutableListOf()

    override val valueProperty: PropertyDeclaration get() = when(constructors.size) {
        0 -> error("ValueType must have a constructor before its valuePropertyName can be read.")
        else -> when (constructors[0].parameters.size) {
            0 -> error("ValueType primary (first) constructor must have at least one parameter")
            1 -> this.findPropertyOrNull(PropertyName(constructors[0].parameters[0].name.value))
                ?: error("There is no property with name '${constructors[0].parameters[0].name.value}' corresponding the the primary constructor argument")
            else -> error("ValueType primary (first) constructor must have only one parameter")
        }
    }

    fun addConstructor(parameters: List<ParameterDeclaration>) {
        val cons = ConstructorDeclarationSimple(this, parameters)
        (constructors as MutableList).add(cons)

    }

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
                val chrs = it.characteristics.joinToString(prefix = "{", postfix = "}") { ch -> ch.asString() }
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

    //override var typeParameters = mutableListOf<SimpleName>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun addSubtype(typeInstance: TypeInstance) {
        this.subtypes.add(typeInstance)
    }

    override fun asStringInContext(context: TypeNamespace): String {
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val props = this.property
            .joinToString(separator = "\n    ") {
                val psig = it.typeInstance.signature(context, 0)
                val chrs = it.characteristics.joinToString(prefix = "{", postfix = "}") { ch -> ch.asString() }
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

    override val constructors: List<ConstructorDeclaration> = mutableListOf()
    //override var typeParameters = mutableListOf<SimpleName>()

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    fun addConstructor(parameters: List<ParameterDeclaration>) {
        val cons = ConstructorDeclarationSimple(this, parameters)
        (constructors as MutableList).add(cons)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun addSubtype_dep(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this, qualifiedTypeName, emptyList(), false)
        this.addSubtype(ti)
    }

    override fun addSubtype(typeInstance: TypeInstance) {
        if (this.subtypes.contains(typeInstance)) {
            //do not add it twice
        } else {
            this.subtypes.add(typeInstance)
        }
    }

    override fun asStringInContext(context: TypeNamespace): String {
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val props = this.property
            .joinToString(separator = "\n    ") {
                val psig = it.typeInstance.signature(context, 0)
                val chrs = it.characteristics.joinToString(prefix = "{", postfix = "}") { ch -> ch.asString() }
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
    override var typeParameters: List<TypeParameter> = mutableListOf()
) : StructuredTypeSimpleAbstract(), CollectionType {

    override val isStdList: Boolean get() = this == SimpleTypeModelStdLib.List
    override val isStdSet: Boolean get() = this == SimpleTypeModelStdLib.Set
    override val isStdMap: Boolean get() = this == SimpleTypeModelStdLib.Map

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun asStringInContext(context: TypeNamespace): String = "collection ${signature(context)}"
}

class ConstructorDeclarationSimple(
    override val owner: TypeDeclaration,
    override val parameters: List<ParameterDeclaration>
) : ConstructorDeclaration {

}

abstract class PropertyDeclarationAbstract() : PropertyDeclaration {

    /**
     * information about this property
     */
    override var metaInfo = mutableMapOf<String, String>()

    override val isConstructor: Boolean get() = characteristics.contains(PropertyCharacteristic.CONSTRUCTOR)
    override val isIdentity: Boolean get() = characteristics.contains(PropertyCharacteristic.IDENTITY)

    override val isComposite: Boolean get() = characteristics.contains(PropertyCharacteristic.COMPOSITE)
    override val isReference: Boolean get() = characteristics.contains(PropertyCharacteristic.REFERENCE)

    override val isReadOnly: Boolean get() = characteristics.contains(PropertyCharacteristic.READ_ONLY)
    override val isReadWrite: Boolean get() = characteristics.contains(PropertyCharacteristic.READ_WRITE)

    override val isDerived: Boolean get() = characteristics.contains(PropertyCharacteristic.DERIVED)
    override val isStored: Boolean get() = characteristics.contains(PropertyCharacteristic.STORED)
    override val isPrimitive: Boolean get() = characteristics.contains(PropertyCharacteristic.PRIMITIVE)

    override fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): PropertyDeclarationResolved = PropertyDeclarationResolvedSimple(
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
        return "${owner.name}.$name: ${typeInstance.signature(this.owner.namespace,0)}$nullable [$index]$chrsStr"
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

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.PRIMITIVE)

}

class PropertyDeclarationDerived(
    override val owner: TypeDeclaration,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val description: String,
    val expression: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.DERIVED)

}

class PropertyDeclarationResolvedSimple(
    override val owner: TypeDeclaration,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>,
    override val description: String
) : PropertyDeclarationAbstract(), PropertyDeclarationResolved {
    override val index: Int get() = -1 // should never be included in owners list
}

abstract class MethodDeclarationAbstract() : MethodDeclaration {
    override fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): MethodDeclarationResolved = MethodDeclarationResolvedSimple(
        this.owner,
        this.name,
        this.parameters.map {
            ParameterDefinitionSimple(it.name, it.typeInstance.resolved(typeArguments),it.defaultValue)
        },
        this.returnType.resolved(typeArguments),
        this.description
    )
}

internal class MethodDeclarationPrimitiveSimple(
    override val owner: TypeDeclaration,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val returnType: TypeInstance,
    override val description: String
) : MethodDeclarationAbstract(), MethodDeclarationPrimitive {

}

class MethodDeclarationDerivedSimple(
    override val owner: TypeDeclaration,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val returnType: TypeInstance,
    override val description: String,
    val body: String
) : MethodDeclarationAbstract(), MethodDeclarationDerived {

}

class MethodDeclarationResolvedSimple(
    override val owner: TypeDeclaration,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val returnType: TypeInstance,
    override val description: String
) : MethodDeclarationAbstract(), MethodDeclarationResolved {

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