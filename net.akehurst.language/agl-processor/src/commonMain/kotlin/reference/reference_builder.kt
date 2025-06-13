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

package net.akehurst.language.reference.builder

import net.akehurst.language.agl.Agl
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.reference.api.*
import net.akehurst.language.reference.asm.*

@DslMarker
annotation class CrossReferenceModelBuilderMarker

fun crossReferenceModel(name:String, init: CrossReferenceModelBuilder.() -> Unit): CrossReferenceModel {
    val b = CrossReferenceModelBuilder(name)
    b.init()
    return b.build()
}

@CrossReferenceModelBuilderMarker
class CrossReferenceModelBuilder(
    private val _name:String,
) {

    private var _namespaces = mutableListOf<CrossReferenceNamespaceDefault>()

    fun declarationsFor(namespaceQualifiedName: String, init: DeclarationsForNamespaceBuilder.() -> Unit) {
        val ns = CrossReferenceNamespaceDefault(QualifiedName(namespaceQualifiedName))
        val b = DeclarationsForNamespaceBuilder(ns)
        b.init()
        val (def, imps) =b.build()
        imps.forEach { ns.addImport(it) }
        _namespaces.add(ns)
    }

    fun build(): CrossReferenceModel {
        val result = CrossReferenceModelDefault(SimpleName(_name), namespace =  _namespaces)
        return result
    }
}

@CrossReferenceModelBuilderMarker
class DeclarationsForNamespaceBuilder(
    private val namespace: CrossReferenceNamespace
) {
    private val _importedNamespaces = mutableListOf<Import>()
    private val _references = mutableListOf<ReferenceDefinition>()
    private val _scopes = mutableListOf<ScopeDefinition>()


    fun import(namespaceQualifiedName: String) {
        _importedNamespaces.add(Import(namespaceQualifiedName))
    }

    fun scope(forTypeName: String, init: ScopeDefinitionBuilder.() -> Unit) {
        val b = ScopeDefinitionBuilder(SimpleName(forTypeName))
        b.init()
        _scopes.add(b.build())
    }

    fun reference(inType: String, init: ReferenceDefinitionBuilder.() -> Unit) {
        val b = ReferenceDefinitionBuilder(SimpleName(inType))
        b.init()
        _references.add(b.build())
    }

    fun build(): Pair<DeclarationsForNamespace, List<Import>> {
        val result = DeclarationsForNamespaceDefault(namespace)
        _scopes.forEach { result.scopeDefinition[it.scopeForTypeName] = it }
        result.references.addAll(_references)
        return Pair(result,_importedNamespaces)
    }
}

@CrossReferenceModelBuilderMarker
class ScopeDefinitionBuilder(
    private val _forTypeName: SimpleName
) {
    private val _identifiables = mutableListOf<Identifiable>()

    fun identify(typeName: String, expressionStr: String) {
        val exprResult = Agl.registry.agl.expressions.processor!!.process(expressionStr)
        check(exprResult.allIssues.errors.isEmpty()) { exprResult.allIssues.toString() }
        val expression = exprResult.asm ?: error("No expression created from given expressionStr")
        val i = IdentifiableDefault(SimpleName(typeName), expression)
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
    private val _inType: SimpleName
) {
    val _refExpressionList = mutableListOf<ReferenceExpression>()

    fun property(referringPropertyStr: String, refersToTypes: List<String>, fromExpressionStr: String?) {
        val exprResult = Agl.registry.agl.expressions.processor!!.process(referringPropertyStr)
        check(exprResult.allIssues.errors.isEmpty()) { exprResult.allIssues.toString() }
        val refPropNav = exprResult.asm?.let {
            when (it) {
                is NavigationExpression -> it
                is RootExpression -> NavigationExpressionDefault(RootExpressionDefault(it.name), emptyList())
                else -> error("Navigation cannot be created from given referringPropertyStr '$it'")
            }
        } ?: error("Navigation cannot be created from given expression '$fromExpressionStr'")

        val fromNav = fromExpressionStr?.let {
            val fromResult = Agl.registry.agl.expressions.processor!!.process(it)
            check(fromResult.allIssues.errors.isEmpty()) { fromResult.allIssues.toString() }
            fromResult.asm?.let { if (it is NavigationExpression) it else null } ?: error("Navigation not created from given fromExpressionStr")
        }

        _refExpressionList.add(ReferenceExpressionPropertyDefault(refPropNav, refersToTypes.map { it.asPossiblyQualifiedName }, fromNav))
    }

    fun collection() {

    }

    fun build(): ReferenceDefinition {
        return ReferenceDefinitionDefault(_inType, _refExpressionList)
    }
}