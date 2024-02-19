/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.language.reference.asm.builder

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.reference.asm.*
import net.akehurst.language.api.language.expressions.NavigationExpression
import net.akehurst.language.api.language.reference.*

@DslMarker
annotation class CrossReferenceModelBuilderMarker

fun crossReferenceModel(init: CrossReferenceModelBuilder.() -> Unit): CrossReferenceModel {
    val b = CrossReferenceModelBuilder()
    b.init()
    return b.build()
}

@CrossReferenceModelBuilderMarker
class CrossReferenceModelBuilder(

) {

    private var _declarationsFor = mutableListOf<DeclarationsForNamespace>()

    fun declarationsFor(namespaceQualifiedName: String, init: DeclarationsForNamespaceBuilder.() -> Unit) {
        val b = DeclarationsForNamespaceBuilder(namespaceQualifiedName)
        b.init()
        _declarationsFor.add(b.build())
    }

    fun build(): CrossReferenceModel {
        val result = CrossReferenceModelDefault()
        _declarationsFor.forEach {
            result.declarationsForNamespace[it.qualifiedName] = it
        }
        return result
    }
}

@CrossReferenceModelBuilderMarker
class DeclarationsForNamespaceBuilder(
    private val _qualifiedName: String
) {
    private val _importedNamespaces = mutableListOf<String>()
    private val _references = mutableListOf<ReferenceDefinition>()
    private val _scopes = mutableListOf<ScopeDefinition>()


    fun import(namespaceQualifiedName: String) {
        _importedNamespaces.add(namespaceQualifiedName)
    }

    fun scope(forTypeName: String, init: ScopeDefinitionBuilder.() -> Unit) {
        val b = ScopeDefinitionBuilder(forTypeName)
        b.init()
        _scopes.add(b.build())
    }

    fun reference(inType: String, init: ReferenceDefinitionBuilder.() -> Unit) {
        val b = ReferenceDefinitionBuilder(inType)
        b.init()
        _references.add(b.build())
    }

    fun build(): DeclarationsForNamespace {
        val result = DeclarationsForNamespaceDefault(_qualifiedName, _importedNamespaces)
        _scopes.forEach { result.scopeDefinition[it.scopeForTypeName] = it }
        result.references.addAll(_references)
        return result
    }
}

@CrossReferenceModelBuilderMarker
class ScopeDefinitionBuilder(
    private val _forTypeName: String
) {
    private val _identifiables = mutableListOf<Identifiable>()

    fun identify(typeName: String, expressionStr: String) {
        val exprResult = Agl.registry.agl.expressions.processor!!.process(expressionStr)
        check(exprResult.issues.errors.isEmpty()) { exprResult.issues.toString() }
        val expression = exprResult.asm ?: error("No expression created from given expressionStr")
        val i = IdentifiableDefault(typeName, expression)
        _identifiables.add(i)
    }

    fun build(): ScopeDefinition {
        val result = ScopeDefinitionDefault(_forTypeName)
        result.identifiables.addAll(_identifiables)
        return result
    }
}

@CrossReferenceModelBuilderMarker
class ReferenceDefinitionBuilder(
    private val _inType: String
) {
    val _refExpressionList = mutableListOf<ReferenceExpression>()

    fun property(referringPropertyStr: String, refersToTypes: List<String>, fromExpressionStr: String?) {
        val exprResult = Agl.registry.agl.expressions.processor!!.process(referringPropertyStr)
        check(exprResult.issues.errors.isEmpty()) { exprResult.issues.toString() }
        val refPropNav = exprResult.asm?.let { if (it is NavigationExpression) it else null } ?: error("Navigation not created from given referringPropertyStr")

        val fromNav = fromExpressionStr?.let {
            val fromResult = Agl.registry.agl.expressions.processor!!.process(it)
            check(fromResult.issues.errors.isEmpty()) { fromResult.issues.toString() }
            fromResult.asm?.let { if (it is NavigationExpression) it else null } ?: error("Navigation not created from given fromExpressionStr")
        }

        _refExpressionList.add(PropertyReferenceExpressionDefault(refPropNav, refersToTypes, fromNav))
    }

    fun collection() {

    }

    fun build(): ReferenceDefinition {
        return ReferenceDefinitionDefault(_inType, _refExpressionList)
    }
}