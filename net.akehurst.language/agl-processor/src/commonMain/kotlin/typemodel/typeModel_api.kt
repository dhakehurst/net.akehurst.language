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

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
/** FIXME: do not really want this annotation approach !**/
annotation class KompositeProperty

interface TypeModel : Model<TypeNamespace, TypeDeclaration> {

    val AnyType: TypeDeclaration
    val NothingType: TypeDeclaration

    val allNamespace: List<TypeNamespace>

    fun resolveImports()

    fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TypeNamespace

    fun findFirstByPossiblyQualifiedOrNull(typeName: PossiblyQualifiedName): TypeDeclaration?

    fun findFirstByNameOrNull(typeName: SimpleName): TypeDeclaration?

    fun findByQualifiedNameOrNull(qualifiedName: QualifiedName): TypeDeclaration?

    fun addAllNamespaceAndResolveImports(namespaces: Iterable<TypeNamespace>)

    // --- DefinitionBlock ---
    override fun findNamespaceOrNull(qualifiedName: QualifiedName): TypeNamespace?

}

interface TypeNamespace : Namespace<TypeDeclaration> {

    /**
     * TypeDefinition.name --> TypeDefinition
     */
    val ownedTypesByName: Map<SimpleName, TypeDeclaration>

    val ownedTypes: Collection<TypeDeclaration>

    val singletonType: Set<SingletonType>

    val primitiveType: Set<PrimitiveType>

    val valueType: Set<ValueType>

    val enumType: Set<EnumType>

    val collectionType: Set<CollectionType>

    val interfaceType: Set<InterfaceType>

    val dataType: Set<DataType>

    fun addImport(value: Import)

    //fun resolveImports(model: TypeModel)

    fun isImported(qualifiedNamespaceName: QualifiedName): Boolean

    /**
     * find type in this namespace with given name
     */
    fun findOwnedTypeNamed(typeName: SimpleName): TypeDeclaration?

    /**
     * find type in this namespace OR imports with given name
     */
    fun findTypeNamed(qualifiedOrImportedTypeName: PossiblyQualifiedName): TypeDeclaration?

    fun findTupleTypeWithIdOrNull(id:Int): TupleType?

    fun findOwnedOrCreateSingletonTypeNamed(typeName: SimpleName): SingletonType
    fun findOwnedOrCreatePrimitiveTypeNamed(typeName: SimpleName): PrimitiveType
    fun findOwnedOrCreateEnumTypeNamed(typeName: SimpleName, literals: List<String>): EnumType
    fun findOwnedOrCreateValueTypeNamed(typeName: SimpleName): ValueType
    fun findOwnedOrCreateInterfaceTypeNamed(typeName: SimpleName): InterfaceType
    fun findOwnedOrCreateDataTypeNamed(typeName: SimpleName): DataType
    fun findOwnedOrCreateCollectionTypeNamed(typeName: SimpleName): CollectionType

    fun createTypeInstance(
        context: TypeDeclaration?, qualifiedOrImportedTypeName: PossiblyQualifiedName, typeArguments: List<TypeArgument> = emptyList(), isNullable: Boolean = false
    ): TypeInstance

    fun createTupleTypeInstance(typeArguments: List<TypeArgumentNamed>, nullable: Boolean): TupleTypeInstance
    fun createUnnamedSupertypeTypeInstance(declaration: UnnamedSupertypeType, typeArguments: List<TypeArgument>, nullable: Boolean): TypeInstance

    fun createUnnamedSupertypeType(subtypes: List<TypeInstance>): UnnamedSupertypeType

    fun createTupleType(): TupleType

}

interface TypeParameter {
    /**
     * name for the type TypeParameter
     */
    val name: SimpleName
}

interface TypeArgument {
    val type:TypeInstance
    fun conformsTo(other: TypeArgument): Boolean
    fun signature(context: TypeNamespace?, currentDepth: Int): String
    fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance
}

interface TypeInstance {
    val namespace: TypeNamespace

    @KompositeProperty
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
    val declaration: TypeDeclaration

    /**
     * properties from the type, with type parameters resolved
     */
    val resolvedProperty: Map<PropertyName, PropertyDeclaration>

    val asTypeArgument: TypeArgument

    fun resolved(resolvingTypeArguments: Map<TypeParameter, TypeInstance>): TypeInstance

    fun notNullable(): TypeInstance
    fun nullable(): TypeInstance

    fun signature(context: TypeNamespace?, currentDepth: Int): String

    fun conformsTo(other: TypeInstance): Boolean
}

interface TypeArgumentNamed : TypeArgument {
    val name: PropertyName
}

interface TupleTypeInstance : TypeInstance {
    override val typeArguments: List<TypeArgumentNamed>
}

interface TypeDeclaration : Definition<TypeDeclaration> {
    override val namespace: TypeNamespace

    @KompositeProperty
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
     * information about this type
     */
    val metaInfo: Map<String, String>

    fun signature(context: TypeNamespace?, currentDepth: Int = 0): String

    fun type(typeArguments: List<TypeArgument> = emptyList(), nullable: Boolean = false): TypeInstance

    fun conformsTo(other: TypeDeclaration): Boolean

    fun getPropertyByIndexOrNull(i: Int): PropertyDeclaration?
    fun findPropertyOrNull(name: PropertyName): PropertyDeclaration?
    fun findMethodOrNull(name: MethodName): MethodDeclaration?

    fun asStringInContext(context: TypeNamespace): String

    fun addTypeParameter(name: TypeParameter)
    fun addSupertype(qualifiedTypeName: PossiblyQualifiedName)
    fun appendPropertyPrimitive(name: PropertyName, typeInstance: TypeInstance, description: String)
    fun appendPropertyDerived(name: PropertyName, typeInstance: TypeInstance, description: String, expression: String)
    fun appendMethodPrimitive(
        name: MethodName,
        parameters: List<ParameterDeclaration>,
        typeInstance: TypeInstance,
        description: String,
        body: (self: Any, arguments: List<Any>) -> Any
    )

    fun appendMethodDerived(name: MethodName, parameters: List<ParameterDeclaration>, typeInstance: TypeInstance, description: String, body: String)
}

interface SingletonType : TypeDeclaration {
}

interface PrimitiveType : TypeDeclaration {
}

interface EnumType : TypeDeclaration {
    val literals: List<String>
}

interface StructuredType : TypeDeclaration {

    fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration>

    /**
     * append property at the next index
     */
    fun appendPropertyStored(name: PropertyName, typeInstance: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int = -1): PropertyDeclaration

}

//interface TypeParameterVarArg : TypeParameter

interface TupleType : TypeDeclaration {

    //override val typeParameters: List<NamedTypeParameter>

    override fun type(typeArguments: List<TypeArgument>, nullable: Boolean): TupleTypeInstance

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
    @KompositeProperty
    val constructors: List<ConstructorDeclaration>
}

interface InterfaceType : StructuredType {
    val subtypes: MutableList<TypeInstance>

    fun addSubtype(qualifiedTypeName: PossiblyQualifiedName)
}

interface DataType : StructuredType {

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: MutableList<TypeInstance>

    @KompositeProperty
    val constructors: List<ConstructorDeclaration>

    fun addSubtype(qualifiedTypeName: PossiblyQualifiedName)
}

interface UnnamedSupertypeType : TypeDeclaration {
    companion object {
        val NAME = QualifiedName("\$UnnamedSupertypeType")
    }

    // identifier, needs a number else can't implement equals without a recursive loop
    val id: Int

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    @KompositeProperty
    val subtypes: List<TypeInstance>
}

interface CollectionType : StructuredType {
    val isStdList: Boolean
    val isStdSet: Boolean
    val isStdMap: Boolean
}

@JvmInline
value class PropertyName(val value: String) {
    override fun toString(): String = value
}

interface PropertyDeclaration {
    val owner: TypeDeclaration
    val name: PropertyName

    @KompositeProperty
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

    val isReadOnly:Boolean
    val isReadWrite: Boolean

    val isStored: Boolean
    val isDerived: Boolean
    val isPrimitive: Boolean


    fun resolved(typeArguments: Map<TypeParameter, TypeInstance>): PropertyDeclaration
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

@JvmInline
value class MethodName(val value: String)

interface MethodDeclaration {
    val owner: TypeDeclaration
    val name: MethodName
    @KompositeProperty
    val parameters: List<ParameterDeclaration>
    val description: String
}

interface ConstructorDeclaration {
    val owner: TypeDeclaration
    @KompositeProperty
    val parameters: List<ParameterDeclaration>
}

@JvmInline
value class ParameterName(val value: String)

interface ParameterDeclaration {
    val name: ParameterName
    @KompositeProperty
    val typeInstance: TypeInstance
    val defaultValue: String?
}