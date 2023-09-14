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
    name: String,
    rootTypeName: String,
    imports: List<TypeNamespace> = listOf(SimpleTypeModelStdLib),
    init: GrammarTypeModelBuilder.() -> Unit
): TypeModel {
    val model = TypeModelSimple(name)
    val b = GrammarTypeModelBuilder(namespaceQualifiedName, imports.map { it.qualifiedName }.toMutableList())
    b.init()
    val ns = b.build()
    imports.forEach { model.addNamespace(it) }
    model.addNamespace(ns)
    model.resolveImports()
    return model
}

@TypeModelDslMarker
class GrammarTypeModelBuilder(
    namespaceQualifiedName: String,
    imports: MutableList<String>
) {
    private val _namespace = GrammarTypeNamespaceSimple(namespaceQualifiedName, imports)
    private val _typeReferences = mutableListOf<TypeUsageReferenceBuilder>()

    val StringType: PrimitiveType get() = SimpleTypeModelStdLib.String.type as PrimitiveType

    fun stringTypeFor(name: String, isNullable: Boolean = false) {
        _namespace.addTypeFor(name, if (isNullable) SimpleTypeModelStdLib.String.nullable() else SimpleTypeModelStdLib.String)
    }

    fun listTypeFor(name: String, elementType: TypeDefinition): TypeInstance {
        val t = SimpleTypeModelStdLib.List.instance(listOf(elementType.instance()))
        _namespace.addTypeFor(name, t)
        return t
    }

    fun listTypeOf(name: String, elementTypeName: String): TypeInstance {
        val elementType = _namespace.findOrCreateDataTypeNamed(elementTypeName)!!
        return listTypeFor(name, elementType)
    }

    fun listSeparatedTypeFor(name: String, itemType: TypeInstance, separatorType: TypeInstance) {
        val t = SimpleTypeModelStdLib.ListSeparated.instance(listOf(itemType, separatorType))
        _namespace.addTypeFor(name, t)
    }

    fun listSeparatedTypeFor(name: String, itemType: TypeDefinition, separatorType: TypeDefinition) =
        listSeparatedTypeFor(name, itemType.instance(), separatorType.instance())

    fun listSeparatedTypeOf(name: String, itemTypeName: String, separatorType: TypeDefinition) {
        val itemType = _namespace.findOrCreateDataTypeNamed(itemTypeName)!!
        listSeparatedTypeFor(name, itemType, separatorType)
    }

    fun listSeparatedTypeOf(name: String, itemTypeName: String, separatorTypeName: String) {
        val itemType = _namespace.findOrCreateDataTypeNamed(itemTypeName)!!
        val separatorType = _namespace.findOrCreateDataTypeNamed(separatorTypeName)!!
        listSeparatedTypeFor(name, itemType, separatorType)
    }

    fun dataType(grammarRuleName: String, typeName: String, init: DataTypeBuilder.() -> Unit = {}): DataType {
        val b = DataTypeBuilder(_namespace, _typeReferences, typeName)
        b.init()
        val et = b.build()
        _namespace.addTypeFor(grammarRuleName, et.instance())
        return et
    }

    fun unnamedSuperTypeTypeOf(name: String, subtypes: List<Any>): UnnamedSuperTypeType {
        val sts = subtypes.map {
            when (it) {
                is String -> _namespace.findOrCreateDataTypeNamed(it)!!
                is TypeDefinition -> it
                else -> error("Cannot map to TypeDefinition: $it")
            }
        }
        val t = _namespace.createUnnamedSuperTypeType(sts.map { it.instance() })
        _namespace.addTypeFor(name, t.instance())
        return t
    }

    fun unnamedSuperTypeType(name: String, init: SubtypeListBuilder.() -> Unit): UnnamedSuperTypeType {
        val b = SubtypeListBuilder(_namespace, _typeReferences)
        b.init()
        val stu = b.build()
        val t = _namespace.createUnnamedSuperTypeType(stu)
        _namespace.addTypeFor(name, t.instance())
        return t
    }

    fun build(): GrammarTypeNamespace {
        return _namespace
    }
}