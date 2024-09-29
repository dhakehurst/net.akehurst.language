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

import net.akehurst.language.base.api.*
import net.akehurst.language.base.api.QualifiedName.Companion.isQualifiedName
import net.akehurst.language.typemodel.api.*

@DslMarker
annotation class TypeModelDslMarker

fun typeModel(
    name: String,
    resolveImports: Boolean,
    namespaces: List<TypeNamespace> = listOf(SimpleTypeModelStdLib),
    init: TypeModelBuilder.() -> Unit
): TypeModel {
    val b = TypeModelBuilder(SimpleName(name), resolveImports, namespaces)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeModelBuilder(
    val name: SimpleName,
    private val resolveImports: Boolean,
    val namespaces: List<TypeNamespace>
) {
    val _model = TypeModelSimple(name)

    fun namespace(qualifiedName: String, imports: List<String> = listOf(SimpleTypeModelStdLib.qualifiedName.value), init: TypeNamespaceBuilder.() -> Unit): TypeNamespace {
        val b = TypeNamespaceBuilder(QualifiedName(qualifiedName), imports.map { Import(it) })
        b.init()
        val ns = b.build()
        _model.addNamespace(ns)
        return ns
    }

    fun build(): TypeModel {
        namespaces.forEach {
            _model.addNamespace(it)
        }
        if (resolveImports) {
            _model.resolveImports()
        }
        return _model
    }
}

@TypeModelDslMarker
class TypeNamespaceBuilder(
    val qualifiedName: QualifiedName,
    imports: List<Import>
) {

    private val _namespace = TypeNamespaceSimple(qualifiedName, imports)
    private val _typeReferences = mutableListOf<TypeInstanceArgBuilder>()

    fun primitiveType(typeName: String): PrimitiveType =
        _namespace.findOwnedOrCreatePrimitiveTypeNamed(SimpleName(typeName))

    fun valueType(typeName: String, init: DataTypeBuilder.() -> Unit = {})  {
        //TODO: do we need and actual 'ValueType' interface,class,etc
        dataType(typeName, init)
    }

    fun interfaceType(typeName: String, init: DataTypeBuilder.() -> Unit = {}) {
        //TODO: do we need and actual 'InterfaceType' interface,class,etc
        dataType(typeName,init)
    }

    fun enumType(typeName: String, literals: List<String>): EnumType =
        _namespace.findOwnedOrCreateEnumTypeNamed(SimpleName(typeName), literals)

    fun collectionType(typeName: String, typeParams: List<String>): CollectionType =
        _namespace.findOwnedOrCreateCollectionTypeNamed(SimpleName(typeName)).also {
            (it.typeParameters as MutableList).addAll(typeParams.map { tp -> TypeParameterSimple(SimpleName(tp)) })
        }

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
        val b = DataTypeBuilder(_namespace, _typeReferences, SimpleName(typeName))
        b.init()
        val et = b.build()
        return et
    }

    fun unnamedSuperTypeTypeFor(subtypes: List<Any>): UnnamedSupertypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(it))!!
                is TypeDeclaration -> it
                else -> error("Cannot map to TypeDefinition: $it")
            }
        }
        val t = _namespace.createUnnamedSupertypeType(sts.map { it.type() })
        return t
    }

    fun singleton(typeName: String) = _namespace.findOwnedOrCreateSingletonTypeNamed(SimpleName(typeName))

    fun build(): TypeNamespace {
        return _namespace
    }
}

@TypeModelDslMarker
abstract class StructuredTypeBuilder(
    protected val _namespace: TypeNamespace,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    protected abstract val _structuredType: StructuredType

    val CONSTRUCTOR = PropertyCharacteristic.CONSTRUCTOR
    val IDENTITY = PropertyCharacteristic.IDENTITY

    val COMPOSITE = PropertyCharacteristic.COMPOSITE
    val REFERENCE = PropertyCharacteristic.REFERENCE

    val READ_ONLY = PropertyCharacteristic.READ_ONLY
    val READ_WRITE = PropertyCharacteristic.READ_WRITE

    val STORED = PropertyCharacteristic.STORED
    val DERIVED = PropertyCharacteristic.DERIVED

    fun propertyOf(
        characteristics: Set<PropertyCharacteristic>,
        propertyName: String,
        typeName: String,
        //typeArgs: List<String> = emptyList(),
        isNullable: Boolean = false,
        init: TypeArgumentBuilder.() -> Unit = {}
    ): PropertyDeclaration {
        val tab = TypeArgumentBuilder(_structuredType, _namespace)
        tab.init()
        val btargs = tab.build()
        //val atargs = typeArgs.map { _namespace.createTypeInstance(_structuredType, it.asPossiblyQualifiedName, emptyList(), false) }
        val targs = btargs //if (btargs.isEmpty()) atargs else btargs
        val ti = _namespace.createTypeInstance(_structuredType, typeName.asPossiblyQualifiedName, targs, isNullable)
        return _structuredType.appendPropertyStored(PropertyName(propertyName), ti, characteristics)
    }

    fun propertyPrimitiveType(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, this._namespace.createTypeInstance(_structuredType, typeName.asPossiblyQualifiedName, emptyList(), isNullable), childIndex)

    fun propertyListTypeOf(propertyName: String, dataTypeName: String, nullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(propertyName, nullable, childIndex) {
            elementRef(dataTypeName)
        }

    fun propertyListType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgBuilder.() -> Unit): PropertyDeclaration {
        val collType = SimpleTypeModelStdLib.List
        val tb = TypeInstanceArgBuilder(this._structuredType, this._namespace, collType, isNullable, _typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        return property(propertyName, tu, childIndex)
    }

    // ListSeparated
    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _namespace.findTypeNamed(itemTypeName.asPossiblyQualifiedName) ?: _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(itemTypeName))
        val separatorType = _namespace.findTypeNamed(itemTypeName.asPossiblyQualifiedName) ?: _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(separatorTypeName))
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorType: TypeDeclaration, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(itemTypeName))
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, itemType: TypeDeclaration, separatorType: TypeDeclaration, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val collType = SimpleTypeModelStdLib.ListSeparated
        val propType = collType.type(listOf(itemType.type().asTypeArgument, separatorType.type().asTypeArgument), isNullable)
        return property(propertyName, propType, childIndex)
    }

    // Tuple
    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgNamedBuilder.() -> Unit = {}): PropertyDeclaration {
        val tt = SimpleTypeModelStdLib.TupleType
        val b = TypeInstanceArgNamedBuilder(this._structuredType, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        val collType = SimpleTypeModelStdLib.List
        val t = collType.type(listOf(tti.asTypeArgument))
        return property(propertyName, t, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TypeInstanceArgNamedBuilder.() -> Unit): PropertyDeclaration {
        val tt = SimpleTypeModelStdLib.TupleType
        val b = TypeInstanceArgNamedBuilder(this._structuredType, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        return property(propertyName, tti, childIndex)
    }

    fun propertyUnnamedSuperType(propertyName: String, isNullable: Boolean, childIndex: Int, init: SubtypeListBuilder.() -> Unit): PropertyDeclaration {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSupertypeType(stu)
        return property(propertyName, t.type(emptyList(), isNullable), childIndex)
    }

    fun propertyDataTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val t = if (elementTypeName.isQualifiedName) {
            _namespace.findTypeNamed(QualifiedName(elementTypeName)) ?: error("Type named '$elementTypeName' not found")
        } else {
            _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(elementTypeName))
        }
        return property(propertyName, t.type(emptyList(), isNullable), childIndex)
    }

    fun property(propertyName: String, typeUse: TypeInstance, childIndex: Int): PropertyDeclaration {
        check(childIndex >= _structuredType.property.size) { "Incorrect property index" }
        val characteristics = setOf(PropertyCharacteristic.COMPOSITE)
        return _structuredType.appendPropertyStored(PropertyName(propertyName), typeUse, characteristics, childIndex)
    }
}

/*
@TypeModelDslMarker
class TupleTypeBuilder(
    protected val _namespace: TypeNamespace,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {

    fun propertyPrimitiveType(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, this._namespace.createTypeInstance(_structuredType, typeName.asPossiblyQualifiedName, emptyList(), isNullable), childIndex)


    fun build(): TupleType {
        return _structuredType
    }
}
*/

@TypeModelDslMarker
class ValueTypeBuilder(
    _namespace: TypeNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateValueTypeNamed(_name) as ValueType
    override val _structuredType: StructuredType get() = _type

    fun supertypes(vararg superTypes: String) {
        superTypes.forEach {
            _type.addSupertype(it.asPossiblyQualifiedName)
        }
    }

    fun constructor_(init: ConstructorBuilder.() -> Unit) {
        val b = ConstructorBuilder(_namespace, _type, _typeReferences)
        b.init()
        val params = b.build()
        (_type as ValueTypeSimple).addConstructor(params)
    }

    fun build(): ValueType {
        return _type
    }

}

@TypeModelDslMarker
class InterfaceTypeBuilder(
    _namespace: TypeNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateInterfaceTypeNamed(_name) as InterfaceType
    override val _structuredType: StructuredType get() = _type

    fun typeParameters(vararg parameters: String) {
        (_type.typeParameters as MutableList).addAll(parameters.map { TypeParameterSimple(SimpleName(it)) })
    }

    fun supertypes(vararg superTypes: String) {
        superTypes.forEach {
            _type.addSupertype(it.asPossiblyQualifiedName)
        }
    }

    fun subtypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val pqn = it.asPossiblyQualifiedName
            _type.addSubtype(pqn)
            (_namespace.findTypeNamed(pqn) as DataType?)?.addSupertype(_type.qualifiedName)
        }
    }

    fun build(): InterfaceType {
        return _type
    }

}

@TypeModelDslMarker
class DataTypeBuilder(
    _namespace: TypeNamespace,
    _typeReferences: MutableList<TypeInstanceArgBuilder>,
    _name: SimpleName
) : StructuredTypeBuilder(_namespace, _typeReferences) {

    private val _type = _namespace.findOwnedOrCreateDataTypeNamed(_name) as DataType
    override val _structuredType: StructuredType get() = _type

    fun typeParameters(vararg parameters: String) {
        (_type.typeParameters as MutableList).addAll(parameters.map { TypeParameterSimple(SimpleName(it)) })
    }

    fun supertypes(vararg superTypes: String) {
        superTypes.forEach {
            _type.addSupertype(it.asPossiblyQualifiedName)
        }
    }

    fun subtypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val pqn = it.asPossiblyQualifiedName
            _type.addSubtype(pqn)
            (_namespace.findTypeNamed(pqn) as DataType?)?.addSupertype(_type.qualifiedName)
        }
    }

    fun constructor_(init: ConstructorBuilder.() -> Unit) {
        val b = ConstructorBuilder(_namespace, _type, _typeReferences)
        b.init()
        val params = b.build()
        (_type as DataTypeSimple).addConstructor(params)
    }

    fun build(): DataType {
        return _type
    }

}

@TypeModelDslMarker
class ConstructorBuilder(
    val _namespace: TypeNamespace,
    private val _type:TypeDeclaration,
    private val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {

    private val _paramList = mutableListOf<ParameterDeclaration>()

    fun parameter(name:String,  typeName: String,nullable:Boolean = false) {
        val ty = _namespace.createTypeInstance(_type, typeName.asPossiblyQualifiedName, emptyList(), nullable)
        _paramList.add(ParameterDefinitionSimple(net.akehurst.language.typemodel.api.ParameterName(name), ty, null))
    }

    fun build(): List<ParameterDeclaration> = _paramList
}

@TypeModelDslMarker
class TypeInstanceArgBuilder(
    val context: TypeDeclaration?,
    val _namespace: TypeNamespace,
    val type: TypeDeclaration,
    val nullable: Boolean,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    private val _args = mutableListOf<TypeArgument>()

    fun primitiveRef(typeName: String) {
        _args.add(_namespace.createTypeInstance(context, typeName.asPossiblyQualifiedName, emptyList(), nullable).asTypeArgument)
    }

    fun elementRef(typeName: String) {
        _args.add(_namespace.createTypeInstance(context, typeName.asPossiblyQualifiedName, emptyList(), nullable).asTypeArgument)
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        val subtypes = subtypeNames.map { _namespace.createTypeInstance(context, it.asPossiblyQualifiedName, emptyList(), false) }
        val t = _namespace.createUnnamedSupertypeType(subtypes)
        _args.add(t.type(emptyList(), nullable).asTypeArgument)
    }

    fun unnamedSuperTypeOf(init: SubtypeListBuilder.() -> Unit) {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSupertypeType(stu)
        _args.add(t.type(emptyList(), nullable).asTypeArgument)
    }

    fun build(): TypeInstance {
        return type.type(_args, false)
    }
}

@TypeModelDslMarker
class TypeInstanceArgNamedBuilder(
    val context: TypeDeclaration?,
    val _namespace: TypeNamespace,
    val type: TypeDeclaration,
    val instanceIsNullable: Boolean,
    protected val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {
    private val _args = mutableListOf<TypeArgumentNamed>()

    fun primitive(name:String, typeName: String, isNullable: Boolean) {
        val t = _namespace.createTypeInstance(context, typeName.asPossiblyQualifiedName, emptyList(), isNullable)
        val ta = TypeArgumentNamedSimple(PropertyName(name), t)
        _args.add(ta)
    }

    fun dataType(name:String, typeName: String, isNullable: Boolean) {
        val t = _namespace.createTypeInstance(context, typeName.asPossiblyQualifiedName, emptyList(), isNullable)
        val ta = TypeArgumentNamedSimple(PropertyName(name), t)
        _args.add(ta)
    }

    fun unnamedSuperTypeOf(name:String, isNullable: Boolean, init: SubtypeListBuilder.() -> Unit) {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSupertypeType(stu).type(emptyList(),isNullable)
        val ta = TypeArgumentNamedSimple(PropertyName(name), t)
        _args.add(ta)
    }

    fun tuple(name:String, isNullable: Boolean, init: TypeInstanceArgNamedBuilder.() -> Unit) {
        val tt = SimpleTypeModelStdLib.TupleType
        val b = TypeInstanceArgNamedBuilder(context, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        val ta = TypeArgumentNamedSimple(PropertyName(name), tti)
        _args.add(ta)
    }

    fun build(): TypeInstance {
        return type.type(_args, instanceIsNullable)
    }
}

@TypeModelDslMarker
class TypeArgumentBuilder(
    private val _context: TypeDeclaration?,
    private val _namespace: TypeNamespace
) {
    private val list = mutableListOf<TypeArgument>()
    fun typeArgument(qualifiedTypeName: String, typeArguments: TypeArgumentBuilder.() -> Unit = {}) {
        val tab = TypeArgumentBuilder(_context, _namespace)
        tab.typeArguments()
        val typeArgs = tab.build()
        val tref = _namespace.createTypeInstance(_context, qualifiedTypeName.asPossiblyQualifiedName, typeArgs, false)
        list.add(tref.asTypeArgument)
    }

    fun build(): List<TypeArgument> {
        return list
    }
}

@TypeModelDslMarker
class SubtypeListBuilder(
    val _namespace: TypeNamespace,
    private val _typeReferences: MutableList<TypeInstanceArgBuilder>
) {

    val _subtypeList = mutableListOf<TypeInstance>()

    fun primitiveRef(typeName: String,isNullable: Boolean = false) {
        val pqn = typeName.asPossiblyQualifiedName
        val ti = _namespace.createTypeInstance(null, pqn, emptyList(), isNullable)
        _subtypeList.add(ti)
    }

    fun elementRef(elementTypeName: String,isNullable: Boolean = false) {
        val pqn = elementTypeName.asPossiblyQualifiedName
        val ti = _namespace.createTypeInstance(null, pqn, emptyList(), isNullable)
        _subtypeList.add(ti)
    }

    fun listType(isNullable: Boolean = false, init: TypeInstanceArgBuilder.() -> Unit) {
        val collType = SimpleTypeModelStdLib.List
        val tb = TypeInstanceArgBuilder(null, this._namespace, collType, isNullable,_typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        _subtypeList.add(tu)
    }

    fun listSeparatedType(isNullable: Boolean = false, init: TypeInstanceArgBuilder.() -> Unit) {
        val collType = SimpleTypeModelStdLib.ListSeparated
        val tb = TypeInstanceArgBuilder(null, this._namespace, collType, isNullable,_typeReferences)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        _subtypeList.add(tu)
    }

    fun tupleType(isNullable:Boolean = false, init: TypeInstanceArgNamedBuilder.() -> Unit) {
        val tt = SimpleTypeModelStdLib.TupleType
        val b = TypeInstanceArgNamedBuilder(null, this._namespace, tt, isNullable, _typeReferences)
        b.init()
        val tti = b.build()
        _subtypeList.add(tti)
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        val sts = subtypeNames.map { _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(it))!! }
        val t = _namespace.createUnnamedSupertypeType(sts.map { it.type() })
        _subtypeList.add(t.type())
    }

    fun unnamedSuperType(isNullable:Boolean = false, init: SubtypeListBuilder.() -> Unit) {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSupertypeType(stu)
        _subtypeList.add(t.type(emptyList(), isNullable))
    }

    fun build(): List<TypeInstance> = _subtypeList
}