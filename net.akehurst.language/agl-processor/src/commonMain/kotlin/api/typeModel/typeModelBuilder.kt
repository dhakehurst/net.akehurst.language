/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.api.typeModel

import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar

@DslMarker
annotation class TypeModelDslMarker

fun typeModel(namespace: String, name: String, init: TypeModelBuilder.() -> Unit): TypeModel {
    val b = TypeModelBuilder(namespace, name)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeModelBuilder(
    val namespace: String,
    val name: String
) {

    private val _types = mutableMapOf<String, RuleType>()
    private fun findOrCreateType(name: String): ElementType {
        val existing = _types[name]
        return if (null == existing) {
            val t = ElementType(_model, name)
            _types[name] = t
            t
        } else {
            existing as ElementType
        }
    }

    private val _model = object : TypeModel {
        override val namespace: String = this@TypeModelBuilder.namespace
        override val name: String = this@TypeModelBuilder.name
        override val types = _types
        override fun findType(name: String): RuleType = findOrCreateType(name)
    }

    fun stringTypeFor(name: String) {
        _types[name] = StringType
    }

    fun elementType(name: String, init: ElementTypeBuilder.() -> Unit = {}): ElementType {
        val b = ElementTypeBuilder(_model, name)
        b.init()
        val el = b.build()

        return el
    }

    fun unnamedSuperTypeTypeFor(name: String, subtypes: List<Any>): UnnamedSuperTypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _model.findType(it)!!
                is RuleType -> it
                else -> error("Cannot map to RuleType: $it")
            }
        }
        val x = UnnamedSuperTypeType(sts)
        _types[name] = x
        return x
    }

    fun build(): TypeModel {
        return _model
    }
}

@TypeModelDslMarker
abstract class StructuredTypeBuilder(
    protected val _model: TypeModel
) {
    protected abstract val _structuredType: StructuredRuleType

    fun propertyAnyTypeUnnamed(isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, AnyType, isNullable, childIndex)

    fun propertyStringTypeUnnamed(isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(TypeModelFromGrammar.UNNAMED_PRIMITIVE_PROPERTY_NAME, StringType, isNullable, childIndex)

    fun propertyStringType(propertyName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, StringType, isNullable, childIndex)

    // ListSimple
    fun propertyListTypeUnnamedOf(elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val elementType = _model.findType(elementTypeName)!!
        return propertyListType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, elementType, isNullable, childIndex)
    }

    fun propertyListTypeUnnamed(elementType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        propertyListType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, elementType, isNullable, childIndex)

    fun propertyListTypeUnnamedOfUnnamedSuperTypeType(subtypeNames: List<String>, childIndex: Int): PropertyDeclaration {
        val subtypes = subtypeNames.map { _model.findType(it)!! }
        val elementType = UnnamedSuperTypeType(subtypes)
        return propertyListType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, elementType, false, childIndex)
    }

    fun propertyListTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val elementType = _model.findType(elementTypeName)!!
        return propertyListType(propertyName, elementType, isNullable, childIndex)
    }

    fun propertyListType(propertyName: String, elementType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, ListSimpleType(elementType), isNullable, childIndex)

    // ListSeparated
    fun propertyUnnamedListSeparatedType(itemType: RuleType, separatorType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, ListSeparatedType(itemType, separatorType), isNullable, childIndex)

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _model.findType(itemTypeName)!!
        val separatorType = _model.findType(separatorTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val itemType = _model.findType(itemTypeName)!!
        return propertyListSeparatedType(propertyName, itemType, separatorType, isNullable, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, itemType: RuleType, separatorType: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration =
        property(propertyName, ListSeparatedType(itemType, separatorType), isNullable, childIndex)

    // Tuple
    fun propertyTupleTypeUnnamed(isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit): PropertyDeclaration =
        propertyTupleType(TypeModelFromGrammar.UNNAMED_GROUP_PROPERTY_NAME, isNullable, childIndex, init)

    fun propertyListUnnamedOfTupleType(isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}) =
        propertyListOfTupleType(TypeModelFromGrammar.UNNAMED_LIST_PROPERTY_NAME, isNullable, childIndex, init)

    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}): PropertyDeclaration {
        val b = TupleTypeBuilder(_model)
        b.init()
        val tt = b.build()
        return property(propertyName, ListSimpleType(tt), isNullable, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit): PropertyDeclaration {
        val b = TupleTypeBuilder(_model)
        b.init()
        val tt = b.build()
        return property(propertyName, tt, isNullable, childIndex)
    }

    //
    fun propertyElementTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        val t = _model.findType(elementTypeName)!!
        return property(propertyName, t, isNullable, childIndex)
    }

    fun property(propertyName: String, type: RuleType, isNullable: Boolean, childIndex: Int): PropertyDeclaration {
        return PropertyDeclaration(_structuredType, propertyName, type, isNullable, childIndex)
    }
}

@TypeModelDslMarker
class TupleTypeBuilder(
    _model: TypeModel
) : StructuredTypeBuilder(_model) {

    override val _structuredType = TupleType()

    fun build(): TupleType {
        return _structuredType
    }
}

@TypeModelDslMarker
class ElementTypeBuilder(
    _model: TypeModel,
    private val _elementName: String
) : StructuredTypeBuilder(_model) {

    private val _elementType = _model.findType(_elementName) as ElementType
    override val _structuredType: StructuredRuleType get() = _elementType

    //fun superType(superTypeName: String) {
    //    val st = _model.findOrCreateType(superTypeName)
    //    _elementType.addSuperType(st as ElementType)
    //}

    fun subTypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val st = _model.findType(it) as ElementType
            st.addSuperType(_elementType)
        }
    }

    fun build(): ElementType {
        return _elementType
    }

}