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

import net.akehurst.language.typemodel.simple.*

@DslMarker
annotation class TypeModelDslMarker

fun typeModel(
    name: String,
    resolveImports: Boolean,
    init: TypeModelBuilder.() -> Unit
): TypeModel {
    val b = TypeModelBuilder(name)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeModelBuilder(
    val name: String
) {
    val _model = TypeModelSimple(name)

    fun namespace(qualifiedName: String, imports: MutableList<String> = mutableListOf(SimpleTypeModelStdLib.qualifiedName), init: TypeNamespaceBuilder.() -> Unit): TypeNamespace {
        val b = TypeNamespaceBuilder(qualifiedName, imports)
        b.init()
        val ns = b.build()
        _model.addNamespace(ns)
        return ns
    }

    fun build(): TypeModel {
        _model.addNamespace(SimpleTypeModelStdLib)
        _model.resolveImports()
        return _model
    }
}

@TypeModelDslMarker
class TypeNamespaceBuilder(
    val qualifiedName: String,
    imports: MutableList<String>
) {

    private val _namespace = TypeNamespaceSimple(qualifiedName, imports)
    private val _typeReferences = mutableListOf<TypeUsageReferenceBuilder>()

    fun primitiveType(typeName: String): PrimitiveType = _namespace.findOrCreatePrimitiveTypeNamed(typeName)

    fun enumType(typeName: String, literals: List<String>): EnumType {
        val et = EnumTypeSimple(_namespace, typeName, literals)
        _namespace.addDeclaration(et)
        return et
    }

    fun collectionType(typeName: String, typeParams: List<String>): CollectionType =
        _namespace.findOrCreateCollectionTypeNamed(typeName).also { (it.typeParameters as MutableList).addAll(typeParams) }

    /**
     * create a list type of the indicated typeName
     * if typeName is not already defined, it will be defined as an DataType
     */
    /*
        fun listTypeOf(elementTypeName: String): TypeInstance {
            return collectionTypeOf(SimpleTypeModelStdLib.List.name, elementTypeName)
        }

        fun listSeparatedType(itemType: TypeDefinition, separatorType: TypeDefinition): TypeInstance {
            val collType = SimpleTypeModelStdLib.ListSeparated
            return collectionType(collType, listOf(itemType.instance(), separatorType.instance()))
        }

        fun listSeparatedTypeOf(itemTypeName: String, separatorTypeName: String): TypeInstance {
            val collType = SimpleTypeModelStdLib.ListSeparated
            val itemType = _namespace.findTypeNamed(itemTypeName) ?: _namespace.findOrCreateElementTypeNamed(itemTypeName)
            val separatorType = _namespace.findTypeNamed(separatorTypeName) ?: _namespace.findOrCreateElementTypeNamed(separatorTypeName)
            return collectionType(collType, listOf(itemType.instance(), separatorType.instance()))
        }
    */

    fun dataType(typeName: String, init: DataTypeBuilder.() -> Unit = {}): DataType {
        val b = DataTypeBuilder(_namespace, _typeReferences, typeName)
        b.init()
        val et = b.build()
        return et
    }

    fun unnamedSuperTypeTypeFor(subtypes: List<Any>): UnnamedSuperTypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _namespace.findOrCreateDataTypeNamed(it)!!
                is TypeDefinition -> it
                else -> error("Cannot map to TypeDefinition: $it")
            }
        }
        val t = _namespace.createUnnamedSuperTypeType(sts.map { it.instance() })
        return t
    }

    fun build(): TypeNamespace {
        return _namespace
    }
}

@TypeModelDslMarker
abstract class StructuredTypeBuilder(
    protected val _namespace: TypeNamespace,
    private val _typeReferences: MutableList<TypeUsageReferenceBuilder>
) {
    protected abstract val _structuredType: StructuredType

    val COMPOSITE = PropertyCharacteristic.COMPOSITE
    val REFERENCE = PropertyCharacteristic.REFERENCE
    val IDENTITY = PropertyCharacteristic.IDENTITY
    val MEMBER = PropertyCharacteristic.MEMBER
    val CONSTRUCTOR = PropertyCharacteristic.CONSTRUCTOR

    fun propertyOf(
        characteristics: Set<PropertyCharacteristic>,
        propertyName: String,
        typeName: String,
        typeArgs: List<String> = emptyList(),
        isNullable: Boolean = false,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_namespace)
        tab.init()
        val btargs = tab.build()
        val atargs = typeArgs.map { TypeInstanceSimple(_namespace, it, emptyList(), false) }
        val targs = if (btargs.isEmpty()) atargs else btargs
        val ti = TypeInstanceSimple(_namespace, typeName, targs, isNullable)
        return _structuredType.appendProperty(propertyName, ti, characteristics)
    }

    fun propertyPrimitiveType(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, TypeInstanceSimple(this._namespace, typeName, emptyList(), isNullable), childIndex)

    fun propertyListTypeOf(propertyName: String, dataTypeName: String, nullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(propertyName, nullable, childIndex) {
            elementRef(dataTypeName)
        }

    fun propertyListType(propertyName: String, nullable: Boolean, childIndex: Int, init: TypeUsageReferenceBuilder.() -> Unit): PropertyDeclaration {
        val collType = SimpleTypeModelStdLib.List
        val tb = TypeUsageReferenceBuilder(this._namespace, collType, nullable)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        return property(propertyName, tu, childIndex)
    }

    // ListSeparated
    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _namespace.findTypeNamed(itemTypeName) ?: _namespace.findOrCreateDataTypeNamed(itemTypeName)!!
        val separatorType = _namespace.findTypeNamed(itemTypeName) ?: _namespace.findOrCreateDataTypeNamed(separatorTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorType: TypeDefinition, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _namespace.findOrCreateDataTypeNamed(itemTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, itemType: TypeDefinition, separatorType: TypeDefinition, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val collType = SimpleTypeModelStdLib.ListSeparated
        val propType = collType.instance(listOf(itemType.instance(), separatorType.instance()), isNullable)
        return property(propertyName, propType, childIndex)
    }

    // Tuple
    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}): PropertyDeclaration {
        val b = TupleTypeBuilder(_namespace, _typeReferences)
        b.init()
        val tt = b.build()
        val collType = SimpleTypeModelStdLib.List
        val t = collType.instance(listOf(tt.instance()))
        return property(propertyName, t, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit): PropertyDeclaration {
        val b = TupleTypeBuilder(_namespace, _typeReferences)
        b.init()
        val tt = b.build()
        return property(propertyName, tt.instance(), childIndex)
    }

    fun propertyUnnamedSuperType(propertyName: String, isNullable: Boolean, childIndex: Int, init: SubtypeListBuilder.() -> Unit): PropertyDeclaration {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSuperTypeType(stu)
        return property(propertyName, t.instance(emptyList(), isNullable), childIndex)
    }

    //
    fun propertyDataTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val t = _namespace.findOrCreateDataTypeNamed(elementTypeName)!!
        return property(propertyName, t.instance(emptyList(), isNullable), childIndex)
    }

    fun property(propertyName: String, typeUse: TypeInstance, childIndex: Int): PropertyDeclaration {
        check(childIndex >= _structuredType.property.size) { "Incorrect property index" }
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        return _structuredType.appendProperty(propertyName, typeUse, characteristics, childIndex)
    }
}

@TypeModelDslMarker
class TupleTypeBuilder(
    _namespace: TypeNamespace,
    _typeReferences: MutableList<TypeUsageReferenceBuilder>
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    override val _structuredType = _namespace.createTupleType()

    fun build(): TupleType {
        return _structuredType
    }
}

@TypeModelDslMarker
class DataTypeBuilder(
    _namespace: TypeNamespace,
    _typeReferences: MutableList<TypeUsageReferenceBuilder>,
    _elementName: String
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _elementType = _namespace.findOrCreateDataTypeNamed(_elementName) as DataType
    override val _structuredType: StructuredType get() = _elementType

    fun typeParameters(vararg parameters: String) {
        ((_elementType as DataType).typeParameters as MutableList).addAll(parameters)
    }

    fun superTypes(vararg superTypes: String) {
        superTypes.forEach {
            _elementType.addSupertype(it)
        }
    }

    fun subTypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            _elementType.addSubtype(it)
        }
    }

    fun build(): DataType {
        return _elementType
    }

}

@TypeModelDslMarker
class TypeUsageReferenceBuilder(
    val _namespace: TypeNamespace,
    val type: TypeDefinition,
    val nullable: Boolean
) {
    private val _args = mutableListOf<TypeInstance>()

    fun primitiveRef(typeName: String) {
        _args.add(TypeInstanceSimple(_namespace, typeName, emptyList(), nullable))
    }

    fun elementRef(typeName: String) {
        _args.add(TypeInstanceSimple(_namespace, typeName, emptyList(), nullable))
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        val subtypes = subtypeNames.map { TypeInstanceSimple(_namespace, it, emptyList(), false) }
        val t = _namespace.createUnnamedSuperTypeType(subtypes)
        _args.add(t.instance(emptyList(), nullable))
    }

    fun build(): TypeInstance {
        return type.instance(_args, false)
    }
}

@TypeModelDslMarker
class TypeArgumentBuilder(
    private val _namespace: TypeNamespace
) {
    private val list = mutableListOf<TypeInstance>()
    fun typeArgument(qualifiedTypeName: String, typeArguments: TypeArgumentBuilder.() -> Unit = {}): TypeInstance {
        val tab = TypeArgumentBuilder(_namespace)
        tab.typeArguments()
        val typeArgs = tab.build()
        val tref = TypeInstanceSimple(_namespace, qualifiedTypeName, typeArgs, false)
        list.add(tref)
        return tref
    }

    fun build(): List<TypeInstance> {
        return list
    }
}

@TypeModelDslMarker
class SubtypeListBuilder(
    val _namespace: TypeNamespace,
    private val _typeReferences: MutableList<TypeUsageReferenceBuilder>
) {

    val _subtypeList = mutableListOf<TypeInstance>()

    fun primitiveRef(typeName: String) {
        val ti = TypeInstanceSimple(_namespace, typeName, emptyList(), false)
        _subtypeList.add(ti)
    }

    fun elementRef(elementTypeName: String) {
        val ti = TypeInstanceSimple(_namespace, elementTypeName, emptyList(), false)
        _subtypeList.add(ti)
    }

    fun listType(nullable: Boolean, init: TypeUsageReferenceBuilder.() -> Unit) {
        val collType = SimpleTypeModelStdLib.List
        val tb = TypeUsageReferenceBuilder(this._namespace, collType, nullable)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        _subtypeList.add(tu)
    }

    fun tupleType(init: TupleTypeBuilder.() -> Unit) {
        val b = TupleTypeBuilder(_namespace, mutableListOf())
        b.init()
        val t = b.build()
        _subtypeList.add(t.instance())
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        val sts = subtypeNames.map { _namespace.findOrCreateDataTypeNamed(it)!! }
        val t = _namespace.createUnnamedSuperTypeType(sts.map { it.instance() })
        _subtypeList.add(t.instance())
    }

    fun unnamedSuperType(init: SubtypeListBuilder.() -> Unit) {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSuperTypeType(stu)
        _subtypeList.add(t.instance())
    }

    fun build(): List<TypeInstance> = _subtypeList
}