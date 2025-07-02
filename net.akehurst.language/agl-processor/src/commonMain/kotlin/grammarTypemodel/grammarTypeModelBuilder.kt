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

package net.akehurst.language.grammarTypemodel.builder

import net.akehurst.language.base.api.*
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.grammarTypemodel.asm.GrammarTypeNamespaceSimple
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeModelSimple
import net.akehurst.language.typemodel.builder.*

fun TypeModelBuilder.grammarTypeNamespace(
    namespaceQualifiedName: String,
    imports: List<String> = listOf(StdLibDefault.qualifiedName.value),
    resolveImports: Boolean = false,
    init: GrammarTypeNamespaceBuilder.() -> Unit
) {
    val b = GrammarTypeNamespaceBuilder(_model, QualifiedName(namespaceQualifiedName), imports.map { Import(it) }.toMutableList(), resolveImports)
    b.init()
    val ns = b.build()
    _model.addNamespace(ns)
}

@Deprecated("does not allow namespaces",ReplaceWith("typeModel(..) { grammarTypeNamespace(..){...} }"))
fun grammarTypeModel(
    namespaceQualifiedName: String,
    modelName: String,
    imports: List<TypeNamespace> = listOf(StdLibDefault),
    init: GrammarTypeNamespaceBuilder.() -> Unit
): TypeModel {
    val model = TypeModelSimple(SimpleName(modelName))
    imports.forEach { model.addNamespace(it) }
    val b = GrammarTypeNamespaceBuilder(model, QualifiedName(namespaceQualifiedName), imports.map { Import(it.qualifiedName.value) }.toMutableList(), true)
    b.init()
    val ns = b.build()
    model.addNamespace(ns)
    model.resolveImports()
    return model
}

@TypeModelDslMarker
class GrammarTypeNamespaceBuilder(
    typeModel: TypeModel,
    namespaceQualifiedName: QualifiedName,
    imports: MutableList<Import>,
    resolveImports:Boolean
)  : TypeNamespaceBuilder(namespaceQualifiedName,imports) {
    override val _namespace:TypeNamespace = GrammarTypeNamespaceSimple(namespaceQualifiedName, import =  imports).also {
        if(resolveImports) {
            it.resolveImports(typeModel as Model<Namespace<TypeDefinition>, TypeDefinition>)
        }
    }
    private val _grammarNamespace get() = _namespace as GrammarTypeNamespace
    private val _typeReferences = mutableListOf<TypeInstanceArgBuilder>()

    val StringType: PrimitiveType get() = StdLibDefault.String.resolvedDeclaration as PrimitiveType

    fun stringTypeFor(grammarRuleName: String, isNullable: Boolean = false) {
        _grammarNamespace.setTypeForGrammarRule(GrammarRuleName(grammarRuleName), if (isNullable) StdLibDefault.String.nullable() else StdLibDefault.String)
    }

    fun listTypeFor(grammarRuleName: String, elementType: TypeDefinition): TypeInstance {
        val t = StdLibDefault.List.type(listOf(elementType.type().asTypeArgument))
        _grammarNamespace.setTypeForGrammarRule(GrammarRuleName(grammarRuleName), t)
        return t
    }

    fun listTypeOf(grammarRuleName: String, elementTypeName: String): TypeInstance {
        val elementType = _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(elementTypeName))!!
        return listTypeFor(grammarRuleName, elementType)
    }

    fun listSeparatedTypeFor(grammarRuleName: String, itemType: TypeInstance, separatorType: TypeInstance) {
        val t = StdLibDefault.ListSeparated.type(listOf(itemType.asTypeArgument, separatorType.asTypeArgument))
        _grammarNamespace.setTypeForGrammarRule(GrammarRuleName(grammarRuleName), t)
    }

    fun listSeparatedTypeFor(grammarRuleName: String, itemType: TypeDefinition, separatorType: TypeDefinition) =
        listSeparatedTypeFor(grammarRuleName, itemType.type(), separatorType.type())

    fun listSeparatedTypeOf(grammarRuleName: String, itemTypeName: String, separatorType: TypeDefinition) {
        val itemType = _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(itemTypeName))!!
        listSeparatedTypeFor(grammarRuleName, itemType, separatorType)
    }

    fun listSeparatedTypeOf(grammarRuleName: String, itemTypeName: String, separatorTypeName: String) {
        val itemType = _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(itemTypeName))!!
        val separatorType = _namespace.findOwnedOrCreateDataTypeNamed(SimpleName(separatorTypeName))!!
        listSeparatedTypeFor(grammarRuleName, itemType, separatorType)
    }

    fun interfaceFor(grammarRuleName: String, typeName: String, init: InterfaceTypeBuilder.() -> Unit = {}) {
        val tp = super.interface_(typeName, init)
        _grammarNamespace.setTypeForGrammarRule(GrammarRuleName(grammarRuleName), tp.type())
    }

    fun dataFor(grammarRuleName: String, typeName: String, init: DataTypeBuilder.() -> Unit = {}): DataType {
        val tp = super.data(typeName,init)
        _grammarNamespace.setTypeForGrammarRule(GrammarRuleName(grammarRuleName), tp.type())
        return tp
    }

    fun unionFor(grammarRuleName: String, typeName: String, init: SubtypeListBuilder.() -> Unit): UnionType {
        val t = super.union(typeName, init)
        _grammarNamespace.setTypeForGrammarRule(GrammarRuleName(grammarRuleName), t.type())
        return t
    }

}