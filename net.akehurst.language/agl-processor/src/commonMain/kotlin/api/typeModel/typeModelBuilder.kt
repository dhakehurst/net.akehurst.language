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

    private val _model = TypeModel()

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
    protected abstract val _structuredType:StructuredRuleType

    fun propertyUnnamedType(type: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, type, isNullable, childIndex)
    }

    fun propertyStringType(propertyName: String, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, propertyName, BuiltInType.STRING, isNullable, childIndex)
    }

    fun propertyUnnamedListTypeOf(listElementTypeName: String, isNullable: Boolean, childIndex: Int) {
        val t = _model.findOrCreateType(listElementTypeName)
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, ListType(t), isNullable, childIndex)
    }

    fun propertyUnnamedListType(listElementType: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, ListType(listElementType), isNullable, childIndex)
    }

    fun propertyListTypeOf(propertyName: String, listElementTypeName: String, isNullable: Boolean, childIndex: Int) {
        val t = _model.findOrCreateType(listElementTypeName)
        PropertyDeclaration(_structuredType, propertyName, ListType(t), isNullable, childIndex)
    }

    fun propertyListType(propertyName: String, listElementType: RuleType, isNullable: Boolean, childIndex: Int) {
        PropertyDeclaration(_structuredType, propertyName, ListType(listElementType), isNullable, childIndex)
    }

    fun propertyTupleType(propertyName: String, isNullable: Boolean, childIndex: Int, init: TupleTypeBuilder.() -> Unit = {}) {
        val b = TupleTypeBuilder(_model)
        b.init()
        val tt = b.build()
        PropertyDeclaration(_structuredType, propertyName, tt, isNullable, childIndex)
    }

    fun propertyElementType(propertyName: String, elementTypeName: String, isNullable: Boolean, childIndex: Int) {
        val t = _model.findOrCreateType(elementTypeName)
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

    private val _elementType = _model.findOrCreateType(_elementName) as ElementType
    override val _structuredType: StructuredRuleType get() = _elementType

    //fun superType(superTypeName: String) {
    //    val st = _model.findOrCreateType(superTypeName)
    //    _elementType.addSuperType(st as ElementType)
    //}

    fun subTypes(vararg elementTypeName: String) {
        elementTypeName.forEach {
            val st = _model.findOrCreateType(it)
            st.addSuperType(_elementType)
        }
    }

    fun build(): ElementType {
        return _elementType
    }

}