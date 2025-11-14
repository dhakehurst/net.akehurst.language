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

package net.akehurst.language.types.api

import net.akehurst.language.base.api.*

interface TypesDomain : Domain<TypesNamespace, TypeDefinition> {

    val AnyType: TypeDefinition
    val NothingType: TypeDefinition

    fun resolveImports()

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TypesNamespace

    fun findByQualifiedNameOrNull(qualifiedName: QualifiedName): TypeDefinition?

    fun addAllNamespaceAndResolveImports(namespaces: Iterable<TypesNamespace>)

    // --- DefinitionBlock ---
    override fun findNamespaceOrNull(qualifiedName: QualifiedName): TypesNamespace?

}

data class AssociationEnd(
    val endName: PropertyName,
    val endType: DataType,
    val isNullable: Boolean,
    val collectionTypeName: PossiblyQualifiedName?,
    val characteristics: Set<PropertyCharacteristic>,
    val navigable: Boolean,
)

interface TypesNamespace : Namespace<TypeDefinition> {

    /**
     * TypeDefinition.name --> TypeDefinition
     */
    val ownedTypesByName: Map<SimpleName, TypeDefinition>

    val ownedTypes: Collection<TypeDefinition>

    val singletonType: Set<SingletonType>

    val primitiveType: Set<PrimitiveType>

    val valueType: Set<ValueType>

    val enumType: Set<EnumType>

    val collectionType: Set<CollectionType>

    val interfaceType: Set<InterfaceType>

    val dataType: Set<DataType>

    fun isImported(qualifiedNamespaceName: QualifiedName): Boolean

    /**
     * find type in this namespace with given name
     */
    fun findOwnedTypeNamed(typeName: SimpleName): TypeDefinition?

    /**
     * find type in this namespace OR imports with given name
     */
    fun findTypeNamed(qualifiedOrImportedTypeName: PossiblyQualifiedName): TypeDefinition?
    fun findOwnedUnnamedSupertypeTypeOrNull(subtypes: List<TypeInstance>): UnionType?

    @Deprecated("No longer needed")
    fun findTupleTypeWithIdOrNull(id: Int): TupleType?

    fun findOwnedSingletonTypeNamedOrNull(typeName: SimpleName): SingletonType?
    fun findOwnedPrimitiveTypeNamedOrNull(typeName: SimpleName): PrimitiveType?
    fun findOwnedEnumTypeNamedOrNull(typeName: SimpleName): EnumType?
    fun findOwnedValueTypeNamedOrNull(typeName: SimpleName): ValueType?
    fun findOwnedInterfaceTypeNamedOrNull(typeName: SimpleName): InterfaceType?
    fun findOwnedDataTypeNamedOrNull(typeName: SimpleName): DataType?
    fun findOwnedCollectionTypeNamedOrNull(typeName: SimpleName): CollectionType?
    fun findOwnedUnionTypeNamedOrNull(typeName: SimpleName): UnionType?

    fun createOwnedDataTypeNamed(typeName: SimpleName): DataType

    fun findOwnedOrCreateSingletonTypeNamed(typeName: SimpleName): SingletonType
    fun findOwnedOrCreatePrimitiveTypeNamed(typeName: SimpleName): PrimitiveType
    fun findOwnedOrCreateEnumTypeNamed(typeName: SimpleName, literals: List<String>): EnumType
    fun findOwnedOrCreateValueTypeNamed(typeName: SimpleName): ValueType
    fun findOwnedOrCreateInterfaceTypeNamed(typeName: SimpleName): InterfaceType
    fun findOwnedOrCreateDataTypeNamed(typeName: SimpleName): DataType
    fun findOwnedOrCreateCollectionTypeNamed(typeName: SimpleName): CollectionType
    fun findOwnedOrCreateUnionTypeNamed(typeName: SimpleName, ifCreate: (UnionType) -> Unit): UnionType

    /**
     * creates properties in the types of each end that point to each other
     * ends must be 'DataType' types
     * TODO: support directionality and
     */
    fun findOrCreateAssociation(ends:List<AssociationEnd>): List<PropertyDeclaration>

    fun createTypeInstance(
        contextQualifiedTypeName: QualifiedName?, qualifiedOrImportedTypeName: PossiblyQualifiedName, typeArguments: List<TypeArgument> = emptyList(), isNullable: Boolean = false
    ): TypeInstance

    fun createTupleTypeInstance(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance
    //fun createUnnamedSupertypeTypeInstance(declaration: UnionType, typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance

    fun createTupleType(): TupleType

    /**
     * clone the namespace but not the content
     */
    fun findInOrCloneTo(other: TypesDomain): TypesNamespace

}

interface TypeParameter {
    /**
     * name for the type TypeParameter
     */
    val name: SimpleName

    fun clone(): TypeParameter
}

interface TypeArgument {
    val type: TypeInstance
    fun conformsTo(other: TypeArgument): Boolean
    fun signature(context: TypesNamespace?, currentDepth: Int): String
    fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance

    fun findInOrCloneTo(other: TypesDomain): TypeArgument
}

interface TypeInstance {
    val namespace: TypesNamespace

    val typeArguments: List<TypeArgument>

    val isNullable: Boolean

    /**
     * the name of the TypeDeclaration, if it is resolvable,
     * or the name the instance refers to (e.g. a type parameter)
     */
    val typeName: SimpleName

    val qualifiedTypeName: QualifiedName

    val isNothing: Boolean
    val isCollection: Boolean

    /**
     * {derived} type is resolved via the namespace
     */
    val resolvedDeclaration: TypeDefinition

    val resolvedDeclarationOrNull: TypeDefinition?

    /**
     * properties from this type, and all supertypes, with type parameters resolved
     */
    val allResolvedProperty: Map<PropertyName, PropertyDeclarationResolved>

    val allResolvedMethod: Map<MethodName, MethodDeclarationResolved>

    val asTypeArgument: TypeArgument

    fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance

    fun notNullable(): TypeInstance
    fun nullable(): TypeInstance

    fun signature(context: TypesNamespace?, currentDepth: Int): String

    /**
     * true if
     *  - this == other
     *  - other == Any (all types conformTo Any)
     *  - this.resolvedDeclaration conformsTo other.resolvedDeclaration
     *    && all this.typeArguments conformTo other.typeArguments
     */
    fun conformsTo(other: TypeInstance): Boolean

    fun commonSuperType(other: TypeInstance): TypeInstance
    fun possiblyQualifiedNameInContext(context: TypesNamespace): Any

    fun findInOrCloneTo(other: TypesDomain): TypeInstance
}

interface TypeArgumentNamed : TypeArgument {
    val name: PropertyName

    override fun findInOrCloneTo(other: TypesDomain): TypeArgumentNamed
}

interface TupleTypeInstance : TypeInstance {
    override val typeArguments: List<TypeArgumentNamed>
}

interface TypeDefinition : Definition<TypeDefinition> {
    override val namespace: TypesNamespace

    val supertypes: List<TypeInstance>
    val subtypes: List<TypeInstance>

    val typeParameters: List<TypeParameter>

    val property: List<PropertyDeclaration>
    val method: List<MethodDeclaration>

    /**
     * transitive closure of supertypes
     */
    val allSuperTypes: List<TypeInstance>

    /**
     * all properties from this and transitive closure of supertypes
     */
    val allProperty: Map<PropertyName, PropertyDeclaration>

    /**
     * all methods from this and transitive closure of supertypes
     */
    val allMethod: Map<MethodName, MethodDeclaration>

    /**
     * information about this type
     */
    val metaInfo: Map<String, String>

    fun signature(context: TypesNamespace?, currentDepth: Int = 0): String

    fun type(typeArguments: List<TypeArgument> = emptyList(), isNullable: Boolean = false): TypeInstance

    /**
     * true if
     *  - this == other
     *  - other == Any (all types conformTo Any)
     *  - other is Union && this conformsTo one of the alternatives
     *  - one of this.supertypes conformsTo other
     */
    fun conformsTo(other: TypeDefinition): Boolean

    fun getOwnedPropertyByIndexOrNull(i: Int): PropertyDeclaration?
    fun findOwnedPropertyOrNull(name: PropertyName): PropertyDeclaration?
    fun findAllPropertyOrNull(name: PropertyName): PropertyDeclaration?
    fun findOwnedMethodOrNull(name: MethodName): MethodDeclaration?
    fun findAllMethodOrNull(name: MethodName): MethodDeclaration?

    fun asStringInContext(context: TypesNamespace): String

    fun addTypeParameter(name: TypeParameter)

    @Deprecated("Create a TypeInstance and use addSupertype(TypeInstance). This (deprecated) method does not add TypeArgs to the supertype.")
    fun addSupertype_dep(qualifiedTypeName: PossiblyQualifiedName)

    fun addSupertype(typeInstance: TypeInstance)
    fun appendPropertyPrimitive(name: PropertyName, typeInstance: TypeInstance, description: String): PropertyDeclaration
    fun appendPropertyDerived(name: PropertyName, typeInstance: TypeInstance, description: String, expression: String): PropertyDeclaration
    fun appendMethodPrimitive(
        name: MethodName,
        parameters: List<ParameterDeclaration>,
        returnType: TypeInstance,
        description: String
    ): MethodDeclarationPrimitive

    fun appendMethodDerived(name: MethodName, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String): MethodDeclarationDerived

    fun findInOrCloneTo(other: TypesDomain): TypeDefinition
}

interface SpecialType : TypeDefinition {}

interface SingletonType : TypeDefinition {
}

interface PrimitiveType : TypeDefinition {
}

interface EnumType : TypeDefinition {
    val literals: List<String>
}

interface StructuredType : TypeDefinition {

    fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration>

    /**
     * append property at the next index
     */
    fun appendPropertyStored(name: PropertyName, typeInstance: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int = -1): PropertyDeclaration

    override fun findInOrCloneTo(other: TypesDomain): StructuredType
}

//interface TypeParameterVarArg : TypeParameter

interface TupleType : TypeDefinition {

    //override val typeParameters: List<NamedTypeParameter>

    override fun type(typeArguments: List<TypeArgument>, isNullable: Boolean): TupleTypeInstance

    fun typeTuple(typeArguments: List<TypeArgumentNamed>, nullable: Boolean = false): TupleTypeInstance

    // @KompositeProperty
    // val entries: List<Pair<PropertyName, TypeInstance>>

    /**
     * The compares two Tuple types by checking for the same name:Type of all entries.
     * The 'equals' method compares the namespace and id of the TupleType.
     */
    fun equalTo(other: TupleType): Boolean
}

interface ValueType : StructuredType {
    val constructors: List<ConstructorDeclaration>
    val valueProperty: PropertyDeclaration

    fun addConstructor(parameters: List<ParameterDeclaration>)
}

interface InterfaceType : StructuredType {
    override val subtypes: List<TypeInstance>

    fun addSubtype(typeInstance: TypeInstance)
}

interface DataType : StructuredType {

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    override val subtypes: List<TypeInstance>

    val constructors: List<ConstructorDeclaration>

    @Deprecated("Create a TypeInstance and use addSubtype(TypeInstance)")
    fun addSubtype_dep(qualifiedTypeName: PossiblyQualifiedName)

    fun addSubtype(typeInstance: TypeInstance)
    fun addConstructor(parameters: List<ParameterDeclaration>)
}

interface UnionType : TypeDefinition {

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val alternatives: List<TypeInstance>

    fun addAlternative(value: TypeInstance)

    override fun findInOrCloneTo(other: TypesDomain): UnionType
}

interface CollectionType : StructuredType {
    val isStdList: Boolean
    val isStdSet: Boolean
    val isStdMap: Boolean
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class PropertyName(override val value: String) : PublicValueType {
    override fun toString(): String = value
}

interface PropertyDeclaration {
    val owner: TypeDefinition
    val name: PropertyName

    val typeInstance: TypeInstance
    val characteristics: Set<PropertyCharacteristic>
    val description: String

    // Important: indicates the child number in an SPPT,
    // assists SimpleAST generation,
    // indicates order of constructor params
    val index: Int

    /**
     * information about this property
     */
    val metaInfo: Map<String, String>

    val isReference: Boolean
    val isComposite: Boolean

    /*
     * a primary constructor parameter
     */
    val isConstructor: Boolean

    /**
     * value is considered to be part of the identity of the owning object,
     * unless otherwise indicated, the parameter is assumed to be a constructor parameter
     * must be read-only
     */
    val isIdentity: Boolean

    val isReadOnly: Boolean
    val isReadWrite: Boolean

    val isStored: Boolean
    val isDerived: Boolean
    val isPrimitive: Boolean

    // to assist execution by reflection without having MPP reflection support
    val execution: ((self:Any) -> Any?)?

    fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): PropertyDeclarationResolved

    fun findInOrCloneTo(other: TypesDomain): PropertyDeclaration
}

interface PropertyDeclarationResolved : PropertyDeclaration {
    val original: PropertyDeclaration
}

enum class PropertyCharacteristic {
    /**
     * value is a reference to an object of the given type
     */
    REFERENCE,

    /**
     * value is a composite part of instanceof owning type,
     * deletion of owning type will result in deletion of this value,
     * instance of the owning type is notionally the 'owner' of this value
     */
    COMPOSITE,

    /**
     * value is considered to be part of the identity of the owning object,
     * unless otherwise indicated, the parameter is assumed to be a constructor parameter
     */
    READ_ONLY,

    /**
     * property can be written and read
     */
    READ_WRITE,

    /**
     * property is stores a value
     */
    STORED,

    /**
     * property is derived, calculated by given expression, not stored
     */
    DERIVED,

    /**
     * property is primitive, with built-in calculation, not stored
     */
    PRIMITIVE,

    /**
     * property is a constructor parameter
     */
    CONSTRUCTOR,

    /**
     * property is a constructor parameter
     */
    IDENTITY
    ;

    fun asString(indent: Indent = Indent()) = when (this) {
        PropertyCharacteristic.READ_ONLY -> "val"
        PropertyCharacteristic.READ_WRITE -> "var"
        PropertyCharacteristic.REFERENCE -> "ref"
        PropertyCharacteristic.COMPOSITE -> "cmp"
        PropertyCharacteristic.STORED -> "str"
        PropertyCharacteristic.DERIVED -> "drv"
        PropertyCharacteristic.PRIMITIVE -> "prm"
        PropertyCharacteristic.CONSTRUCTOR -> "cns"
        PropertyCharacteristic.IDENTITY -> "idn"
    }
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class MethodName(override val value: String) : PublicValueType

interface MethodDeclaration {
    val owner: TypeDefinition
    val name: MethodName
    //TODO: Method TypeArgs
    val parameters: List<ParameterDeclaration>
    val returnType: TypeInstance
    val description: String

    fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): MethodDeclarationResolved
    fun findInOrCloneTo(other: TypesDomain): MethodDeclaration
}

interface MethodDeclarationPrimitive : MethodDeclaration
interface MethodDeclarationDerived : MethodDeclaration
interface MethodDeclarationResolved : MethodDeclaration {
    val original: MethodDeclaration
}

interface ConstructorDeclaration {
    val owner: TypeDefinition
    val parameters: List<ParameterDeclaration>

    fun findInOrCloneTo(other: TypesDomain): ConstructorDeclaration
}

// ParameterName clashes with kotlin.ParameterName
// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class TmParameterName(override val value: String) : PublicValueType

interface ParameterDeclaration {
    val name: TmParameterName
    val typeInstance: TypeInstance
    val defaultValue: String?

    fun findInOrCloneTo(other: TypesDomain): ParameterDeclaration
}