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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.*
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.builder.typeModel
import net.akehurst.language.util.cached

class TypeModelSimple(
    override val name: SimpleName,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
) : TypeModelSimpleAbstract() {

    companion object {
        fun fromString(name: SimpleName, context: ContextWithScope<Any, Any>, typesString: TypesString): ProcessResult<TypeModel> {
            return when {
                typesString.value.isBlank() -> ProcessResultDefault(typeModel(name.value, true) { }, IssueHolder(LanguageProcessorPhase.ALL))
                else -> {
                    val proc = Agl.registry.agl.types.processor ?: error("Types language not found!")
                    proc.process(
                        sentence = typesString.value,
                        options = Agl.options { semanticAnalysis { context(context) } }
                    )
                }
            }
        }
    }

}

// TODO: extend ModelAbstract !
abstract class TypeModelSimpleAbstract() : TypeModel {

    override val AnyType: TypeDefinition get() = StdLibDefault.AnyType.resolvedDeclaration //TODO: stdLib not necessarily part of model !
    override val NothingType: TypeDefinition get() = StdLibDefault.NothingType.resolvedDeclaration //TODO: stdLib not necessarily part of model !

    // stored in namespace so deserialisation works
    private val _namespace = cached {
        namespace.associateBy { it.qualifiedName }
    }

    //store this separately to keep order of namespaces - important for lookup of types
    override val namespace: List<TypeNamespace> = mutableListOf<TypeNamespace>()

    override fun resolveImports() {
        namespace.forEach { it.resolveImports(this as Model<Namespace<TypeDefinition>, TypeDefinition>) } //TODO
    }

    override fun addNamespace(value: TypeNamespace) {
        if (_namespace.value.containsKey(value.qualifiedName)) {
            if (_namespace.value[value.qualifiedName] === value) {
                //same object, no need to add it
            } else {
                error("TypeModel '${this.name}' already contains a namespace '${value.qualifiedName}'")
            }
        } else {
            (namespace as MutableList).add(value as TypeNamespace)
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

    override fun findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName: PossiblyQualifiedName): TypeDefinition? {
        return when (typeName) {
            is QualifiedName -> findNamespaceOrNull(typeName.front)?.findOwnedTypeNamed(typeName.last)
            is SimpleName -> findFirstDefinitionByNameOrNull(typeName)
        }
    }

    override fun findFirstDefinitionByNameOrNull(typeName: SimpleName): TypeDefinition? {
        for (ns in namespace) {
            val t = ns.findOwnedTypeNamed(typeName)
            if (null != t) {
                return t
            }
        }
        return null
    }

    override fun findByQualifiedNameOrNull(qualifiedName: QualifiedName): TypeDefinition? {
        val nsn = qualifiedName.front
        val tn = qualifiedName.last
        return _namespace.value[nsn]?.findOwnedTypeNamed(tn)
    }

    override fun addAllNamespaceAndResolveImports(namespaces: Iterable<TypeNamespace>) {
        namespaces.forEach { this.addNamespace(it) }
        this.resolveImports()
    }

    // --- DefinitionBlock ---
    override val allDefinitions: List<TypeDefinition> get() = namespace.flatMap { it.definition }

    override val isEmpty: Boolean get() = namespace.isEmpty()

    override fun findNamespaceOrNull(qualifiedName: QualifiedName): TypeNamespace? = _namespace.value[qualifiedName]

    override fun findDefinitionByQualifiedNameOrNull(qualifiedName: QualifiedName): TypeDefinition? {
        TODO("not implemented")
    }

    override fun resolveReference(reference: DefinitionReference<TypeDefinition>): TypeDefinition? {
        TODO("not implemented")
    }

    // -- Formatable ---
    override fun asString(indent: Indent): String {
        val ns = this.namespace
            .filterNot { it == StdLibDefault } // Don't show the stdlib details
            .sortedBy { it.qualifiedName.value }
            .joinToString(separator = "\n") { it.asString() }
        return "$ns"
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
            val superProps = resolvedDeclaration.supertypes.map {
                val resIt = it.resolved(typeArgMap)
                resIt.allResolvedProperty
            }.fold(mapOf<PropertyName, PropertyDeclarationResolved>()) { acc, it -> acc + it }
            val ownResolveProps = resolvedDeclaration.property.associate {
                val rp = it.resolved(typeArgMap)
                Pair(it.name, rp)
            }
            return superProps + ownResolveProps
        }

    override val allResolvedMethod: Map<MethodName, MethodDeclarationResolved>
        get() {
            val typeArgMap = createTypeArgMap()
            val superProps = resolvedDeclaration.supertypes.map {
                val resIt = it.resolved(typeArgMap)
                resIt.allResolvedMethod
            }.fold(mapOf<MethodName, MethodDeclarationResolved>()) { acc, it -> acc + it }
            val ownResolveProps = resolvedDeclaration.method.associate {
                val rp = it.resolved(typeArgMap)
                Pair(it.name, rp)
            }
            return superProps + ownResolveProps
        }

    override val asTypeArgument: TypeArgument get() = TypeArgumentSimple(this)

    override fun notNullable() = this.resolvedDeclaration.type(typeArguments, false)
    override fun nullable() = this.resolvedDeclaration.type(typeArguments, true)

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
                    typeArguments.isEmpty() -> resolvedDeclarationOrNull?.signature(context, currentDepth + 1) ?: this.typeName.value
                    else -> resolvedDeclaration.name.value
                }
                return "${name}$args$n"
            }
        }
    }

    override fun conformsTo(other: TypeInstance): Boolean = when {
        other === this -> true // fast option
        this == StdLibDefault.NothingType -> other.isNullable
        other == StdLibDefault.NothingType -> false
        other == StdLibDefault.AnyType -> true
        this.resolvedDeclaration.conformsTo(other.resolvedDeclaration).not() -> false
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

    override fun commonSuperType(other: TypeInstance): TypeInstance = when {
        other.conformsTo(this) -> this
        this.conformsTo(other) -> other
        // TODO: look at supertypes
        else -> StdLibDefault.AnyType
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
        resolvedDeclarationOrNull?.typeParameters?.forEachIndexed { index, it ->
            val tp = it
            // maybe the type arg has not been explicitly provided, in which case provide AnyType
            val ta = this.typeArguments.getOrNull(index) ?: StdLibDefault.AnyType.asTypeArgument
            typeArgMap[tp] = when (ta) {
                is TypeArgument -> ta.type
                else -> error("Unsupported")
            }
        }
        return typeArgMap
    }
}

class TypeParameterReference(
    val context: TypeDefinition,
    val typeParameterName: SimpleName,
    override val isNullable: Boolean = false
) : TypeInstanceAbstract(), TypeInstance {

    override val namespace: TypeNamespace get() = context.namespace

    override val qualifiedTypeName: QualifiedName get() = context.qualifiedName.append(typeParameterName)

    override val typeName: SimpleName get() = typeParameterName

    override val typeArguments: List<TypeArgument> = emptyList()

    override val resolvedDeclarationOrNull: TypeDefinition? = null

    override val resolvedDeclaration: TypeDefinition get() = error("TypeParameterReference does not have a declaration")

    override fun conformsTo(other: TypeInstance): Boolean {
        TODO("not implemented")
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = "${typeParameterName.value}"

    override fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance =
        context.typeParameters.firstOrNull { it.name == typeParameterName }
            ?.let { resolvingTypeArguments[it] }
            ?: error("Cannot resolve TypeArgument named '$typeParameterName' in context of '${context.qualifiedName}'")

    override fun possiblyQualifiedNameInContext(context: TypeNamespace): PossiblyQualifiedName = typeParameterName

    override fun findInOrCloneTo(other: TypeModel): TypeInstance {
        return TypeParameterReference(
            this.context.findInOrCloneTo(other),
            this.typeParameterName,
            this.isNullable
        )
    }

    override fun hashCode(): Int = listOf(context, typeParameterName).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeParameterReference -> false
        this.context != other.context -> false
        this.typeParameterName != other.typeParameterName -> false
        else -> true
    }

    override fun toString(): String = "${typeParameterName.value}"
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

    override fun findInOrCloneTo(other: TypeModel): TypeArgument {
        val clonedType = type.findInOrCloneTo(other)
        // val clonedTargs = this.type.typeArguments.map { it.cloneTo(other) }
        //val clonedTi = clonedDecl.type(clonedTargs, this.type.isNullable)
        return TypeArgumentSimple(clonedType)
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
                        result.any { it.key.name == v.typeParameterName } -> result[k] = result[TypeParameterSimple(v.typeParameterName)]!!
                    }

                    else -> result[k] = v
                }
            }
            return result
        }
    }

    val context: TypeDefinition? get() = contextQualifiedTypeName?.let { namespace.findTypeNamed(it) }

    override val typeName: SimpleName
        get() = resolvedDeclarationOrNull?.name
            ?: qualifiedOrImportedTypeName.simpleName

    override val qualifiedTypeName: QualifiedName
        get() = resolvedDeclarationOrNull?.qualifiedName
            ?: when (qualifiedOrImportedTypeName) {
                is QualifiedName -> qualifiedOrImportedTypeName
                is SimpleName -> context?.namespace?.qualifiedName?.append(qualifiedOrImportedTypeName)
                    ?: namespace.findTypeNamed(qualifiedOrImportedTypeName)?.qualifiedName
            }
            ?: error("Cannot construct a Qualified name for '$qualifiedOrImportedTypeName' in context of '$contextQualifiedTypeName'")

    override val resolvedDeclarationOrNull: TypeDefinition? get() = namespace.findTypeNamed(qualifiedOrImportedTypeName)

    override val resolvedDeclaration: TypeDefinition
        get() = resolvedDeclarationOrNull
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
        this.resolvedDeclaration.namespace == context -> this.typeName
        else -> this.qualifiedTypeName
    }

    override fun findInOrCloneTo(other: TypeModel): TypeInstance {
        return TypeInstanceSimple(
            this.contextQualifiedTypeName,
            this.namespace.findInOrCloneTo(other),
            this.qualifiedOrImportedTypeName,
            this.typeArguments.map { it.findInOrCloneTo(other) },
            this.isNullable
        )
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

    override val typeName: SimpleName get() = resolvedDeclaration.name
    override val qualifiedTypeName: QualifiedName get() = resolvedDeclaration.qualifiedName
    override val resolvedDeclarationOrNull: TypeDefinition get() = resolvedDeclaration
    override val resolvedDeclaration: TypeDefinition = StdLibDefault.TupleType

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
        this.resolvedDeclaration.namespace == context -> this.typeName
        else -> this.qualifiedTypeName
    }

    override fun findInOrCloneTo(other: TypeModel): TypeInstance {
        return TupleTypeInstanceSimple(
            this.namespace.findInOrCloneTo(other),
            this.typeArguments.map { it.findInOrCloneTo(other) },
            this.isNullable
        )
    }

    override fun hashCode(): Int = listOf(resolvedDeclaration, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.resolvedDeclaration != other.resolvedDeclaration -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }

    override fun toString(): String {
        val args = typeArguments.joinToString(separator = ", ") { "${it.name}: ${it.type.signature(null, 0)}" }
        return "${typeName}<$args>"
    }
}

/*
class UnnamedSupertypeTypeInstance(
    override val namespace: TypeNamespace,
    override val declaration: UnionType,
    override val typeArguments: List<TypeArgument>,
    override val isNullable: Boolean
) : TypeInstanceAbstract() {

    override val typeName: SimpleName get() = UnionType.NAME.last
    override val qualifiedTypeName: QualifiedName get() = UnionType.NAME

    override val declarationOrNull: TypeDefinition get() = declaration

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

    override fun cloneTo(other: TypeModel): TypeInstance {
        return UnnamedSupertypeTypeInstance(
            this.namespace.cloneTo(other),
            this.declaration.cloneTo(other),
            typeArguments.map { it.cloneTo(other) },
            this.isNullable
        )
    }

    override fun hashCode(): Int = listOf(declaration, typeArguments, isNullable).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeInstanceSimple -> false
        this.declaration != other.declaration -> false
        this.isNullable != other.isNullable -> false
        this.typeArguments != other.typeArguments -> false
        else -> true
    }
}*/

class TypeNamespaceSimple(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : TypeNamespaceAbstract(options, import) {

    override fun findInOrCloneTo(other: TypeModel): TypeNamespace {
        return other.findNamespaceOrNull(this.qualifiedName)
            ?: run {
                TypeNamespaceSimple(
                    this.qualifiedName,
                    this.options.clone(other.options),
                    this.import
                ).also { other.addNamespace(it) }
            }
    }

}

abstract class TypeNamespaceAbstract(
    options: OptionHolder,
    import: List<Import>
) : TypeNamespace, NamespaceAbstract<TypeDefinition>(options, import) {

    private var _nextUnnamedSuperTypeTypeId = 0
    private val _unnamedSuperTypes = hashMapOf<List<TypeInstance>, UnionType>()

    private var _nextTupleTypeTypeId = 0

    // qualified namespace name -> TypeNamespace
    //private val _requiredNamespaces = mutableMapOf<QualifiedName, TypeNamespace?>()

    override val import: List<Import> = import.toMutableList()

    override val ownedTypesByName get() = super.definitionByName

    override val ownedTypes: Collection<TypeDefinition> get() = ownedTypesByName.values

    val ownedUnnamedSupertypeType = mutableListOf<UnionType>()

    val ownedTupleTypes = mutableListOf<TupleType>()

    override val singletonType: Set<SingletonType> get() = ownedTypesByName.values.filterIsInstance<SingletonType>().toSet()
    override val primitiveType: Set<PrimitiveType> get() = ownedTypesByName.values.filterIsInstance<PrimitiveType>().toSet()
    override val valueType: Set<ValueType> get() = ownedTypesByName.values.filterIsInstance<ValueType>().toSet()
    override val enumType: Set<EnumType> get() = ownedTypesByName.values.filterIsInstance<EnumType>().toSet()
    override val collectionType: Set<CollectionType> get() = ownedTypesByName.values.filterIsInstance<CollectionType>().toSet()
    override val interfaceType: Set<InterfaceType> get() = ownedTypesByName.values.filterIsInstance<InterfaceType>().toSet()
    override val dataType: Set<DataType> get() = ownedTypesByName.values.filterIsInstance<DataType>().toSet()

    override fun isImported(qualifiedNamespaceName: QualifiedName): Boolean = import.contains(Import(qualifiedNamespaceName.value))

    /*
        override fun addDefinition(decl: TypeDefinition) {
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
    */
    override fun findOwnedTypeNamed(typeName: SimpleName): TypeDefinition? =
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

    override fun findTypeNamed(qualifiedOrImportedTypeName: PossiblyQualifiedName): TypeDefinition? {
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
        }
    }

    override fun findOwnedUnnamedSupertypeTypeOrNull(subtypes: List<TypeInstance>): UnionType? = _unnamedSuperTypes[subtypes]

    @Deprecated("No longer needed")
    override fun findTupleTypeWithIdOrNull(id: Int): TupleType? = ownedTupleTypes.getOrNull(id)

    fun findOwnedSpecialTypeNamedOrNull(typeName: SimpleName): SpecialTypeSimple? = findOwnedTypeNamed(typeName) as SpecialTypeSimple?
    override fun findOwnedSingletonTypeNamedOrNull(typeName: SimpleName): SingletonType? = findOwnedTypeNamed(typeName) as SingletonType?
    override fun findOwnedPrimitiveTypeNamedOrNull(typeName: SimpleName): PrimitiveType? = findOwnedTypeNamed(typeName) as PrimitiveType?
    override fun findOwnedEnumTypeNamedOrNull(typeName: SimpleName): EnumType? = findOwnedTypeNamed(typeName) as EnumType?
    override fun findOwnedCollectionTypeNamedOrNull(typeName: SimpleName): CollectionType? = findOwnedTypeNamed(typeName) as CollectionType?
    override fun findOwnedValueTypeNamedOrNull(typeName: SimpleName): ValueType? = findOwnedTypeNamed(typeName) as ValueType?
    override fun findOwnedInterfaceTypeNamedOrNull(typeName: SimpleName): InterfaceType? = findOwnedTypeNamed(typeName) as InterfaceType?
    override fun findOwnedDataTypeNamedOrNull(typeName: SimpleName): DataType? = findOwnedTypeNamed(typeName) as DataType?
    override fun findOwnedUnionTypeNamedOrNull(typeName: SimpleName): UnionType? = findOwnedTypeNamed(typeName) as UnionType?

    override fun createOwnedDataTypeNamed(typeName: SimpleName): DataType = DataTypeSimple(this, typeName)

    fun findOwnedOrCreateSpecialTypeNamed(typeName: SimpleName): SpecialTypeSimple {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            SpecialTypeSimple(this, typeName)
        } else {
            existing as SpecialTypeSimple
        }
    }

    override fun findOwnedOrCreateSingletonTypeNamed(typeName: SimpleName): SingletonType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            SingletonTypeSimple(this, typeName)
        } else {
            existing as SingletonType
        }
    }

    override fun findOwnedOrCreatePrimitiveTypeNamed(typeName: SimpleName): PrimitiveType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            PrimitiveTypeSimple(this, typeName)
        } else {
            existing as PrimitiveType
        }
    }

    override fun findOwnedOrCreateEnumTypeNamed(typeName: SimpleName, literals: List<String>): EnumType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            EnumTypeSimple(this, typeName, literals)
        } else {
            existing as EnumType
        }
    }

    override fun findOwnedOrCreateCollectionTypeNamed(typeName: SimpleName): CollectionType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            CollectionTypeSimple(this, typeName)
        } else {
            existing as CollectionType
        }
    }

    override fun findOwnedOrCreateValueTypeNamed(typeName: SimpleName): ValueType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            ValueTypeSimple(this, typeName)
        } else {
            existing as ValueType
        }
    }

    override fun findOwnedOrCreateInterfaceTypeNamed(typeName: SimpleName): InterfaceType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            InterfaceTypeSimple(this, typeName)
        } else {
            existing as InterfaceType
        }
    }

    override fun findOwnedOrCreateDataTypeNamed(typeName: SimpleName): DataType =
        findOwnedDataTypeNamedOrNull(typeName) ?: createOwnedDataTypeNamed(typeName)

    override fun findOwnedOrCreateUnionTypeNamed(typeName: SimpleName, ifCreate: (UnionType) -> Unit): UnionType {
        val existing = findOwnedTypeNamed(typeName)
        return if (null == existing) {
            UnionTypeSimple(this, typeName).also(ifCreate)
        } else {
            existing as UnionType
        }
    }

    override fun createTupleType(): TupleType {
        return StdLibDefault.TupleType
        //val td = TupleTypeSimple(this, _nextTupleTypeTypeId++)
        //ownedTupleTypes.add(td) //FIXME: don't think this is needed
        //return td
    }

    override fun createTypeInstance(
        contextQualifiedTypeName: QualifiedName?,
        qualifiedOrImportedTypeName: PossiblyQualifiedName,
        typeArguments: List<TypeArgument>,
        isNullable: Boolean
    ): TypeInstance {
        return TypeInstanceSimple(contextQualifiedTypeName, this, qualifiedOrImportedTypeName, typeArguments, isNullable)
    }

    override fun createTupleTypeInstance(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance {
        return TupleTypeInstanceSimple(this, typeArguments, nullable)
    }

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        val types = this.ownedTypesByName.entries //.sortedBy { it.key.value }
            .sortedWith(compareBy<Map.Entry<SimpleName, TypeDefinition>> { it.value::class.simpleName }.thenBy { it.value.name.value })
            .joinToString(prefix = "  ", separator = "\n  ") { it.value.asStringInContext(this) }
        val importstr = this.import.joinToString(prefix = "  ", separator = "\n  ") { "import ${it}" }
        val s = """namespace $qualifiedName
$importstr
$types
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

class TypeParameterSimple(
    override val name: SimpleName
) : TypeParameter {

    override fun clone(): TypeParameter = TypeParameterSimple(name)

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypeParameter -> false
        other.name != this.name -> false
        else -> true
    }

    override fun toString(): String = name.value
}

object TypeParameterMultiple : TypeParameter {
    override val name = SimpleName("...")

    override fun clone(): TypeParameter = this
}

abstract class TypeDefinitionSimpleAbstract(
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap())
) : TypeDefinition {
    companion object {
        const val maxDepth = 10
    }

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val supertypes: List<TypeInstance> = mutableListOf()
    override val subtypes: List<TypeInstance> = emptyList()

    override val typeParameters: List<TypeParameter> = mutableListOf() //make implementation mutable for serialisation

    // store properties by map(index) rather than list(index), because when constructing from grammar, not every index is used
    // public, so it can be serialised
    val propertyByIndex = mutableMapOf<Int, PropertyDeclaration>()

    override val property get() = propertyByIndex.values.toList() //should be in order because mutableMap is LinkedHashMap by default
    //protected val properties = mutableListOf<PropertyDeclaration>()

    override val method = mutableListOf<MethodDeclaration>()

    override val allSuperTypes: List<TypeInstance> get() = supertypes + supertypes.flatMap { (it.resolvedDeclaration as DataType).allSuperTypes }

    override val allProperty: Map<PropertyName, PropertyDeclaration>
        get() = supertypes.flatMap {
            it.resolvedDeclaration.allProperty.values
        }.associateBy { it.name } + this.property.associateBy { it.name }

    override val allMethod: Map<MethodName, MethodDeclaration>
        get() = supertypes.flatMap {
            it.resolvedDeclaration.allMethod.values
        }.associateBy { it.name } + this.method.associateBy { it.name }

    /**
     * information about this type
     */
    override var metaInfo = mutableMapOf<String, String>()

    override fun type(typeArguments: List<TypeArgument>, isNullable: Boolean): TypeInstance =
        namespace.createTypeInstance(this.qualifiedName, this.name, typeArguments, isNullable)

    override fun conformsTo(other: TypeDefinition): Boolean = when {
        other === this -> true // fast option
        this == StdLibDefault.NothingType.resolvedDeclaration -> false
        other == StdLibDefault.NothingType.resolvedDeclaration -> false
        other == StdLibDefault.AnyType.resolvedDeclaration -> true
        other == this -> true
        other is UnionType -> other.alternatives.any { this.conformsTo(it.resolvedDeclaration) }
        else -> this.supertypes.any { it.resolvedDeclaration.conformsTo(other) }
    }

    override fun getOwnedPropertyByIndexOrNull(i: Int): PropertyDeclaration? = propertyByIndex[i]

    override fun findOwnedPropertyOrNull(name: PropertyName): PropertyDeclaration? =
        this.property.firstOrNull { it.name == name }

    override fun findAllPropertyOrNull(name: PropertyName): PropertyDeclaration? =
        this.allProperty[name]

    override fun findOwnedMethodOrNull(name: MethodName): MethodDeclaration? =
        this.method.firstOrNull { it.name == name }

    override fun findAllMethodOrNull(name: MethodName): MethodDeclaration? =
        this.allMethod[name]

    // --- mutable ---
    override fun addTypeParameter(name: TypeParameter) {
        (this.typeParameters as MutableList).add(name)
    }

    override fun addSupertype_dep(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this.qualifiedName, qualifiedTypeName, emptyList(), false)
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
    override fun appendPropertyDerived(name: PropertyName, typeInstance: TypeInstance, description: String, expression: String): PropertyDeclarationDerived {
        val propIndex = property.size
        val prop = PropertyDeclarationDerived(this, name, typeInstance, description, expression, propIndex)
        this.addProperty(prop)
        return prop
    }

    override fun appendPropertyPrimitive(name: PropertyName, typeInstance: TypeInstance, description: String): PropertyDeclarationPrimitive {
        val propIndex = property.size
        val prop = PropertyDeclarationPrimitive(this, name, typeInstance, description, propIndex)
        this.addProperty(prop)
        return prop
    }

    override fun appendMethodPrimitive(
        name: MethodName,
        parameters: List<ParameterDeclaration>,
        returnType: TypeInstance,
        description: String
    ): MethodDeclarationPrimitive {
        return MethodDeclarationPrimitiveSimple(this, name, parameters, returnType, description)
    }

    /**
     * append a method/function with the expression that should execute
     */
    override fun appendMethodDerived(name: MethodName, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String): MethodDeclarationDerived {
        return MethodDeclarationDerivedSimple(this, name, parameters, typeInstance, description, body)
    }

    override fun asStringInContext(context: TypeNamespace): String = signature(context, 0)

    // --- Formatable ---
    override fun asString(indent: Indent): String {
        TODO("not implemented")
    }

    // --- Any ---
    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is TypeDefinition -> false
        else -> this.qualifiedName == other.qualifiedName
    }

    override fun toString(): String = qualifiedName.value

    // --- Implementation

    protected fun findInOrCloneTo(other: TypeModel, clone: TypeDefinitionSimpleAbstract) {
        this.supertypes.forEach { clone.addSupertype(it.findInOrCloneTo(other)) }
        this.typeParameters.forEach { clone.addTypeParameter(it.clone()) }
        this.property.forEach { it.findInOrCloneTo(other) }
        this.method.forEach { it.findInOrCloneTo(other) }
        clone.metaInfo.putAll(this.metaInfo)
    }

    fun addProperty(propertyDeclaration: PropertyDeclaration) {
        check(this.findOwnedPropertyOrNull(propertyDeclaration.name) == null) { "TypeDefinition '${this.qualifiedName}' already owns a property named '${propertyDeclaration.name}'" }
        this.propertyByIndex[propertyDeclaration.index] = propertyDeclaration
        //this.property[propertyDeclaration.name] = propertyDeclaration
    }

    fun addMethod(methodDeclaration: MethodDeclaration) {
        this.method.add(methodDeclaration)
        //this.method[methodDeclaration.name] = methodDeclaration
    }
}

class SpecialTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : TypeDefinitionSimpleAbstract(), SpecialType {

    init {
        namespace.addDefinition(this)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): SpecialTypeSimple =
        (namespace.findInOrCloneTo(other) as TypeNamespaceAbstract).findOwnedSpecialTypeNamedOrNull(this.name)
            ?: run {
                (namespace.findInOrCloneTo(other) as TypeNamespaceAbstract).findOwnedOrCreateSpecialTypeNamed(this.name) //FIXME: createOwnedSpecialType
                    .also { clone -> super.findInOrCloneTo(other, clone) }
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
) : TypeDefinitionSimpleAbstract(), SingletonType {

    init {
        namespace.addDefinition(this)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): SingletonType =
        namespace.findInOrCloneTo(other).findOwnedSingletonTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).findOwnedOrCreateSingletonTypeNamed(this.name) //FIXME: createOwnedSingletonType
                    .also { clone -> super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract) }
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
) : TypeDefinitionSimpleAbstract(), PrimitiveType {

    init {
        namespace.addDefinition(this)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): PrimitiveType =
        namespace.findInOrCloneTo(other).findOwnedPrimitiveTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).findOwnedOrCreatePrimitiveTypeNamed(this.name) //FIXME: create
                    .also { clone -> super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract) }
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
) : TypeDefinitionSimpleAbstract(), EnumType {

    init {
        namespace.addDefinition(this)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): EnumType =
        namespace.findInOrCloneTo(other).findOwnedEnumTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).findOwnedOrCreateEnumTypeNamed(this.name, this.literals)
                    .also { clone -> super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract) }
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

class UnionTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
) : TypeDefinitionSimpleAbstract(), UnionType {

    override val alternatives = mutableListOf<TypeInstance>()

    init {
        namespace.addDefinition(this)
    }

    override fun addAlternative(value: TypeInstance) {
        alternatives.add(value)
    }
    /*
    override fun type(typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance {
        return namespace.createUnnamedSupertypeTypeInstance(this, typeArguments, nullable)
    }*/

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun conformsTo(other: TypeDefinition): Boolean = when {
        this === other -> true
        other == StdLibDefault.NothingType.resolvedDeclaration -> false
        other == StdLibDefault.AnyType.resolvedDeclaration -> true
        other is UnionType -> this.alternatives == other.alternatives
        else -> false
    }

    override fun findInOrCloneTo(other: TypeModel): UnionType =
        namespace.findInOrCloneTo(other).findOwnedUnionTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).findOwnedOrCreateUnionTypeNamed(this.name) { ut ->
                    this.alternatives.forEach { ut.addAlternative(it.findInOrCloneTo(other)) }
                }.also { clone ->
                    super.findInOrCloneTo(other, clone as UnionTypeSimple)
                }
            }

    override fun asStringInContext(context: TypeNamespace): String {
        val alts = alternatives.joinToString(separator = " | ") { it.signature(context, 0) }
        return when {
            alts.length < 80 -> "union ${signature(context)} { $alts }"
            else -> {
                val altsnl = alternatives.joinToString(separator = "\n    | ") { it.signature(context, 0) }
                "union ${signature(context)} {\n   ${altsnl}\n  }"
            }
        }

    }

    override fun hashCode(): Int = qualifiedName.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is UnionType -> false
        this.qualifiedName != other.qualifiedName -> false
        else -> true
    }
}

abstract class StructuredTypeSimpleAbstract : TypeDefinitionSimpleAbstract(), StructuredType {

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

    override fun findInOrCloneTo(other: TypeModel): TypeArgumentNamed {
        return TypeArgumentNamedSimple(
            this.name,
            this.type.findInOrCloneTo(other)
        )
    }

    override fun toString(): String = "$name: ${type.signature(null, 0)}"
}

class TupleTypeSimple(
    override val namespace: TypeNamespace,
    override val name: SimpleName
    //val id: Int // must be public for serialisation
) : TypeDefinitionSimpleAbstract(), TupleType {

    override val typeParameters = listOf(TypeParameterMultiple)

    init {
        namespace.addDefinition(this)
    }

    override fun type(typeArguments: List<TypeArgument>, isNullable: Boolean): TupleTypeInstance {
        return typeTuple(typeArguments.map { it as TypeArgumentNamed }, isNullable)
    }

    override fun typeTuple(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance {
        return namespace.createTupleTypeInstance(typeArguments, nullable)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        currentDepth >= maxDepth -> "..."
        else -> "${name}<${this.property.joinToString { it.name.value + ":" + it.typeInstance.signature(context, currentDepth + 1) }}>"
    }

    override fun findInOrCloneTo(other: TypeModel): TupleType = StdLibDefault.TupleType

    override fun conformsTo(other: TypeDefinition): Boolean = when {
        this === other -> true
        other == StdLibDefault.NothingType.resolvedDeclaration -> false
        other == StdLibDefault.AnyType.resolvedDeclaration -> true
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

    init {
        namespace.addDefinition(this)
    }

    override val constructors: List<ConstructorDeclaration> = mutableListOf()

    override val valueProperty: PropertyDeclaration
        get() = when (constructors.size) {
            0 -> error("ValueType must have a constructor before its valuePropertyName can be read.")
            else -> when (constructors[0].parameters.size) {
                0 -> error("ValueType primary (first) constructor must have at least one parameter")
                1 -> this.findAllPropertyOrNull(PropertyName(constructors[0].parameters[0].name.value))
                    ?: error("There is no property with name '${constructors[0].parameters[0].name.value}' corresponding the the primary constructor argument")

                else -> error("ValueType primary (first) constructor must have only one parameter")
            }
        }

    override fun addConstructor(parameters: List<ParameterDeclaration>) {
        val cons = ConstructorDeclarationSimple(this, parameters)
        (constructors as MutableList).add(cons)

    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): ValueType =
        this.namespace.findInOrCloneTo(other).findOwnedValueTypeNamedOrNull(this.name)
            ?: run {
                this.namespace.findInOrCloneTo(other).findOwnedOrCreateValueTypeNamed(this.name)
                    .also { clone ->
                        super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract)
                        this.constructors.forEach { clone.addConstructor(it.parameters.map { it.findInOrCloneTo(other) }) }
                    }
            }

    override fun asStringInContext(context: TypeNamespace): String {
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val cargs = this.property
            .joinToString(separator = ", ") {
                val psig = it.typeInstance.signature(context, 0)
                val cmp_ref = it.characteristics
                    .filter { setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.REFERENCE).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val val_var = it.characteristics
                    .filter { setOf(PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.READ_ONLY).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val chrs = when {
                    cmp_ref.isBlank() -> val_var
                    else -> "$cmp_ref $val_var"
                }
                "$chrs ${it.name}:$psig"
            }

        val props = this.property
            .joinToString(separator = "\n    ") {
                val psig = it.typeInstance.signature(context, 0)
                val cmp_ref = it.characteristics
                    .filter { setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.REFERENCE).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val val_var = it.characteristics
                    .filter { setOf(PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.READ_ONLY).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val chrs = when {
                    cmp_ref.isBlank() -> val_var
                    else -> "$cmp_ref $val_var"
                }
                "$chrs ${it.name}:$psig"
            }
        return "value ${name}${sups} ( $cargs )"
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

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: MutableList<TypeInstance> = mutableListOf()

    init {
        namespace.addDefinition(this)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): InterfaceType =
        namespace.findInOrCloneTo(other).findOwnedInterfaceTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).findOwnedOrCreateInterfaceTypeNamed(this.name)
                    .also { clone ->
                        super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract)
                        this.subtypes.forEach { clone.addSubtype(it.findInOrCloneTo(other)) }
                    }
            }

    override fun addSubtype(typeInstance: TypeInstance) {
        this.subtypes.add(typeInstance)
    }

    override fun asStringInContext(context: TypeNamespace): String {
        val targs = if (this.typeParameters.isEmpty()) {
            ""
        } else {
            "<" + this.typeParameters.joinToString(","){it.name.value} + ">"
        }
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val props = this.property
            .joinToString(separator = "\n    ") {
                val psig = it.typeInstance.signature(context, 0)
                val cmp_ref = it.characteristics
                    .filter { setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.REFERENCE).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val val_var = it.characteristics
                    .filter { setOf(PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.READ_ONLY).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val chrs = when {
                    cmp_ref.isBlank() -> val_var
                    else -> "$cmp_ref $val_var"
                }
                "$chrs ${it.name}:$psig"
            }
        return "interface ${name}${targs}${sups} {\n    $props\n  }"
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

    init {
        namespace.addDefinition(this)
    }

    override fun addConstructor(parameters: List<ParameterDeclaration>) {
        val cons = ConstructorDeclarationSimple(this, parameters)
        (constructors as MutableList).add(cons)
    }

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): DataType =
        namespace.findInOrCloneTo(other).findOwnedDataTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).createOwnedDataTypeNamed(this.name)
                    .also { clone ->
                        super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract)
                        this.constructors.forEach { clone.addConstructor(it.parameters.map { it.findInOrCloneTo(other) }) }
                        this.subtypes.forEach { clone.addSubtype(it.findInOrCloneTo(other)) }
                    }
            }

    @Deprecated("Create a TypeInstance and use addSubtype(TypeInstance)")
    override fun addSubtype_dep(qualifiedTypeName: PossiblyQualifiedName) {
        val ti = namespace.createTypeInstance(this.qualifiedName, qualifiedTypeName, emptyList(), false)
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
        val targs = if (this.typeParameters.isEmpty()) {
            ""
        } else {
            "<" + this.typeParameters.joinToString(","){it.name.value} + ">"
        }
        val sups = if (this.supertypes.isEmpty()) "" else " : " + this.supertypes.sortedBy { it.signature(context, 0) }.joinToString { it.signature(context, 0) }
        val props = this.property
            .joinToString(separator = "\n    ") {
                val psig = it.typeInstance.signature(context, 0)
                val cmp_ref = it.characteristics
                    .filter { setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.REFERENCE).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val val_var = it.characteristics
                    .filter { setOf(PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.READ_ONLY).contains(it) }
                    .joinToString(separator = " ") { ch -> ch.asString() }
                val chrs = when {
                    cmp_ref.isBlank() -> val_var
                    else -> "$cmp_ref $val_var"
                }
                "$chrs ${it.name}: $psig"
            }
        return "data ${name}${targs}${sups} {\n    $props\n  }"
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

    init {
        namespace.addDefinition(this)
    }

    override val isStdList: Boolean get() = this == StdLibDefault.List
    override val isStdSet: Boolean get() = this == StdLibDefault.Set
    override val isStdMap: Boolean get() = this == StdLibDefault.Map

    override fun signature(context: TypeNamespace?, currentDepth: Int): String = when {
        null == context -> qualifiedName.value
        context == this.namespace -> name.value
        context.isImported(this.namespace.qualifiedName) -> name.value
        else -> qualifiedName.value
    }

    override fun findInOrCloneTo(other: TypeModel): CollectionType =
        namespace.findInOrCloneTo(other).findOwnedCollectionTypeNamedOrNull(this.name)
            ?: run {
                namespace.findInOrCloneTo(other).findOwnedOrCreateCollectionTypeNamed(this.name)
                    .also { clone -> super.findInOrCloneTo(other, clone as TypeDefinitionSimpleAbstract) }
            }

    override fun asStringInContext(context: TypeNamespace): String {
        val targs = if (this.typeParameters.isEmpty()) {
            ""
        } else {
            "<" + this.typeParameters.joinToString(","){it.name.value} + ">"
        }
        return "collection ${signature(context)}$targs"
    }
}

class ConstructorDeclarationSimple(
    override val owner: TypeDefinition,
    override val parameters: List<ParameterDeclaration>
) : ConstructorDeclaration {

    override fun findInOrCloneTo(other: TypeModel): ConstructorDeclaration =
        ConstructorDeclarationSimple(
            this.owner.findInOrCloneTo(other),
            this.parameters.map { it.findInOrCloneTo(other) }
        )
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
        this,
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
        return "${owner.name}.$name: ${typeInstance.signature(this.owner.namespace, 0)}$nullable [$index]$chrsStr"
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

    override fun findInOrCloneTo(other: TypeModel): PropertyDeclaration =
        owner.findInOrCloneTo(other).findOwnedPropertyOrNull(this.name)
            ?: run {
                owner.findInOrCloneTo(other).appendPropertyStored(
                    this.name,
                    this.typeInstance.findInOrCloneTo(other),
                    this.characteristics,
                    this.index
                )
            }
}

/**
 * A Property whose value is computed using built-in computation,
 * it is 'Primitive' in the same sense that 'Primitive' types are based on built-in constructs.
 */
class PropertyDeclarationPrimitive(
    override val owner: TypeDefinition,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val description: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.PRIMITIVE)

    override fun findInOrCloneTo(other: TypeModel): PropertyDeclaration =
        owner.findInOrCloneTo(other).findOwnedPropertyOrNull(this.name)
            ?: run {
                owner.findInOrCloneTo(other).appendPropertyPrimitive(
                    this.name,
                    this.typeInstance.findInOrCloneTo(other),
                    this.description
                )
            }
}

class PropertyDeclarationDerived(
    override val owner: TypeDefinition,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val description: String,
    val expression: String,
    override val index: Int // Not really needed except that its used as part of storing property decls in the type that owns them
) : PropertyDeclarationAbstract() {

    override val characteristics: Set<PropertyCharacteristic> get() = setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.DERIVED)

    override fun findInOrCloneTo(other: TypeModel): PropertyDeclaration =
        owner.findInOrCloneTo(other).findOwnedPropertyOrNull(this.name)
            ?: run {
                owner.findInOrCloneTo(other).appendPropertyDerived(
                    this.name,
                    this.typeInstance.findInOrCloneTo(other),
                    this.description,
                    this.expression, //no need to clone this !
                )
            }
}

class PropertyDeclarationResolvedSimple(
    override val original: PropertyDeclaration,
    override val owner: TypeDefinition,
    override val name: PropertyName,
    override val typeInstance: TypeInstance,
    override val characteristics: Set<PropertyCharacteristic>,
    override val description: String
) : PropertyDeclarationAbstract(), PropertyDeclarationResolved {
    override val index: Int get() = -1 // should never be included in owners list

    override fun findInOrCloneTo(other: TypeModel): PropertyDeclaration =
        owner.findInOrCloneTo(other).findOwnedPropertyOrNull(this.name)
            ?: run {
                PropertyDeclarationResolvedSimple(
                    this.original.findInOrCloneTo(other),
                    this.owner.findInOrCloneTo(other),
                    this.name,
                    this.typeInstance.findInOrCloneTo(other),
                    this.characteristics,
                    this.description
                )
            }
}

abstract class MethodDeclarationAbstract() : MethodDeclaration {
    override fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): MethodDeclarationResolved = MethodDeclarationResolvedSimple(
        this,
        this.owner,
        this.name,
        this.parameters.map {
            ParameterDefinitionSimple(it.name, it.typeInstance.resolved(typeArguments), it.defaultValue)
        },
        this.returnType.resolved(typeArguments),
        this.description
    )
}

internal class MethodDeclarationPrimitiveSimple(
    override val owner: TypeDefinition,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val returnType: TypeInstance,
    override val description: String
) : MethodDeclarationAbstract(), MethodDeclarationPrimitive {
    init {
        (owner as TypeDefinitionSimpleAbstract).addMethod(this)
    }

    override fun findInOrCloneTo(other: TypeModel): MethodDeclarationPrimitive =
        owner.findInOrCloneTo(other).findOwnedMethodOrNull(this.name) as MethodDeclarationPrimitive?
            ?: run {
                owner.findInOrCloneTo(other).appendMethodPrimitive(
                    this.name,
                    this.parameters.map { it.findInOrCloneTo(other) },
                    this.returnType.findInOrCloneTo(other),
                    this.description
                )
            }
}

class MethodDeclarationDerivedSimple(
    override val owner: TypeDefinition,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val returnType: TypeInstance,
    override val description: String,
    val body: String
) : MethodDeclarationAbstract(), MethodDeclarationDerived {
    init {
        (owner as TypeDefinitionSimpleAbstract).addMethod(this)
    }

    override fun findInOrCloneTo(other: TypeModel): MethodDeclarationDerived =
        owner.findInOrCloneTo(other).findOwnedMethodOrNull(this.name) as MethodDeclarationDerived?
            ?: run {
                owner.findInOrCloneTo(other).appendMethodDerived(
                    this.name,
                    this.parameters.map { it.findInOrCloneTo(other) },
                    this.returnType.findInOrCloneTo(other),
                    this.description,
                    this.body
                )
            }
}

class MethodDeclarationResolvedSimple(
    override val original: MethodDeclaration,
    override val owner: TypeDefinition,
    override val name: MethodName,
    override val parameters: List<ParameterDeclaration>,
    override val returnType: TypeInstance,
    override val description: String
) : MethodDeclarationAbstract(), MethodDeclarationResolved {
    //init {
    //    (owner as TypeDefinitionSimpleAbstract).addMethod(this)
    //}

    override fun findInOrCloneTo(other: TypeModel): MethodDeclaration =
        this.owner.findInOrCloneTo(other).findOwnedMethodOrNull(this.name)
            ?: run {
                MethodDeclarationResolvedSimple(
                    this.original.findInOrCloneTo(other),
                    owner.findInOrCloneTo(other),
                    this.name,
                    this.parameters.map { it.findInOrCloneTo(other) },
                    this.returnType.findInOrCloneTo(other),
                    this.description
                )
            }

}

class ParameterDefinitionSimple(
    override val name: net.akehurst.language.typemodel.api.ParameterName,
    override val typeInstance: TypeInstance,
    override val defaultValue: String?
) : ParameterDeclaration {

    override fun findInOrCloneTo(other: TypeModel): ParameterDeclaration =
        ParameterDefinitionSimple(
            this.name,
            this.typeInstance.findInOrCloneTo(other),
            this.defaultValue
        )

    override fun hashCode(): Int = listOf(name).hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is ParameterDeclaration -> false
        this.name != other.name -> false
        else -> true
    }

    override fun toString(): String = "${name}: ${typeInstance}${if (null != defaultValue) " = $defaultValue" else ""}"
}