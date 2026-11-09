/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.completion.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.completion.api.AglCompletionDomain
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

class AglCompletionSemanticAnalyser : SemanticAnalyser<AglCompletionDomain, SentenceContext> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: LocationMap? = null

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: AglCompletionDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: LocationMapDefault()
        //TODO: check the rule names (forItem, context) exist in the grammar of the language, when the context holds it
        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }
}
