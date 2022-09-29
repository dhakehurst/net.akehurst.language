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

fun typeModel(init: TypeModelBuilder.() -> Unit): TypeModel {
    val b = TypeModelBuilder()
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class TypeModelBuilder {

    private val _types = mutableMapOf<String, ElementType>()
    private fun findOrCreateType(name: String): ElementType {
        val existing = _types[name]
        return if (null == existing) {
            val t = ElementType(name)
            _types[name] = t
            t
        } else {
            existing
        }
    }

    private val _model = object : TypeModel {
        override val types = _types
        override fun findType(name: String): RuleType? = findOrCreateType(name)
    }

    fun elementType(name: String, init: ElementTypeBuilder.() -> Unit = {}): ElementType {
        val b = ElementTypeBuilder(_model, name)
        b.init()
        val el = b.build()

        return el
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

    fun propertyUnnamedType(type: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, type, isNullable, childIndex)
    }

    fun propertyStringType(propertyName: String, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, propertyName, PrimitiveType.STRING, isNullable, childIndex)
    }

    fun propertyUnnamedListTypeOf(elementType: String, isNullable: Boolean, childIndex: Int) {
        val t = _model.findType(elementType)!!
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, ListSimpleType(t), isNullable, childIndex)
    }

    fun propertyUnnamedListType(elementType: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, ListSimpleType(elementType), isNullable, childIndex)
    }

    fun propertyUnnamedListSeparatedType(itemType: RuleType, separatorType: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, ListSeparatedType(itemType, separatorType), isNullable, childIndex)
    }

    fun propertyListType(propertyName: String, elementType: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, propertyName, ListSimpleType(elementType), isNullable, childIndex)
    }

    fun propertyListSeparatedType(propertyName: String, itemType: RuleType, separatorType: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, propertyName, ListSeparatedType(itemType, separatorType), isNullable, childIndex)
    }

    fun propertyListTypeOf(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int) {
        val t = _model.findType(elementTypeName)!!
        PropertyDeclaration(_structuredType, propertyName, ListSimpleType(t), isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorTypeName: String, isNullable: Boolean, childIndex: Int) {
        val itemType = _model.findType(itemTypeName)!!
        val separatorType = _model.findType(separatorTypeName)!!
        PropertyDeclaration(_structuredType, propertyName, ListSeparatedType(itemType, separatorType), isNullable, childIndex)
    }

    fun propertyListSeparatedTypeOf(propertyName: String, itemTypeName: String, separatorType: PrimitiveType, isNullable: Boolean, childIndex: Int) {
        val itemType = _model.findType(itemTypeName)!!
        PropertyDeclaration(_structuredType, propertyName, ListSeparatedType(itemType, separatorType), isNullable, childIndex)
    }

    fun propertyListOfTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}) {
        val b = TupleTypeBuilder(_model)
        b.init()
        val tt = b.build()
        PropertyDeclaration(_structuredType, propertyName, ListSimpleType(tt), isNullable, childIndex)
    }

    fun propertyUnnamedTupleType(isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit) =
        propertyTupleType(net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar.Companion.UNNAMED_GROUP_PROPERTY_NAME, isNullable, childIndex, init)

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit) {
        val b = TupleTypeBuilder(_model)
        b.init()
        val tt = b.build()
        PropertyDeclaration(_structuredType, propertyName, tt, isNullable, childIndex)
    }

    fun propertyElementType(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int) {
        val t = _model.findType(elementTypeName)!!
        PropertyDeclaration(_structuredType, propertyName, t, isNullable, childIndex)
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