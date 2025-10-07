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
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain

class M2mTransformSemanticAnalyser : SemanticAnalyser<M2mTransformDomain, ContextWithScope<Any, Any>> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    override fun clear() {

    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: M2mTransformDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<ContextWithScope<Any, Any>>
    ): SemanticAnalysisResult {
        val context = options.context
        when {
            null == context -> _issues.warn(null, "No context provided, either provide one or switch off Semantic Analysis.")
            else -> {
                asm.allDefinitions.forEach { def ->
                    val typeDomains = def.domainParameters
                    def.rule.forEach { (k, v) ->
                        v.domainItem.forEach { (dk, dv) ->
                            val typesDomainName = typeDomains[dv.domainRef]
                            if (null == typesDomainName) {
                                _issues.error(null, "TypeDomain '${dv.domainRef}' not found for rule '${k}'")
                            } else {
                                val tm = (context.findItemsNamedConformingTo(typesDomainName.value,) { true }).firstOrNull()?.item
                                when(tm) {
                                    null -> _issues.error(null, "TypeModel '${typesDomainName.value}' not found for rule '${k}'")
                                    !is TypesDomain -> _issues.error(null, "TypeModel '${typesDomainName.value}' is not a TypesDomain for rule '${k}'")
                                    else -> dv.variable.resolveType(tm)
                                }
                            }
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }
}