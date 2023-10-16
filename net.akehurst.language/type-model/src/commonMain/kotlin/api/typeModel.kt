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

    val AnyType: TypeDefinition
    val NothingType: TypeDefinition

    val name: String
    //val rootTypeName: String?

    /**
     * namespace qualified named --> namespace
     */
    val namespace: Map<String, TypeNamespace>

    val allNamespace: List<TypeNamespace>

    fun resolveImports()

    fun findFirstByNameOrNull(typeName: String): TypeDefinition?

    fun findByQualifiedNameOrNull(qualifiedName: String): TypeDefinition?

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
    val allTypesByName: Map<String, TypeDefinition>

    val allTypes: Collection<TypeDefinition>

    val primitiveType: Set<PrimitiveType>

    val enumType: Set<EnumType>

    val collectionType: Set<CollectionType>

    val elementType: Set<DataType>

    fun resolveImports(model: TypeModel)

    fun isImported(qualifiedNamespaceName: String): Boolean

    /**
     * find type in this namespace with given name
     */
    fun findOwnedTypeNamed(typeName: String): TypeDefinition?

    /**
     * find type in this namespace OR imports with given name
     */
    fun findTypeNamed(qualifiedOrImportedTypeName: String): TypeDefinition?

    fun findOwnedOrCreatePrimitiveTypeNamed(typeName: String): PrimitiveType
    fun findOwnedOrCreateDataTypeNamed(typeName: String): DataType
    fun findOwnedOrCreateCollectionTypeNamed(typeName: String): CollectionType

    fun createTypeInstance(qualifiedOrImportedTypeName: String, typeArguments: List<TypeInstance>, isNullable: Boolean): TypeInstance
    fun createTupleTypeInstance(type: TupleType, arguments: List<TypeInstance>, nullable: Boolean): TypeInstance
    fun createUnnamedSuperTypeTypeInstance(type: UnnamedSuperTypeType, arguments: List<TypeInstance>, nullable: Boolean): TypeInstance

    fun createUnnamedSuperTypeType(subtypes: List<TypeInstance>): UnnamedSuperTypeType

    fun createTupleType(): TupleType

    fun asString(): String

}

interface TypeInstance {
    val namespace: TypeNamespace
    val type: TypeDefinition
    val typeArguments: List<TypeInstance>
    val isNullable: Boolean

    fun notNullable(): TypeInstance
    fun nullable(): TypeInstance

    fun signature(context: TypeNamespace?, currentDepth: Int): String
}

interface TypeDefinition {
    val namespace: TypeNamespace
    val name: String
    val qualifiedName: String
    val typeParameters: List<String>

    /**
     * information about this type
     */
    val metaInfo: Map<String, String>

    fun signature(context: TypeNamespace?, currentDepth: Int = 0): String

    fun instance(arguments: List<TypeInstance> = emptyList(), nullable: Boolean = false): TypeInstance

    fun asString(context: TypeNamespace): String
}

interface PrimitiveType : TypeDefinition {
}

interface EnumType : TypeDefinition {
    val literals: List<String>
}

interface StructuredType : TypeDefinition {

    val property: Map<String, PropertyDeclaration>
    val properties: Map<Int, PropertyDeclaration>

    fun propertiesWithCharacteristic(chr: PropertyCharacteristic): List<PropertyDeclaration>

    fun getPropertyByIndex(i: Int): PropertyDeclaration?

    /**
     * append property at the next index
     */
    fun appendProperty(name: String, type: TypeInstance, characteristics: Set<PropertyCharacteristic>, index: Int = -1): PropertyDeclaration
}

interface TupleType : StructuredType {
    val entries: List<Pair<String, TypeInstance>>
}

interface DataType : StructuredType {

    val supertypes: List<TypeInstance>

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: MutableList<TypeInstance>

    /**
     * transitive closure of supertypes
     */
    val allSuperTypes: List<TypeInstance>

    /**
     * all properties from this and transitive closure of supertypes
     */
    val allProperty: Map<String, PropertyDeclaration>

    fun addSupertype(qualifiedTypeName: String)
    fun addSubtype(qualifiedTypeName: String)
}

/**
 * constructor appends this property to the owner
 */
interface PropertyDeclaration {
    val owner: StructuredType
    val name: String
    val typeInstance: TypeInstance
    val characteristics: Set<PropertyCharacteristic>

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
    MEMBER
}

interface UnnamedSuperTypeType : TypeDefinition {

    // identifier, needs a number else can't implement equals without a recursive loop
    val id: Int

    // List rather than Set or OrderedSet because same type can appear more than once, and the 'option' index in the SPPT indicates which
    val subtypes: List<TypeInstance>
}

interface CollectionType : StructuredType {
    val supertypes: Set<CollectionType>

    val isArray: Boolean
    val isList: Boolean
    val isSet: Boolean
    val isMap: Boolean
}

