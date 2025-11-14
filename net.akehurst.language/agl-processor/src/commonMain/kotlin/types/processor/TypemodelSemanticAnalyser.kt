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

package net.akehurst.language.types.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault

internal class TypemodelSemanticAnalyser : SemanticAnalyser<TypesDomain, SentenceContextAny> {

    companion object {
        const val OPTION_INCLUDE_STD = "include-std"

        private val OptionHolder.includeStd get() = this[OPTION_INCLUDE_STD] == "true"
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()


    override fun clear() {
        _issues.clear()
        _resolvedReferences.clear()
    }

    override fun analyse(sentenceIdentity: Any?, asm: TypesDomain, locationMap: LocationMap?, options: SemanticAnalysisOptions<SentenceContextAny>): SemanticAnalysisResult {
        if (asm.options.includeStd) {
            asm.addNamespace(StdLibDefault)
        }
        return SemanticAnalysisResultDefault(_resolvedReferences, _issues)
    }

}