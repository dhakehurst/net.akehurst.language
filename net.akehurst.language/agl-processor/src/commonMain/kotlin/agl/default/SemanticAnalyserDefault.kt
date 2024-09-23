/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.default_

import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.asm.AsmPath
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.scope.Scope
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.typemodel.api.TypeModel

class SemanticAnalyserDefault(
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModel
) : SemanticAnalyser<Asm, ContextAsmDefault> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private lateinit var _locationMap: Map<Any, InputLocation>

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        asm: Asm,
        locationMap: Map<Any, InputLocation>?,
        context: ContextAsmDefault?,
        options: SemanticAnalysisOptions<Asm, ContextAsmDefault>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: emptyMap<Any, InputLocation>()

        when {
            null == context -> _issues.info(null, "No context provided, references not checked or resolved, switch off reference checking or provide a context.")
            options.checkReferences.not() -> _issues.info(null, "Semantic Analysis option 'checkReferences' is off, references not checked.")
            crossReferenceModel.isEmpty -> _issues.warn(null, "Empty CrossReferenceModel")
            else -> {
                this.buildScope(asm, context.rootScope)
                val resolve = if (options.resolveReferences) {
                    true
                } else {
                    _issues.info(null, "Semantic Analysis option 'resolveReferences' is off, references checked but not resolved.")
                    false
                }
                this.walkReferences(asm, _locationMap, context.rootScope, resolve)
            }
        }
        return SemanticAnalysisResultDefault(this._issues)
    }

    private fun walkReferences(asm: Asm, locationMap: Map<Any, InputLocation>, rootScope: Scope<AsmPath>, resolve: Boolean) {
        val resolveFunction: ResolveFunction? = if (resolve) {
            { ref -> asm.elementIndex[ref] }
        } else {
            null
        }
        asm.traverseDepthFirst(ReferenceResolverDefault(typeModel, crossReferenceModel, rootScope, resolveFunction, locationMap, _issues))
    }

    private fun buildScope(asm: Asm, rootScope: Scope<AsmPath>) {
        val scopeCreator = ScopeCreator(typeModel, crossReferenceModel as CrossReferenceModelDefault, rootScope, _locationMap, _issues)
        asm.traverseDepthFirst(scopeCreator)
    }

}