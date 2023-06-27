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
package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.ScopeSimple
import net.akehurst.language.agl.syntaxAnalyser.createReferenceLocalToScope
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.collections.mutableStackOf

class SemanticAnalyserSimple(
    val scopeModel: ScopeModel?
) : SemanticAnalyser<AsmSimple, ContextSimple> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _scopeModel = scopeModel as ScopeModelAgl?
    private lateinit var _locationMap: Map<*, InputLocation>

    override fun clear() {
        _issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        //TODO: pass grammar as context ?
        return emptyList()
    }

    override fun analyse(asm: AsmSimple, locationMap: Map<Any, InputLocation>?, context: ContextSimple?, options: Map<String, Any>): SemanticAnalysisResult {
        this._locationMap = locationMap ?: emptyMap<Any, InputLocation>()

        this.buildScope(asm, context?.rootScope)

        asm.rootElements.forEach { resolveReferences(it, locationMap, context) }

        return SemanticAnalysisResultDefault(this._issues)
    }

    private fun resolveReferences(o: Any?, locationMap: Map<Any, InputLocation>?, context: ContextSimple?) {
        when (o) {
            is AsmElementSimple -> _scopeModel?.resolveReferencesElement(_issues, o, locationMap, context?.rootScope)
            is List<*> -> o.forEach { resolveReferences(it, locationMap, context) }
        }
    }

    private fun buildScope(asm: AsmSimple, rootScope: ScopeSimple<AsmElementPath>?) {
        if (null == rootScope) {
            //
        } else {
            asm.traverseDepthFirst(object : AsmSimpleTreeWalker() {

                val currentScope = mutableStackOf(rootScope)

                override fun root(root: AsmElementSimple) {
                    addToScope(currentScope.peek(), root)
                }

                override fun beforeElement(propertyName: String?, element: AsmElementSimple) {
                    val scope = currentScope.peek()
                    addToScope(scope, element)
                    val chScope = createScope(scope, element)
                    currentScope.push(chScope)
                }

                override fun afterElement(propertyName: String?, element: AsmElementSimple) {
                    currentScope.pop()
                }

                override fun property(property: AsmElementProperty) {
                    // do nothing
                }

            })
        }

    }

    private fun createScope(scope: ScopeSimple<AsmElementPath>, el: AsmElementSimple): ScopeSimple<AsmElementPath> {
        return if (_scopeModel!!.isScopeDefinition(el.typeName)) {
            val refInParent = _scopeModel!!.createReferenceLocalToScope(scope, el)
            if (null != refInParent) {
                val newScope = scope.createOrGetChildScope(refInParent, el.typeName, el.asmPath)
                //_scopeMap[el.asmPath] = newScope
                newScope
            } else {
                _issues.error(
                    this._locationMap[el],
                    "Trying to create child scope but cannot create a reference for $el"
                )
                scope
            }
        } else {
            scope
        }
    }

    private fun addToScope(scope: ScopeSimple<AsmElementPath>, el: AsmElementSimple) {
        if (_scopeModel!!.shouldCreateReference(scope.forTypeName, el.typeName)) {
            //val reference = _scopeModel!!.createReferenceFromRoot(scope, el)
            val scopeLocalReference = _scopeModel!!.createReferenceLocalToScope(scope, el)
            if (null != scopeLocalReference) {
                val contextRef = el.asmPath
                scope.addToScope(scopeLocalReference, el.typeName, contextRef)
            } else {
                _issues.error(this._locationMap[el], "Cannot create a local reference in '$scope' for $el")
            }
        } else {
            // no need to add it to scope
        }
    }


}