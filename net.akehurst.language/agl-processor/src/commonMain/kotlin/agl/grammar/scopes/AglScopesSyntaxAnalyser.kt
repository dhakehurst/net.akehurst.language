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

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree

class AglScopesSyntaxAnalyser : SyntaxAnalyser<ScopeModel, ContextSimple> {

    override val locationMap = mutableMapOf<Any, InputLocation>()

    private val issues = mutableListOf<LanguageIssue>()

    override fun clear() {
        this.locationMap.clear()
        this.issues.clear()
    }

    override fun configure(configurationContext: SentenceContext, configuration: String) {
    }

    override fun transform(sppt: SharedPackedParseTree, context: ContextSimple?): Pair<ScopeModel, List<LanguageIssue>> {
        val asm = this.declarations(sppt.root.asBranch)

        if (null!=context) {
            asm.scopes.forEach { scope ->
                val scopeRule = context.scope.findOrNull(scope.scopeFor, "Rule")
                if (null == scopeRule) {
                    val loc = this.locationMap[scope]
                    issues.add(
                        LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, loc, "Rule '${scope.scopeFor}' not found")
                    )
                }
                scope.identifiables.forEach { identifiable ->
                    val propRule = context.scope.findOrNull(identifiable.propertyName,"Rule")
                    TODO()
                }
            }
        }

        return Pair(asm,issues)
    }

    // declarations = scopes references
    private fun declarations(node: SPPTBranch): ScopeModel {
        val asm = ScopeModel()
        val scopes = this.scopes(node.branchChild(0))
        val references = this.references(node.branchChild(1))
        asm.scopes.addAll(scopes)
        asm.references.addAll(references)
        locationMap[asm] = node.location
        return asm
    }

    // scopes = scope+
    private fun scopes(node: SPPTBranch): List<Scope> {
        return node.branchNonSkipChildren.map {
            this.scope(it.asBranch)
        }
    }

    // scope = 'scope' typeReference '{' identifiables '}
    private fun scope(node: SPPTBranch): Scope {
        val scopeFor = this.typeReference(node.branchChild(0))
        val identifiables = this.identifiables(node.branchChild(1))
        val scope = Scope(scopeFor)
        scope.identifiables.addAll(identifiables)
        locationMap[scope] = node.location
        return scope
    }

    // identifiables = identifiable*
    private fun identifiables(node: SPPTBranch): List<Identifiable> {
        return node.branchNonSkipChildren.map {
            this.identifiable(it.asBranch)
        }
    }

    // identifiable = 'identify' typeReference 'by' propertyName
    private fun identifiable(node: SPPTBranch): Identifiable {
        val typeName = this.typeReference(node.branchChild(0))
        val propertyName = this.propertyReference(node.branchChild(1))
        val identifiable = Identifiable(typeName, propertyName)
        locationMap[identifiable] = node.location
        return identifiable
    }

    // references = 'references' '{' referenceDefinitions '}'
    private fun references(node: SPPTBranch): List<ReferenceDefinition> {
        return this.referenceDefinitions(node.branchChild(0))
    }

    // referenceDefinitions = referenceDefinition*
    private fun referenceDefinitions(node: SPPTBranch): List<ReferenceDefinition> {
        return node.branchNonSkipChildren.map {
            this.referenceDefinition(it.asBranch)
        }
    }

    // referenceDefinition = 'in' typeReference 'property' propertyReference 'refers-to' typeReferences
    private fun referenceDefinition(node: SPPTBranch): ReferenceDefinition {
        val inTypeName = this.typeReference(node.branchChild(0))
        val referringPropertyName = this.typeReference(node.branchChild(1))
        val typeReferences = this.typeReferences(node.branchChild(2))
        val def = ReferenceDefinition(inTypeName, referringPropertyName, typeReferences)
        this.locationMap[def] = node.location
        return def
    }

    // typeReferences = [typeReferences / ',']+
    private fun typeReferences(node: SPPTBranch): List<String> {
        return node.branchNonSkipChildren.map {
            this.typeReference(it.asBranch)
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