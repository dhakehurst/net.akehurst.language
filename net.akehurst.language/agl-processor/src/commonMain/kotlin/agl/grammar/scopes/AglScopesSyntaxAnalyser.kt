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
package net.akehurst.language.agl.grammar.scopes

import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNodeInfo

class AglScopesSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<ScopeModelAgl>() {

    override fun registerHandlers() {
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
        super.register(this::typeReferences)
        super.register(this::propertyReferenceOrNothing)
        super.register(this::typeReference)
        super.register(this::propertyReference)
    }

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    override val embeddedSyntaxAnalyser: Map<String, SyntaxAnalyser<ScopeModelAgl>> = emptyMap()

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    // declarations = rootIdentifiables scopes referencesOpt
    private fun declarations(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeModelAgl {
        val asm = ScopeModelAgl()
        val rootIdentifiables = children[0] as List<Identifiable>
        val scopes = children[1] as List<ScopeDefinition>
        val referencesOpt = children[2] as List<ReferenceDefinition>?
        asm.scopes[ScopeModelAgl.ROOT_SCOPE_TYPE_NAME]?.identifiables?.addAll(rootIdentifiables)
        scopes.forEach { asm.scopes[it.scopeFor] = it }
        referencesOpt?.let { asm.references.addAll(referencesOpt) }
        return asm
    }

    // rootIdentifiables = identifiable*
    private fun rootIdentifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Identifiable> =
        children as List<Identifiable>

    // scopes = scope* ;
    private fun scopes(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ScopeDefinition> =
        children as List<ScopeDefinition>

    // scope = 'scope' typeReference '{' identifiables '}
    private fun scope(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ScopeDefinition {
        val typeReference = children[1] as String
        val identifiables = children[3] as List<Identifiable>
        val scope = ScopeDefinition(typeReference)
        scope.identifiables.addAll(identifiables)
        locationMap[PropertyValue(scope, "typeReference")] = locationMap[typeReference]!!
        return scope
    }

    // identifiables = identifiable*
    private fun identifiables(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Identifiable> =
        (children as List<Identifiable>?) ?: emptyList()

    // identifiable = 'identify' typeReference 'by' propertyReferenceOrNothing
    private fun identifiable(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Identifiable {
        val typeName = children[1] as String
        val propertyName = children[3] as String
        val identifiable = Identifiable(typeName, propertyName)
        locationMap[PropertyValue(identifiable, "typeReference")] = locationMap[typeName]!!
        locationMap[PropertyValue(identifiable, "propertyName")] = locationMap[propertyName]!!
        return identifiable
    }

    // referencesOpt = references?
    private fun referencesOpt(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinition>? =
        children[0] as List<ReferenceDefinition>?

    // references = 'references' '{' referenceDefinitions '}'
    private fun references(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinition> =
        children[2] as List<ReferenceDefinition>

    // referenceDefinitions = referenceDefinition*
    private fun referenceDefinitions(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<ReferenceDefinition> =
        children as List<ReferenceDefinition>

    // referenceDefinition = 'in' typeReference 'property' propertyReference 'refers-to' typeReferences
    private fun referenceDefinition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): ReferenceDefinition {
        val inTypeName = children[1] as String
        val referringPropertyName = children[3] as String
        val typeReferences = children[5] as List<Pair<String, InputLocation>>
        val def = ReferenceDefinition(inTypeName, referringPropertyName, typeReferences.map { it.first })
        locationMap[PropertyValue(def, "in")] = locationMap[inTypeName]!!
        locationMap[PropertyValue(def, "propertyReference")] = locationMap[referringPropertyName]!!
        typeReferences.forEachIndexed { i, n ->
            locationMap[PropertyValue(def, "typeReferences[$i]")] = n.second
        }
        return def
    }

    // typeReferences = [typeReferences / ',']+
    private fun typeReferences(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<String, InputLocation>> {
        return children.toSeparatedList<String, String>().items.mapIndexed { i, n ->
            val ref = n as String
            Pair(ref, sentence.locationFor(nodeInfo.node))
        }
    }

    // propertyReferenceOrNothing = '§nothing' | propertyReference
    private fun propertyReferenceOrNothing(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String {
        val text = children[0] as String
        return when (text) {
            "§nothing" -> ScopeModelAgl.IDENTIFY_BY_NOTHING
            else -> text
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
}