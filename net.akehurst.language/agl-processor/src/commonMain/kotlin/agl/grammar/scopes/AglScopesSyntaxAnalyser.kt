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

import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree

class AglScopesSyntaxAnalyser : SyntaxAnalyser<ScopeModelAgl> {

    data class PropertyValue(
        val asmObject: Any,
        val value: String
    )

    override val locationMap = mutableMapOf<Any, InputLocation>()

    private val issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    override fun clear() {
        this.locationMap.clear()
        this.issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem): SyntaxAnalysisResult<ScopeModelAgl> {
        val asm = this.declarations(sppt.root.asBranch)
        return SyntaxAnalysisResultDefault(asm, issues,locationMap)
    }

    // declarations = rootIdentifiables scopes referencesOpt
    private fun declarations(node: SPPTBranch): ScopeModelAgl {
        val asm = ScopeModelAgl()
        val rootIdentifiables = this.rootIdentifiables(node.branchChild(0))
        val scopes = this.scopes(node.branchChild(1))
        val references = this.referencesOpt(node.branchChild(2))
        asm.scopes[ScopeModelAgl.ROOT_SCOPE_TYPE_NAME]?.identifiables?.addAll(rootIdentifiables)
        scopes.forEach {
            asm.scopes[it.scopeFor] = it
        }
        asm.references.addAll(references)
        locationMap[asm] = node.location
        return asm
    }

    // rootIdentifiables = identifiable*
    private fun rootIdentifiables(node: SPPTBranch): List<Identifiable> {
        return node.branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.identifiable(it.asBranch)
        }
    }

    // scopes = scope+
    private fun scopes(node: SPPTBranch): List<ScopeDefinition> {
        return node.branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.scope(it.asBranch)
        }
    }

    // scope = 'scope' typeReference '{' identifiables '}
    private fun scope(node: SPPTBranch): ScopeDefinition {
        val scopeFor = this.typeReference(node.branchChild(0))
        val identifiables = this.identifiables(node.branchChild(1))
        val scope = ScopeDefinition(scopeFor)
        scope.identifiables.addAll(identifiables)
        locationMap[scope] = node.location
        locationMap[PropertyValue(scope, "typeReference")] = node.branchChild(0).location
        return scope
    }

    // identifiables = identifiable*
    private fun identifiables(node: SPPTBranch): List<Identifiable> {
        return node.branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.identifiable(it.asBranch)
        }
    }

    // identifiable = 'identify' typeReference 'by' propertyReferenceOrNothing
    private fun identifiable(node: SPPTBranch): Identifiable {
        val typeName = this.typeReference(node.branchChild(0))
        val propertyName = this.propertyReferenceOrNothing(node.branchChild(1))
        val identifiable = Identifiable(typeName, propertyName)
        locationMap[identifiable] = node.location
        locationMap[PropertyValue(identifiable, "typeReference")] = node.branchChild(0).location
        locationMap[PropertyValue(identifiable, "propertyName")] = node.branchChild(1).location
        return identifiable
    }

    // referencesOpt = references?
    private fun referencesOpt(node: SPPTBranch): List<ReferenceDefinition> {
        return node.branchNonSkipChildren[0].branchNonSkipChildren.flatMap {
            this.references(it.asBranch)
        }
    }

    // references = 'references' '{' referenceDefinitions '}'
    private fun references(node: SPPTBranch): List<ReferenceDefinition> {
        return this.referenceDefinitions(node.branchChild(0))
    }

    // referenceDefinitions = referenceDefinition*
    private fun referenceDefinitions(node: SPPTBranch): List<ReferenceDefinition> {
        return node.branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.referenceDefinition(it.asBranch)
        }
    }

    // referenceDefinition = 'in' typeReference 'property' propertyReference 'refers-to' typeReferences
    private fun referenceDefinition(node: SPPTBranch): ReferenceDefinition {
        val inTypeName = this.typeReference(node.branchChild(0))
        val referringPropertyName = this.typeReference(node.branchChild(1))
        val typeReferences = this.typeReferences(node.branchChild(2))
        val def = ReferenceDefinition(inTypeName, referringPropertyName, typeReferences.map { it.first })
        this.locationMap[def] = node.location
        locationMap[PropertyValue(def, "in")] = node.branchChild(0).location
        locationMap[PropertyValue(def, "propertyReference")] = node.branchChild(1).location
        typeReferences.forEachIndexed { i, n ->
            locationMap[PropertyValue(def, "typeReferences[$i]")] = n.second
        }
        return def
    }

    // typeReferences = [typeReferences / ',']+
    private fun typeReferences(node: SPPTBranch): List<Pair<String,InputLocation>> {
        return node.branchNonSkipChildren[0].branchNonSkipChildren.mapIndexed { i, n ->
            val ref = this.typeReference(n.asBranch)
            Pair(ref,n.location)
        }
    }

    // propertyReferenceOrNothing = '§nothing' | propertyReference
    private fun propertyReferenceOrNothing(node: SPPTBranch): String {
        val text = node.nonSkipMatchedText
        return when (text) {
            "§nothing" -> ScopeModelAgl.IDENTIFY_BY_NOTHING
            else -> text
        }
    }

    // typeReference = IDENTIFIER
    private fun typeReference(node: SPPTBranch): String {
        return node.nonSkipMatchedText
    }

    // propertyReference = IDENTIFIER
    private fun propertyReference(node: SPPTBranch): String {
        return node.nonSkipMatchedText
    }
}