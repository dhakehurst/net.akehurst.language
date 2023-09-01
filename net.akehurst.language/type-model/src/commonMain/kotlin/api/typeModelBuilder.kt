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

import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple
import kotlin.reflect.KClass

@DslMarker
annotation class TypeModelDslMarker

fun typeModel(
    namespace: String,
    name: String,
    rootTypeName: String? = null,
    imports: Set<TypeModel> = setOf(SimpleTypeModelStdLib),
    init: TypeModelBuilder.() -> Unit
): TypeModel {
    val b = TypeModelBuilder(namespace, name, rootTypeName, imports)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeModelBuilder(
    val namespace: String,
    val name: String,
    val rootTypeName: String?,
    imports: Set<TypeModel>
) {

    private val _model = TypeModelSimple(namespace, name, rootTypeName, imports)
    private val _typeReferences = mutableListOf<TypeUsageReferenceBuilder>()

    fun primitiveType(typeName: String): PrimitiveType = _model.findOrCreatePrimitiveTypeNamed(typeName)

    /**
     * create a list type of the indicated typeName
     * if typeName is not already defined, it will be defined as an ElementType
     */
    fun listTypeOf(typeName: String): TypeUsage {
        val elementType = _model.findTypeNamed(typeName) ?: _model.findOrCreateElementTypeNamed(typeName)
        return ListSimpleType.ofType(TypeUsage.ofType(elementType))
    }

    fun listSeparatedTypeOf(itemType: TypeDefinition, separatorType: TypeDefinition): TypeUsage {
        return ListSeparatedType.ofType(TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType))
    }

    fun listSeparatedTypeOf(itemTypeName: String, separatorType: TypeDefinition): TypeUsage {
        val itemType = _model.findTypeNamed(itemTypeName) ?: _model.findOrCreateElementTypeNamed(itemTypeName)
        return ListSeparatedType.ofType(TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType))
    }

    fun listSeparatedTypeOf(itemTypeName: String, separatorTypeName: String): TypeUsage {
        val itemType = _model.findTypeNamed(itemTypeName) ?: _model.findOrCreateElementTypeNamed(itemTypeName)
        val separatorType = _model.findTypeNamed(separatorTypeName) ?: _model.findOrCreateElementTypeNamed(separatorTypeName)
        return listSeparatedTypeOf(itemType, separatorType)
    }

    fun elementType(typeName: String, init: ElementTypeBuilder.() -> Unit = {}): ElementType {
        val b = ElementTypeBuilder(_model, _typeReferences, typeName)
        b.init()
        val et = b.build()
        return et
    }

    fun unnamedSuperTypeTypeFor(subtypes: List<Any>): UnnamedSuperTypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _model.findOrCreateElementTypeNamed(it)!!
                is TypeDefinition -> it
                else -> error("Cannot map to TypeDefinition: $it")
            }
        }
        val t = _model.createUnnamedSuperTypeType(sts.map { TypeUsage.ofType(it) })
        return t
    }

    fun build(): TypeModel {
        _typeReferences.forEach {
            it.resolve()
        }
        return _model
    }
}

@TypeModelDslMarker
abstract class StructuredTypeBuilder(
    protected val _model: TypeModel,
    private val _typeReferences: MutableList<TypeUsageReferenceBuilder>
) {
    protected abstract val _structuredType: StructuredRuleType

    fun propertyPrimitiveType(propertyName: String, typeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, TypeUsage.ofType(this._model.findTypeNamed(typeName)!!, emptyList(), isNullable), childIndex)

    fun propertyListTypeOf(propertyName: String, elementTypeName: String, nullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(propertyName, nullable, childIndex) {
            elementTypeOf(elementTypeName)
        }

    fun propertyListType(propertyName: String, nullable: Boolean, childIndex: Int, init: TypeUsageReferenceBuilder.() -> Unit): PropertyDeclaration {
        val tb = TypeUsageReferenceBuilder(this._model, ListSimpleType, nullable)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        return property(propertyName, tu, childIndex)
    }

    // ListSeparated
    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _model.findTypeNamed(itemTypeName) ?: _model.findOrCreateElementTypeNamed(itemTypeName)!!
        val separatorType = _model.findTypeNamed(itemTypeName) ?: _model.findOrCreateElementTypeNamed(separatorTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorType: TypeDefinition, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _model.findOrCreateElementTypeNamed(itemTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, itemType: TypeDefinition, separatorType: TypeDefinition, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, ListSeparatedType.ofType(TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType), isNullable), childIndex)

    // Tuple
    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}): PropertyDeclaration {
        val b = TupleTypeBuilder(_model, _typeReferences)
        b.init()
        val tt = b.build()
        val t = ListSimpleType.ofType(TypeUsage.ofType(tt))
        return property(propertyName, t, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit): PropertyDeclaration {
        val b = TupleTypeBuilder(_model, _typeReferences)
        b.init()
        val tt = b.build()
        return property(propertyName, TypeUsage.ofType(tt), childIndex)
    }

    fun propertyUnnamedSuperType(propertyName: String, isNullable: Boolean, childIndex: Int, init: SubtypeListBuilder.() -> Unit): PropertyDeclaration {
        val b = SubtypeListBuilder(_model, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _model.createUnnamedSuperTypeType(stu)
        return property(propertyName, TypeUsage.ofType(t, emptyList(), isNullable), childIndex)
    }

    //
    fun propertyElementTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val t = _model.findOrCreateElementTypeNamed(elementTypeName)!!
        return property(propertyName, TypeUsage.ofType(t, emptyList(), isNullable), childIndex)
    }

    fun property(propertyName: String, typeUse: TypeUsage, childIndex: Int): PropertyDeclaration {
        check(childIndex == _structuredType.property.size) { "Incorrect property index" }
        return PropertyDeclaration(_structuredType, propertyName, typeUse, childIndex)
    }
}

@TypeModelDslMarker
class TupleTypeBuilder(
    _model: TypeModel,
    _typeReferences: MutableList<TypeUsageReferenceBuilder>
) : StructuredTypeBuilder(_model, _typeReferences) {

    override val _structuredType = TupleType()

    fun build(): TupleType {
        return _structuredType
    }
}

@TypeModelDslMarker
class ElementTypeBuilder(
    _model: TypeModel,
    _typeReferences: MutableList<TypeUsageReferenceBuilder>,
    _elementName: String
) : StructuredTypeBuilder(_model, _typeReferences) {

    private val _elementType = _model.findOrCreateElementTypeNamed(_elementName) as ElementType
    override val _structuredType: StructuredRuleType get() = _elementType

    fun typeParameters(vararg parameters: String) {
        (_elementType as ElementType).typeParameters.addAll(parameters)
    }

    fun superType(superTypeName: String) {
        val st = _model.findOrCreateElementTypeNamed(superTypeName)
        _elementType.addSuperType(st as ElementType)
    }

    fun subTypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val st = _model.findOrCreateElementTypeNamed(it) as ElementType
            st.addSuperType(_elementType)
        }
    }

    fun build(): ElementType {
        return _elementType
    }

}

class TypeUsageReferenceBuilder(
    val _model: TypeModel,
    val type: TypeDefinition,
    val nullable: Boolean
) {
    private val _refs = mutableListOf<String>()
    private val _args = mutableListOf<TypeUsage>()
    private lateinit var _refClass: KClass<*>

    fun primitiveType(typeName: String) {
        _refClass = PrimitiveType::class
        _refs.add(typeName)
    }

    fun elementTypeOf(typeName: String) {
        _refClass = ElementType::class
        _refs.add(typeName)
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        _refClass = UnnamedSuperTypeType::class
        _refs.addAll(subtypeNames)
    }

    fun build(): TypeUsage {
        return TypeUsage.ofType(type, _args, false)
    }

    fun resolve() {
        when (_refClass) {
            PrimitiveType::class -> {
                val ref = _refs.first()
                val t = _model.findTypeNamed(ref) ?: error("Type not found: '$ref'")
                _args.add(TypeUsage.ofType(t, emptyList(), nullable))
            }

            ElementType::class -> {
                val ref = _refs.first()
                val t = _model.findTypeNamed(ref) ?: error("Type not found: '$ref'")
                _args.add(TypeUsage.ofType(t, emptyList(), nullable))
            }

            UnnamedSuperTypeType::class -> {
                val subtypes = _refs.map { _model.findTypeNamed(it) ?: error("Type not found: '$it'") }
                val stu = subtypes.map { TypeUsage.ofType(it) }
                val t = _model.createUnnamedSuperTypeType(stu)
                _args.add(TypeUsage.ofType(t, emptyList(), nullable))
            }

            else -> error("Not handled: ${_refClass.simpleName}")
        }
    }
}

@TypeModelDslMarker
class SubtypeListBuilder(
    val _model: TypeModel,
    private val _typeReferences: MutableList<TypeUsageReferenceBuilder>
) {

    val _subtypeList = mutableListOf<TypeUsage>()

    fun primitiveRef(typeName: String) {
        val t = _model.findTypeNamed(typeName) ?: error("Type not found: '$typeName'")
        _subtypeList.add(t.typeUse())
    }

    fun elementRef(elementTypeName: String) {
        val t = _model.findOrCreateElementTypeNamed(elementTypeName) as ElementType
        _subtypeList.add(TypeUsage.ofType(t))
    }

    fun listType(nullable: Boolean, init: TypeUsageReferenceBuilder.() -> Unit) {
        val tb = TypeUsageReferenceBuilder(this._model, ListSimpleType, nullable)
        tb.init()
        _typeReferences.add(tb)
        val tu = tb.build()
        _subtypeList.add(tu)
    }

    fun tupleType(init: TupleTypeBuilder.() -> Unit) {
        val b = TupleTypeBuilder(_model, mutableListOf())
        b.init()
        val t = b.build()
        _subtypeList.add(TypeUsage.ofType(t))
    }

    fun unnamedSuperTypeOf(vararg subtypeNames: String) {
        val sts = subtypeNames.map { _model.findOrCreateElementTypeNamed(it)!! }
        val t = _model.createUnnamedSuperTypeType(sts.map { TypeUsage.ofType(it) })
        _subtypeList.add(TypeUsage.ofType(t))
    }

    fun unnamedSuperType(init: SubtypeListBuilder.() -> Unit) {
        val b = SubtypeListBuilder(_model, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _model.createUnnamedSuperTypeType(stu)
        _subtypeList.add(TypeUsage.ofType(t))
    }

    fun build(): List<TypeUsage> = _subtypeList
}