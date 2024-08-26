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

import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple

fun grammarTypeModel(
    namespaceQualifiedName: String,
    modelName: String,
    imports: List<TypeNamespace> = listOf(SimpleTypeModelStdLib),
    init: GrammarTypeModelBuilder.() -> Unit
): TypeModel {
    val model = TypeModelSimple(modelName)
    imports.forEach { model.addNamespace(it) }
    val b = GrammarTypeModelBuilder(model, namespaceQualifiedName, imports.map { it.qualifiedName }.toMutableList())
    b.init()
    val ns = b.build()
    model.addNamespace(ns)
    model.resolveImports()
    return model
}

@TypeModelDslMarker
class GrammarTypeModelBuilder(
    typeModel: TypeModel,
    namespaceQualifiedName: String,
    imports: MutableList<String>
) {
    private val _namespace = GrammarTypeNamespaceSimple(namespaceQualifiedName, imports).also {
        it.resolveImports(typeModel)
    }
    private val _typeReferences = mutableListOf<TypeUsageReferenceBuilder>()

    val StringType: PrimitiveType get() = SimpleTypeModelStdLib.String.declaration as PrimitiveType

    fun stringTypeFor(grammarRuleName: String, isNullable: Boolean = false) {
        _namespace.addTypeFor(grammarRuleName, if (isNullable) SimpleTypeModelStdLib.String.nullable() else SimpleTypeModelStdLib.String)
    }

    fun listTypeFor(grammarRuleName: String, elementType: TypeDeclaration): TypeInstance {
        val t = SimpleTypeModelStdLib.List.type(listOf(elementType.type()))
        _namespace.addTypeFor(grammarRuleName, t)
        return t
    }

    fun listTypeOf(grammarRuleName: String, elementTypeName: String): TypeInstance {
        val elementType = _namespace.findOwnedOrCreateDataTypeNamed(elementTypeName)!!
        return listTypeFor(grammarRuleName, elementType)
    }

    fun listSeparatedTypeFor(grammarRuleName: String, itemType: TypeInstance, separatorType: TypeInstance) {
        val t = SimpleTypeModelStdLib.ListSeparated.type(listOf(itemType, separatorType))
        _namespace.addTypeFor(grammarRuleName, t)
    }

    fun listSeparatedTypeFor(grammarRuleName: String, itemType: TypeDeclaration, separatorType: TypeDeclaration) =
        listSeparatedTypeFor(grammarRuleName, itemType.type(), separatorType.type())

    fun listSeparatedTypeOf(grammarRuleName: String, itemTypeName: String, separatorType: TypeDeclaration) {
        val itemType = _namespace.findOwnedOrCreateDataTypeNamed(itemTypeName)!!
        listSeparatedTypeFor(grammarRuleName, itemType, separatorType)
    }

    fun listSeparatedTypeOf(grammarRuleName: String, itemTypeName: String, separatorTypeName: String) {
        val itemType = _namespace.findOwnedOrCreateDataTypeNamed(itemTypeName)!!
        val separatorType = _namespace.findOwnedOrCreateDataTypeNamed(separatorTypeName)!!
        listSeparatedTypeFor(grammarRuleName, itemType, separatorType)
    }

    fun dataType(grammarRuleName: String, typeName: String, init: DataTypeBuilder.() -> Unit = {}): DataType {
        val b = DataTypeBuilder(_namespace, _typeReferences, typeName)
        b.init()
        val et = b.build()
        _namespace.addTypeFor(grammarRuleName, et.type())
        return et
    }

    fun unnamedSuperTypeTypeOf(grammarRuleName: String, subtypes: List<Any>): UnnamedSupertypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _namespace.findOwnedOrCreateDataTypeNamed(it)!!
                is TypeDeclaration -> it
                else -> error("Cannot map to TypeDefinition: $it")
            }
        }
        val t = _namespace.createUnnamedSupertypeType(sts.map { it.type() })
        _namespace.addTypeFor(grammarRuleName, t.type())
        return t
    }

    fun unnamedSuperTypeType(grammarRuleName: String, init: SubtypeListBuilder.() -> Unit): UnnamedSupertypeType {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSupertypeType(stu)
        _namespace.addTypeFor(grammarRuleName, t.type())
        return t
    }

    fun build(): GrammarTypeNamespace {
        return _namespace
    }
}