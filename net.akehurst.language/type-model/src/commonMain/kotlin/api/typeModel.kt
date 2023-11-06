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

interface TypeModel {

    val AnyType: TypeDeclaration
    val NothingType: TypeDeclaration

    val name: String
    //val rootTypeName: String?

    /**
     * namespace qualified named --> namespace
     */
    val namespace: Map<String, TypeNamespace>

    val allNamespace: List<TypeNamespace>

    fun resolveImports()

    fun findOrCreateNamespace(qualifiedName: String, imports: List<String>): TypeNamespace

    fun findFirstByNameOrNull(typeName: String): TypeDeclaration?

    fun findByQualifiedNameOrNull(qualifiedName: String): TypeDeclaration?

    fun asString(): String
}

interface TypeNamespace {

    val qualifiedName: String

    /**
     * Things in these namespaces can be referenced non-qualified
     * ordered so that 'first' imported name takes priority
     */
    val imports: List<String>

    /**
     * TypeDefinition.name --> TypeDefinition
     */
    val allTypesByName: Map<String, TypeDeclaration>

    val allTypes: Collection<TypeDeclaration>

    val primitiveType: Set<PrimitiveType>

    val enumType: Set<EnumType>

    val collectionType: Set<CollectionType>

    val elementType: Set<DataType>

    fun addImport(qualifiedName: String)

    fun resolveImports(model: TypeModel)

    fun isImported(qualifiedNamespaceName: String): Boolean

    /**
     * find type in this namespace with given name
     */
    fun findOwnedTypeNamed(typeName: String): TypeDeclaration?

    /**
     * find type in this namespace OR imports with given name
     */
    fun findTypeNamed(qualifiedOrImportedTypeName: String): TypeDeclaration?

    fun findOwnedOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType
    fun findOwnedOrCreateDataTypeNamed(typeName: String): DataType
    fun findOwnedOrCreateCollectionTypeNamed(typeName: String): CollectionType

    fun createTypeInstance(
        context: TypeDeclaration?, qualifiedOrImportedTypeName: String, typeArguments: List<TypeInstance> = emptyList(), isNullable: Boolean = false
    ): TypeInstance

    fun createTupleTypeInstance(type: TupleType, typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance
    fun createUnnamedSupertypeTypeInstance(type: UnnamedSupertypeType, typeArguments: List<TypeInstance>, nullable: Boolean): TypeInstance

    fun createUnnamedSupertypeType(subtypes: List<TypeInstance>): UnnamedSupertypeType

    fun createTupleType(): TupleType

    fun asString(): String

}

interface TypeInstance {
    val namespace: TypeNamespace
    val typeArguments: List<TypeInstance>
    val isNullable: Boolean

    /**
     * the name of the type, if it is resolvable,
     * or the name the instance refers to (e.g. a type parameter)
     */
    val typeName: String

    val qualifiedTypeName: String

    /**
     * {derived} type is resolved via the namespace
     */
    val type: TypeDeclaration

    /**
     * properties from the type, with type parameters resolved
     */
    val resolvedProperty: Map<String, PropertyDeclaration>

    fun resolved(resolvingTypeArguments: Map<String, TypeInstance>): TypeInstance

    fun notNullable(): TypeInstance
    fun nullable(): TypeInstance

    fun signature(context: TypeNamespace?, currentDepth: Int): String

    fun conformsTo(other: TypeInstance): Boolean
}

interface TypeDeclaration {
    val namespace: TypeNamespace
    val name: String
    val qualifiedName: String

    val supertypes: List<TypeInstance>
    val typeParameters: List<String>

    val property: List<PropertyDeclaration>
    val method: List<MethodDeclaration>

    /**
     * transitive closure of supertypes
     */
    val allSuperTypes: List<TypeInstance>

    /**
     * all properties from this and transitive closure of supertypes
     */
    val allProperty: Map<String, PropertyDeclaration>

    /**
     * information about this type
     */
    val metaInfo: Map<String, String>

    fun signature(context: TypeNamespace?, currentDepth: Int = 0): String

    fun instance(arguments: List<TypeInstance> = emptyList(), nullable: Boolean = false): TypeInstance

    fun conformsTo(other: TypeDeclaration): Boolean

    fun getPropertyByIndexOrNull(i: Int): PropertyDeclaration?
    fun findPropertyOrNull(name: String): PropertyDeclaration?
    fun findMethodOrNull(name: String): MethodDeclaration?

    fun asString(context: TypeNamespace): String

    fun addSupertype(qualifiedTypeName: String)
    fun appendPropertyPrimitive(name: String, typeInstance: TypeInstance, description: String, expression: (self: Any) -> Any)
    fun appendPropertyDerived(name: String, typeInstance: TypeInstance, description: String, expression: String)
    fun appendMethodPrimitive(name: String, parameters: List<ParameterDefinition>, typeInstance: TypeInstance, description: String, body: (self: Any, arguments: List<Any>) -> Any)
    fun appendMethodDerived(name: String, parameters: List<ParameterDefinition>, typeInstance: TypeInstance, description: String, body: String)
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
    fun appendPropertyStored(name: String, typeInstance: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int = -1): PropertyDeclaration

}

interface TupleType : StructuredType {
    val entries: List<Pair<String, TypeInstance>>

    /**
     * The compares two Tuple types by checking for the same name:Type of all entries.
     * The 'equals' method compares the namespace and id of the TupleType.
     */
    fun equalTo(other: TupleType): Boolean
}

interface DataType : StructuredType {

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: MutableList<TypeInstance>

    fun addSubtype(qualifiedTypeName: String)
}

interface UnnamedSupertypeType : TypeDeclaration {

    // identifier, needs a number else can't implement equals without a recursive loop
    val id: Int

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: List<TypeInstance>
}

interface CollectionType : StructuredType {

//    val isArray: Boolean
//    val isList: Boolean
//    val isSet: Boolean
//    val isMap: Boolean
}

interface PropertyDeclaration {
    val owner: TypeDeclaration
    val name: String
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
    val isIdentity: Boolean
    val isConstructor: Boolean
    val isMember: Boolean
    val isDerived: Boolean

    fun resolved(typeArguments: Map<String, TypeInstance>): PropertyDeclaration
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
    IDENTITY,

    /**
     * property is a constructor parameter
     */
    CONSTRUCTOR,

    /**
     * property is a member (not a constructor property)
     */
    MEMBER,

    /**
     * property is derived, calculated by given expression, not stored
     */
    DERIVED,

    /**
     * property is primitive, with built-in calculation, not stored
     */
    PRIMITIVE
}

interface MethodDeclaration {
    val owner: TypeDeclaration
    val name: String
    val parameters: List<ParameterDefinition>
    val description: String
}

interface ParameterDefinition {
    val name: String
    val typeInstance: TypeInstance
    val defaultValue: String?
}