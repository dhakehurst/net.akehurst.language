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

package net.akehurst.language.agl.language.scopes

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.semanticAnalyser.DeclarationsForNamespace
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList

class AglScopesSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<ScopeModelAgl>() {

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
        super.register(this::referenceExpression)
        super.register(this::propertyReferenceExpression)
        super.register(this::from)
        super.register(this::collectionReferenceExpression)
        super.register(this::navigation)
        super.register(this::ofType)
        super.register(this::typeReferences)
        super.register(this::navigationOrNothing)
        super.register(this::typeReference)
        super.register(this::propertyReference)
        super.register(this::qualifiedName)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<ScopeModelAgl>> = emptyMap()

//    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//        return emptyList()
//    }

    // unit = namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeModelAgl {
        val namespace = children as List<DeclarationsForNamespace>
        val result = ScopeModelAgl()
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
        val referencesOpt = children[2] as List<ReferenceDefinitionDefault>?
        return { decl ->
            decl.scopes[ScopeModelAgl.ROOT_SCOPE_TYPE_NAME]?.identifiables?.addAll(rootIdentifiables)
            scopes.forEach { decl.scopes[it.scopeForTypeName] = it }
            referencesOpt?.let { decl.references.addAll(referencesOpt) }
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

    // identifiable = 'identify' typeReference 'by' navigationOrNothing
    private fun identifiable(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): IdentifiableDefault {
        val typeReference = children[1] as String
        val navigation = children[3] as NavigationDefault
        val identifiable = IdentifiableDefault(typeReference, navigation)
        locationMap[PropertyValue(identifiable, "typeReference")] = locationMap[typeReference]!!
        locationMap[PropertyValue(identifiable, "navigation")] = locationMap[navigation]!!
        return identifiable
    }

    // referencesOpt = references?
    private fun referencesOpt(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault>? =
        children[0] as List<ReferenceDefinitionDefault>?

    // references = 'references' '{' referenceDefinitions '}'
    private fun references(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinitionDefault> =
        children[2] as List<ReferenceDefinitionDefault>

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
        val navigation = children[1] as NavigationDefault
        val typeReferences = children[3] as List<String>
        val from = children[4] as NavigationDefault?
        return PropertyReferenceExpressionDefault(navigation, typeReferences, from)
    }

    // from = 'from' navigation
    private fun from(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationDefault =
        children[1] as NavigationDefault

    // collectionReferenceExpression = 'forall' navigation ofType? '{' referenceExpressionList '}' ;
    private fun collectionReferenceExpression(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): CollectionReferenceExpressionDefault {
        val navigation = children[1] as NavigationDefault
        val referenceExpression = children[4] as List<ReferenceExpressionAbstract>
        val ofType = children[2] as String?
        return CollectionReferenceExpressionDefault(navigation, ofType, referenceExpression)
    }

    // ofType = 'of-type' typeReference ;
    private fun ofType(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[1] as String

    //navigation = [propertyReference / '.']+ ;
    private fun navigation(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationDefault =
        NavigationDefault(children.toSeparatedList<String, String>().items)

    // typeReferences = [typeReferences / ',']+
    private fun typeReferences(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> { //List<Pair<String, InputLocation>> {
        return children.toSeparatedList<String, String>().items
//            .mapIndexed { i, n ->
//            val ref = n as String
//            Pair(ref, sentence.locationFor(nodeInfo.node))
//        }
    }

    // navigationOrNothing = '§nothing' | navigation
    private fun navigationOrNothing(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): NavigationDefault {
        val res = children[0] as Any
        return when (res) {
            is NavigationDefault -> res
            is String -> NavigationDefault(ScopeModelAgl.IDENTIFY_BY_NOTHING)
            else -> error("type '${res::class.simpleName}' not handled")
        }
    }

    // typeReference = IDENTIFIER
    private fun typeReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    // propertyReference = IDENTIFIER
    private fun propertyReference(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        return children[0] as String
    }

    // qualifiedName = [IDENTIFIER / '.']+ ;
    private fun qualifiedName(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return children.toSeparatedList<String, String>().items
    }
}