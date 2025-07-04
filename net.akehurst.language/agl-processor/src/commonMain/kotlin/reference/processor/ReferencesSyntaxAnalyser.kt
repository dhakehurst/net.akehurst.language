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

package net.akehurst.language.reference.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.api.CrossReferenceNamespace
import net.akehurst.language.reference.asm.*
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.ParsePath
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class ReferencesSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<CrossReferenceModel>() {

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::declarations)
        super.register(this::rootIdentifiables)
        super.register(this::scopes)
        super.register(this::scope)
        super.register(this::identifiables)
        super.register(this::identifiable)
        super.register(this::referencesOpt)
        super.register(this::references)
        super.register(this::referenceDefinitions)
        super.register(this::referenceDefinition)
        super.register(this::referenceExpressionList)
        super.register(this::referenceExpression)
        super.register(this::propertyReferenceExpression)
        super.register(this::from)
        super.register(this::collectionReferenceExpression)
        super.register(this::rootOrNavigation)
        super.register(this::ofType)
        super.register(this::typeReferences)
        super.register(this::possiblyQualifiedTypeReference)
        super.register(this::simpleTypeName)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    //  Base::unit = options* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CrossReferenceModelDefault {
        val options = children[0] as List<Pair<String, String>>
        val namespace = children[1] as List<CrossReferenceNamespace>

        val optHolder = OptionHolderDefault(null, options.associate { it.first to it.second })
        val result = CrossReferenceModelDefault(SimpleName("Unit"), optHolder, namespace)
        //namespace.forEach { result.declarationsForNamespace[it.qualifiedName] = it }
        return result
    }

    // override namespace = namespace = 'namespace' possiblyQualified import* declarations  ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CrossReferenceNamespace {
        val qualifiedName = children[1] as PossiblyQualifiedName
        //TODO: parse options
        val imports = children[2] as List<Import>
        val declarations = children[3] as (CrossReferenceNamespace) -> DeclarationsForNamespaceDefault

        val ns = CrossReferenceNamespaceDefault(qualifiedName = qualifiedName.asQualifiedName(null), import = imports)
        val def = declarations.invoke(ns)
        return ns
    }

    // declarations = rootIdentifiables scopes referencesOpt
    private fun declarations(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (CrossReferenceNamespace) -> DeclarationsForNamespaceDefault {
        val rootIdentifiables = children[0] as List<IdentifiableDefault>
        val scopes = children[1] as List<ScopeDefinitionDefault>
        val referencesOpt = children[2] as List<ReferenceDefinitionDefault>?
        return { ns ->
            val decl = DeclarationsForNamespaceDefault(ns)
            (decl.scopeDefinition[CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME.last]?.identifiables as MutableList?)?.addAll(rootIdentifiables)
            scopes.forEach { decl.scopeDefinition[it.scopeForTypeName] = it }
            referencesOpt?.let { decl.references.addAll(referencesOpt) }
            decl.also { setLocationFor(it,nodeInfo,sentence) }
        }
    }

    // rootIdentifiables = identifiable*
    private fun rootIdentifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<IdentifiableDefault> =
        children as List<IdentifiableDefault>

    // scopes = scope* ;
    private fun scopes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ScopeDefinitionDefault> =
        children as List<ScopeDefinitionDefault>

    // scope = 'scope' simpleTypeName '{' identifiables '}
    private fun scope(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeDefinitionDefault {
        val simpleTypeName = children[1] as SimpleName
        val identifiables = children[3] as List<IdentifiableDefault>
        val scope = ScopeDefinitionDefault(simpleTypeName)
        scope.identifiables.addAll(identifiables)
        locationMap.add(ParsePath(), PropertyValue(scope, "typeReference"), locationMap[simpleTypeName]!!)
        return scope
    }

    // identifiables = identifiable*
    private fun identifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<IdentifiableDefault> =
        (children as List<IdentifiableDefault>?) ?: emptyList()

    // identifiable = 'identify' simpleTypeName 'by' expression
    private fun identifiable(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IdentifiableDefault {
        val simpleTypeName = children[1] as SimpleName
        val expression = children[3] as Expression
        val identifiable = IdentifiableDefault(simpleTypeName, expression)
        locationMap.add(ParsePath(), PropertyValue(identifiable, "simpleTypeName"), locationMap[simpleTypeName]!!)
        locationMap.add(ParsePath(), PropertyValue(identifiable, "expression"), locationMap[expression]!!)
//        locationMap[PropertyValue(identifiable, "simpleTypeName")] = locationMap[simpleTypeName]!!
//        locationMap[PropertyValue(identifiable, "expression")] = locationMap[expression]!!
        return identifiable
    }

    // referencesOpt = references?
    private fun referencesOpt(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault>? =
        children[0] as List<ReferenceDefinitionDefault>?

    // references = 'references' '{' referenceDefinitions '}' ;
    private fun references(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault> =
        children[2] as List<ReferenceDefinitionDefault>

    // referenceDefinitions = referenceDefinition*
    private fun referenceDefinitions(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault> =
        children as List<ReferenceDefinitionDefault>

    // referenceDefinition = 'in' simpleTypeName '{' referenceExpressionList '}'
    private fun referenceDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceDefinitionDefault {
        val inTypeName = children[1] as SimpleName
        val referenceExpressionList = children[3] as List<ReferenceExpressionAbstract>
        val def = ReferenceDefinitionDefault(inTypeName, referenceExpressionList)
//        locationMap[PropertyValue(def, "in")] = locationMap[inTypeName]!!
        locationMap.add(ParsePath(), PropertyValue(def, "in"), locationMap[inTypeName]!!)
        // locationMap[PropertyValue(def, "propertyReference")] = locationMap[referringPropertyName]!!
        //typeReferences.forEachIndexed { i, n ->
        //    locationMap[PropertyValue(def, "typeReferences[$i]")] = n.second
        //}
        return def
    }

    // referenceExpressionList = referenceExpression* ;
    private fun referenceExpressionList(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceExpressionAbstract> =
        children as List<ReferenceExpressionAbstract>

    //referenceExpression = propertyReferenceExpression | collectionReferenceExpression ;
    private fun referenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceExpressionAbstract =
        children[0] as ReferenceExpressionAbstract

    //propertyReferenceExpression = 'property' rootOrNavigation 'refers-to' typeReferences from? ;
    private fun propertyReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceExpressionPropertyDefault {
        val expr = children[1] as Expression
        val typeReferences = children[3] as List<PossiblyQualifiedName>
        val from = children[4] as NavigationExpression?

        val navigation = when (expr) {
            is NavigationExpression -> expr
            is RootExpression -> NavigationExpressionDefault(RootExpressionDefault(expr.name), emptyList())
            else -> {
                issues.error(locationMap[expr], "", null)
                NavigationExpressionDefault(RootExpressionDefault.ERROR, emptyList())
            }
        }
        return ReferenceExpressionPropertyDefault(navigation, typeReferences, from).also {
            typeReferences.forEachIndexed { i, n ->
//                locationMap[PropertyValue(it, "typeReferences[$i]")] = locationMap[n]!!
                locationMap.add(ParsePath(), PropertyValue(it, "typeReferences[$i]"), locationMap[n]!!)
            }
        }
    }

    // from = 'from' navigation
    private fun from(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationExpression =
        children[1] as NavigationExpression

    // collectionReferenceExpression = 'forall' rootOrNavigation ofType? '{' referenceExpressionList '}' ;
    private fun collectionReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceExpressionCollectionDefault {
        val rootOrNavigation = children[1] as Expression
        val referenceExpression = children[4] as List<ReferenceExpressionAbstract>
        val ofType = children[2] as PossiblyQualifiedName?
        return ReferenceExpressionCollectionDefault(rootOrNavigation, ofType, referenceExpression)
    }

    // ofType = 'of-type' possiblyQualifiedTypeReference ;
    private fun ofType(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[1] as PossiblyQualifiedName

    //rootOrNavigation = root | navigation ;
    private fun rootOrNavigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Expression =
        children[0] as Expression

    // typeReferences = [possiblyQualifiedTypeReference / '|']+
    private fun typeReferences(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<PossiblyQualifiedName> { //List<Pair<String, InputLocation>> {
        return (children as List<Any>).toSeparatedList<Any, PossiblyQualifiedName, String>().items
    }

    // possiblyQualifiedTypeReference = possiblyQualifiedName
    private fun possiblyQualifiedTypeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PossiblyQualifiedName =
        children[0] as PossiblyQualifiedName

    // simpleTypeName = IDENTIFIER ;
    private fun simpleTypeName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): SimpleName =
        SimpleName((children[0] as String))

}