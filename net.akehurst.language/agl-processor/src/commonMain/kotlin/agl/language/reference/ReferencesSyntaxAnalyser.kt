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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.language.expressions.ExpressionsSyntaxAnalyser
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.Navigation
import net.akehurst.language.api.language.reference.DeclarationsForNamespace
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList

class ReferencesSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<CrossReferenceModelDefault>() {

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
        super.register(this::externalTypes)
        super.register(this::referenceDefinitions)
        super.register(this::referenceDefinition)
        super.register(this::referenceExpression)
        super.register(this::propertyReferenceExpression)
        super.register(this::from)
        super.register(this::collectionReferenceExpression)
        super.register(this::ofType)
        super.register(this::typeReferences)
        super.register(this::typeReference)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    override val extendsSyntaxAnalyser: Map<String, SyntaxAnalyser<*>> = mapOf(
        "Expressions" to ExpressionsSyntaxAnalyser()
    )

    // unit = namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CrossReferenceModelDefault {
        val namespace = children as List<DeclarationsForNamespace>
        val result = CrossReferenceModelDefault()
        namespace.forEach { result.declarationsForNamespace[it.qualifiedName] = it }
        return result
    }

    // namespace = 'namespace' qualifiedName '{' declarations '}' ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): DeclarationsForNamespace {
        val qualifiedName = children[1] as List<String>
        val declarations = children[3] as (DeclarationsForNamespace) -> Unit
        val result = DeclarationsForNamespaceDefault(qualifiedName.joinToString(separator = "."))
        declarations.invoke(result)
        return result
    }

    // declarations = rootIdentifiables scopes referencesOpt
    private fun declarations(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (DeclarationsForNamespaceDefault) -> Unit {
        val rootIdentifiables = children[0] as List<IdentifiableDefault>
        val scopes = children[1] as List<ScopeDefinitionDefault>
        val referencesOpt = children[2] as Pair<List<String>, List<ReferenceDefinitionDefault>>?
        return { decl ->
            decl.scopes[CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME]?.identifiables?.addAll(rootIdentifiables)
            scopes.forEach { decl.scopes[it.scopeForTypeName] = it }
            referencesOpt?.let {
                decl.externalTypes.addAll(referencesOpt.first)
                decl.references.addAll(referencesOpt.second)
            }
        }
    }

    // rootIdentifiables = identifiable*
    private fun rootIdentifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<IdentifiableDefault> =
        children as List<IdentifiableDefault>

    // scopes = scope* ;
    private fun scopes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ScopeDefinitionDefault> =
        children as List<ScopeDefinitionDefault>

    // scope = 'scope' typeReference '{' identifiables '}
    private fun scope(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeDefinitionDefault {
        val typeReference = children[1] as String
        val identifiables = children[3] as List<IdentifiableDefault>
        val scope = ScopeDefinitionDefault(typeReference)
        scope.identifiables.addAll(identifiables)
        locationMap[PropertyValue(scope, "typeReference")] = locationMap[typeReference]!!
        return scope
    }

    // identifiables = identifiable*
    private fun identifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<IdentifiableDefault> =
        (children as List<IdentifiableDefault>?) ?: emptyList()

    // identifiable = 'identify' typeReference 'by' expression
    private fun identifiable(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IdentifiableDefault {
        val typeReference = children[1] as String
        val expression = children[3] as Expression
        val identifiable = IdentifiableDefault(typeReference, expression)
        locationMap[PropertyValue(identifiable, "typeReference")] = locationMap[typeReference]!!
        locationMap[PropertyValue(identifiable, "expression")] = locationMap[expression]!!
        return identifiable
    }

    // referencesOpt = references?
    private fun referencesOpt(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault>? =
        children[0] as List<ReferenceDefinitionDefault>?

    // references = 'references' '{' externalTypes? referenceDefinitions '}' ;
    private fun references(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<List<String>?, List<ReferenceDefinitionDefault>> =
        Pair(children[2] as List<String>? ?: emptyList(), children[3] as List<ReferenceDefinitionDefault>)

    // externalTypes = 'external-types' [typeReference / ',']+ ;
    private fun externalTypes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> =
        (children[1] as List<String>).toSeparatedList<String, String, String>().items

    // referenceDefinitions = referenceDefinition*
    private fun referenceDefinitions(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault> =
        children as List<ReferenceDefinitionDefault>

    // referenceDefinition = 'in' typeReference '{' referenceExpressionList '}'
    private fun referenceDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceDefinitionDefault {
        val inTypeName = children[1] as String
        val referenceExpressionList = children[3] as List<ReferenceExpressionAbstract>
        val def = ReferenceDefinitionDefault(inTypeName, referenceExpressionList)
        //locationMap[PropertyValue(def, "in")] = locationMap[inTypeName]!!
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

    //propertyReferenceExpression = 'property' navigation 'refers-to' typeReferences from? ;
    private fun propertyReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): PropertyReferenceExpressionDefault {
        val navigation = children[1] as Navigation
        val typeReferences = children[3] as List<String>
        val from = children[4] as Navigation?
        return PropertyReferenceExpressionDefault(navigation, typeReferences, from)
    }

    // from = 'from' navigation
    private fun from(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Navigation =
        children[1] as Navigation

    // collectionReferenceExpression = 'forall' navigation ofType? '{' referenceExpressionList '}' ;
    private fun collectionReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CollectionReferenceExpressionDefault {
        val navigation = children[1] as Navigation
        val referenceExpression = children[4] as List<ReferenceExpressionAbstract>
        val ofType = children[2] as String?
        return CollectionReferenceExpressionDefault(navigation, ofType, referenceExpression)
    }

    // ofType = 'of-type' typeReference ;
    private fun ofType(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[1] as String


    // typeReferences = [typeReferences / ',']+
    private fun typeReferences(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> { //List<Pair<String, InputLocation>> {
        return (children as List<String>).toSeparatedList<String, String, String>().items
//            .mapIndexed { i, n ->
//            val ref = n as String
//            Pair(ref, sentence.locationFor(nodeInfo.node))
//        }
    }

    // typeReference = IDENTIFIER
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    // qualifiedName = [IDENTIFIER / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return (children as List<String>).toSeparatedList<String, String, String>().items
    }
}