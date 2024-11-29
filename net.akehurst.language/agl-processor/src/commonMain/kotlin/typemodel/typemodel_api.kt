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

import net.akehurst.language.base.api.*
import kotlin.jvm.JvmInline

interface TypeModel : Model<TypeNamespace, TypeDefinition> {

    val AnyType: TypeDefinition
    val NothingType: TypeDefinition

    fun resolveImports()

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TypeNamespace

    fun findFirstByPossiblyQualifiedOrNull(typeName: PossiblyQualifiedName): TypeDefinition?

    fun findFirstByNameOrNull(typeName: SimpleName): TypeDefinition?

    fun findByQualifiedNameOrNull(qualifiedName: QualifiedName): TypeDefinition?

    fun addAllNamespaceAndResolveImports(namespaces: Iterable<TypeNamespace>)

    // --- DefinitionBlock ---
    override fun findNamespaceOrNull(qualifiedName: QualifiedName): TypeNamespace?

}

interface TypeNamespace : Namespace<TypeDefinition> {

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

    //fun resolveImports(model: TypeModel)

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

    fun findOwnedOrCreateSingletonTypeNamed(typeName: SimpleName): SingletonType
    fun findOwnedOrCreatePrimitiveTypeNamed(typeName: SimpleName): PrimitiveType
    fun findOwnedOrCreateEnumTypeNamed(typeName: SimpleName, literals: List<String>): EnumType
    fun findOwnedOrCreateValueTypeNamed(typeName: SimpleName): ValueType
    fun findOwnedOrCreateInterfaceTypeNamed(typeName: SimpleName): InterfaceType
    fun findOwnedOrCreateDataTypeNamed(typeName: SimpleName): DataType
    fun findOwnedOrCreateCollectionTypeNamed(typeName: SimpleName): CollectionType
    fun findOwnedOrCreateUnionTypeNamed(typeName: SimpleName, ifCreate:(UnionType)->Unit): UnionType

    fun createTypeInstance(
        context: TypeDefinition?, qualifiedOrImportedTypeName: PossiblyQualifiedName, typeArguments: List<TypeArgument> = emptyList(), isNullable: Boolean = false
    ): TypeInstance

    fun createTupleTypeInstance(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance
    //fun createUnnamedSupertypeTypeInstance(declaration: UnionType, typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance

    fun createTupleType(): TupleType

    /**
     * clone the namespace but not the content
     */
    fun cloneTo(other: TypeModel): TypeNamespace
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
    fun signature(context: TypeNamespace?, currentDepth: Int): String
    fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance

    fun cloneTo(other: TypeModel):TypeArgument
}

interface TypeInstance {
    val namespace: TypeNamespace

    val typeArguments: List<TypeArgument>

    val isNullable: Boolean

    /**
     * the name of the TypeDeclaration, if it is resolvable,
     * or the name the instance refers to (e.g. a type parameter)
     */
    val typeName: SimpleName

    val qualifiedTypeName: QualifiedName

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

    fun signature(context: TypeNamespace?, currentDepth: Int): String

    fun conformsTo(other: TypeInstance): Boolean
    fun possiblyQualifiedNameInContext(context: TypeNamespace): Any

    fun cloneTo(other: TypeModel): TypeInstance
}

interface TypeArgumentNamed : TypeArgument {
    val name: PropertyName

    override fun cloneTo(other: TypeModel):TypeArgumentNamed
}

interface TupleTypeInstance : TypeInstance {
    override val typeArguments: List<TypeArgumentNamed>
}

interface TypeDefinition : Definition<TypeDefinition> {
    override val namespace: TypeNamespace

    val supertypes: List<TypeInstance>

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

    fun signature(context: TypeNamespace?, currentDepth: Int = 0): String

    fun type(typeArguments: List<TypeArgument> = emptyList(), isNullable: Boolean = false): TypeInstance

    fun conformsTo(other: TypeDefinition): Boolean

    fun getOwnedPropertyByIndexOrNull(i: Int): PropertyDeclaration?
    fun findOwnedPropertyOrNull(name: PropertyName): PropertyDeclaration?
    fun findAllPropertyOrNull(name: PropertyName): PropertyDeclaration?
    fun findOwnedMethodOrNull(name: MethodName): MethodDeclaration?
    fun findAllMethodOrNull(name: MethodName): MethodDeclaration?

    fun asStringInContext(context: TypeNamespace): String

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
    ):MethodDeclarationPrimitive

    fun appendMethodDerived(name: MethodName, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String) : MethodDeclarationDerived

    fun cloneTo(other: TypeModel): TypeDefinition
}

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

    override fun cloneTo(other: TypeModel): StructuredType
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
    val subtypes: MutableList<TypeInstance>

    fun addSubtype(typeInstance: TypeInstance)
}

interface DataType : StructuredType {

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: MutableList<TypeInstance>

    val constructors: List<ConstructorDeclaration>

    @Deprecated("Create a TypeInstance and use addSubtype(TypeInstance)")
    fun addSubtype_dep(qualifiedTypeName: PossiblyQualifiedName)

    fun addSubtype(typeInstance: TypeInstance)
    fun addConstructor(parameters: List<ParameterDeclaration>)
}

interface UnionType : TypeDefinition {

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val alternatives: List<TypeInstance>

    fun addAlternative(value:TypeInstance)

    override fun cloneTo(other: TypeModel): UnionType
}

interface CollectionType : StructuredType {
    val isStdList: Boolean
    val isStdSet: Boolean
    val isStdMap: Boolean
}

@JvmInline
value class PropertyName(override val value: String):PublicValueType {
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

    fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): PropertyDeclarationResolved

    fun cloneTo(other: TypeModel): PropertyDeclaration
}

interface PropertyDeclarationResolved : PropertyDeclaration

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

@JvmInline
value class MethodName(override val value: String):PublicValueType

interface MethodDeclaration {
    val owner: TypeDefinition
    val name: MethodName
    val parameters: List<ParameterDeclaration>
    val returnType: TypeInstance
    val description: String

    fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): MethodDeclarationResolved
    fun cloneTo(other: TypeModel): MethodDeclaration
}

interface MethodDeclarationPrimitive : MethodDeclaration
interface MethodDeclarationDerived : MethodDeclaration
interface MethodDeclarationResolved : MethodDeclaration

interface ConstructorDeclaration {
    val owner: TypeDefinition
    val parameters: List<ParameterDeclaration>

    fun cloneTo(other: TypeModel): ConstructorDeclaration
}

@JvmInline
value class ParameterName(override val value: String):PublicValueType

interface ParameterDeclaration {
    val name: ParameterName
    val typeInstance: TypeInstance
    val defaultValue: String?

    fun cloneTo(other: TypeModel): ParameterDeclaration
}