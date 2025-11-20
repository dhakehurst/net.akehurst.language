/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.types.api.TypesDomain

class M2mTransformSemanticAnalyser : SemanticAnalyser<M2mTransformDomain, SentenceContextAny> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: M2mTransformDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContextAny>
    ): SemanticAnalysisResult {
        val context = options.context
        when {
            null == context -> _issues.warn(null, "No context provided, either provide one or switch off Semantic Analysis.")
            else -> {
                asm.allTransformRuleSet.forEach { def ->
                    def.domainParameters.entries.forEach { (k, v) ->
                        val td = (context.findItemsNamedConformingTo(v.value) { true }).firstOrNull()?.item
                        when (td) {
                            null -> _issues.error(null, "TypesDomain '${v.value}' not found in context for parameter '${k}'")
                            !is TypesDomain -> _issues.error(null, "TypesDomain '${v.value}' is not a TypesDomain in the context, rather a '${td::class.simpleName}'")
                            else -> def.resolveDomainParameter(k, td)
                        }
                    }
                    def.rule.forEach { (k, v) ->
                        v.domainSignature.forEach { (dk, dv) ->
                            val typesDomain = def.domainParameterResolved[dv.domainRef]
                            if (null == typesDomain) {
                                _issues.error(null, "TypesDomain '${dv.domainRef}' not found in rule '${k}'")
                            } else {
                                 dv.variable.resolveType(typesDomain)
                            }
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }
}