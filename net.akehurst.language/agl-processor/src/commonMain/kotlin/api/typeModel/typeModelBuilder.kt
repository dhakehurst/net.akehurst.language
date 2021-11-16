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

fun typeModel( init: TypeModelBuilder.() -> Unit): TypeModel {
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
class ElementTypeBuilder(
    private val _model: TypeModel,
    private val _elementName: String
) {
    private val _elementType = _model.findOrCreateType(_elementName) as ElementType


    fun superType(superTypeName: String) {
        val st = _model.findOrCreateType(superTypeName)
        _elementType.addSuperType(st as ElementType)
    }

    fun propertyUnnamedStringType(isNullable:Boolean, childIndex:Int, ) {
        PropertyDeclaration(_elementType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, BuiltInType.STRING,isNullable,childIndex)
    }

    fun propertyStringType(propertyName: String, isNullable:Boolean,childIndex:Int) {
        PropertyDeclaration(_elementType, propertyName, BuiltInType.STRING,isNullable,childIndex)
    }

    fun propertyUnnamedListType(isNullable:Boolean,childIndex:Int) {
        PropertyDeclaration(_elementType, TypeModelFromGrammar.UNNAMED_STRING_PROPERTY_NAME, BuiltInType.LIST,isNullable,childIndex)
    }

    fun propertyListOfStringType(propertyName: String, isNullable:Boolean,childIndex:Int) {
        //TODO: listElementType
        PropertyDeclaration(_elementType, propertyName, BuiltInType.LIST,isNullable,childIndex)
    }

    fun propertyListType(propertyName: String, listElementType: String, isNullable:Boolean, childIndex:Int) {
        //TODO: listElementType
        PropertyDeclaration(_elementType, propertyName, BuiltInType.LIST,isNullable, childIndex)
    }

    fun propertyElementType(propertyName: String, elementTypeName: String, isNullable:Boolean, childIndex:Int) {
        val t = _model.findOrCreateType(elementTypeName)
        PropertyDeclaration(_elementType, propertyName, t,isNullable, childIndex)
    }

    fun build(): ElementType {
        return _elementType
    }
}