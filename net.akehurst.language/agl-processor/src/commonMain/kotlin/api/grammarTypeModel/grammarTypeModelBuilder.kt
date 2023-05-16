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

package net.akehurst.language.agl.grammarTypeModel

import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.api.grammarTypeModel.StringType
import net.akehurst.language.typemodel.api.*

fun grammarTypeModel(namespace: String, name: String, init: GrammarTypeModelBuilder.() -> Unit): GrammarTypeModel {
    val b = GrammarTypeModelBuilder(namespace, name)
    b.init()
    val m = b.build()
    return m
}

@TypeModelDslMarker
class GrammarTypeModelBuilder(
    val namespace: String,
    val name: String
) {

    private val _model = GrammarTypeModelSimple(namespace, name)
    private val _typeReferences = mutableListOf<TypeUsageReferenceBuilder>()

    val StringType: PrimitiveType get() = _model.StringType

    fun stringTypeFor(name: String, isNullable: Boolean = false) {
        _model.addTypeFor(name, if (isNullable) _model.StringType.useNullable else _model.StringType.use)
    }

    fun listTypeFor(name: String, elementType: TypeDefinition): TypeUsage {
        val t = ListSimpleType.ofType(TypeUsage.ofType(elementType))
        _model.addTypeFor(name, t)
        return t
    }

    fun listTypeOf(name: String, elementTypeName: String): TypeUsage {
        val elementType = _model.findOrCreateElementTypeNamed(elementTypeName)!!
        return listTypeFor(name, elementType)
    }

    fun listSeparatedTypeFor(name: String, itemType: TypeUsage, separatorType: TypeUsage) {
        val t = ListSeparatedType.ofType(itemType, separatorType)
        _model.addTypeFor(name, t)
    }

    fun listSeparatedTypeFor(name: String, itemType: TypeDefinition, separatorType: TypeDefinition) =
        listSeparatedTypeFor(name, TypeUsage.ofType(itemType), TypeUsage.ofType(separatorType))

    fun listSeparatedTypeOf(name: String, itemTypeName: String, separatorType: TypeDefinition) {
        val itemType = _model.findOrCreateElementTypeNamed(itemTypeName)!!
        listSeparatedTypeFor(name, itemType, separatorType)
    }

    fun listSeparatedTypeOf(name: String, itemTypeName: String, separatorTypeName: String) {
        val itemType = _model.findOrCreateElementTypeNamed(itemTypeName)!!
        val separatorType = _model.findOrCreateElementTypeNamed(separatorTypeName)!!
        listSeparatedTypeFor(name, itemType, separatorType)
    }

    fun elementType(grammarRuleName: String, typeName: String, init: ElementTypeBuilder.() -> Unit = {}): ElementType {
        val b = ElementTypeBuilder(_model, _typeReferences, typeName)
        b.init()
        val et = b.build()
        _model.addTypeFor(grammarRuleName, TypeUsage.ofType(et))
        return et
    }

    fun unnamedSuperTypeTypeFor(name: String, subtypes: List<Any>): UnnamedSuperTypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _model.findOrCreateElementTypeNamed(it)!!
                is TypeDefinition -> it
                else -> error("Cannot map to TypeDefinition: $it")
            }
        }
        val t = UnnamedSuperTypeType(sts.map { TypeUsage.ofType(it) }, false)
        _model.addTypeFor(name, TypeUsage.ofType(t))
        return t
    }

    fun build(): GrammarTypeModel {
        _typeReferences.forEach {
            it.resolve()
        }
        return _model
    }
}