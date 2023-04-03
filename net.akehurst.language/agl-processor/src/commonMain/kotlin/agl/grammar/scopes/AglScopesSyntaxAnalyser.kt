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
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree

class AglScopesSyntaxAnalyser : SyntaxAnalyser<ScopeModelAgl, SentenceContext<GrammarItem>> {

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

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem, context: SentenceContext<GrammarItem>?): SyntaxAnalysisResult<ScopeModelAgl> {
        val asm = this.declarations(sppt.root.asBranch)

        if (null != context) {
            asm.scopes.forEach { (k, scope) ->
                val msgStart = if (ScopeModelAgl.ROOT_SCOPE_TYPE_NAME == scope.scopeFor) {
                    //do nothing
                    "In root scope"
                } else {
                    if (context.rootScope.isMissing(scope.scopeFor, "GrammarRule")) {
                        val loc = this.locationMap[PropertyValue(scope, "typeReference")]
                        issues.error(loc, "GrammarRule '${scope.scopeFor}' not found for scope")
                    } else {
                        //OK
                    }
                    "In scope for '${scope.scopeFor}' GrammarRule"
                }
                scope.identifiables.forEach { identifiable ->
                    when {
                        context.rootScope.isMissing(identifiable.typeName, "GrammarRule") -> {
                            val loc = this.locationMap[PropertyValue(identifiable, "typeReference")]
                            issues.error(loc, "$msgStart '${identifiable.typeName}' not found as identifiable type")
                        }
                        ScopeModelAgl.IDENTIFY_BY_NOTHING == identifiable.propertyName -> {
                            //OK
                        }
                        else -> {
                            // only check this if the typeName is valid - else it is always invalid
                            //TODO: check this in context of typeName GrammarRule
                            if (context.rootScope.isMissing(identifiable.propertyName, "GrammarRule")) {
                                val loc = this.locationMap[PropertyValue(identifiable, "propertyName")]
                                issues.error(
                                    loc,
                                    "$msgStart '${identifiable.propertyName}' not found for identifying property of '${identifiable.typeName}'"
                                )
                            } else {
                                //OK
                            }
                        }
                    }
                }
            }

            asm.references.forEach { ref ->
                if (context.rootScope.isMissing(ref.inTypeName, "GrammarRule")) {
                    val loc = this.locationMap[PropertyValue(ref, "in")]
                    issues.error(loc, "Referring type GrammarRule '${ref.inTypeName}' not found")
                }
                if (context.rootScope.isMissing(ref.referringPropertyName, "GrammarRule")) {
                    val loc = this.locationMap[PropertyValue(ref, "propertyReference")]
                    issues.error(loc, "For reference in '${ref.inTypeName}' referring property GrammarRule '${ref.referringPropertyName}' not found")
                }
                ref.refersToTypeName.forEachIndexed { i, n ->
                    if (context.rootScope.isMissing(n, "GrammarRule")) {
                        val loc = this.locationMap[PropertyValue(ref, "typeReferences[$i]")]
                        issues.error(loc, "For reference in '${ref.inTypeName}' referred to type GrammarRule '$n' not found")
                    }
                }
            }
        }

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
        return node.branchNonSkipChildren.map {
            this.identifiable(it.asBranch)
        }
    }

    // scopes = scope+
    private fun scopes(node: SPPTBranch): List<ScopeDefinition> {
        return node.branchNonSkipChildren.map {
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
        return node.branchNonSkipChildren.map {
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
        return node.branchNonSkipChildren.flatMap {
            this.references(it.asBranch)
        }
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
        locationMap[PropertyValue(def, "in")] = node.branchChild(0).location
        locationMap[PropertyValue(def, "propertyReference")] = node.branchChild(1).location
        node.branchChild(2).branchNonSkipChildren.forEachIndexed { i, n ->
            locationMap[PropertyValue(def, "typeReferences[$i]")] = n.location
        }
        return def
    }

    // typeReferences = [typeReferences / ',']+
    private fun typeReferences(node: SPPTBranch): List<String> {
        return node.branchNonSkipChildren.map {
            this.typeReference(it.asBranch)
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