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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammarTypemodel.api.GrammarTypesNamespace
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

class ExpressionsSemanticAnalyser(
) : SemanticAnalyser<Expression, SentenceContextAny> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()
    private var _locationMap: LocationMap = LocationMapDefault()

    private var _grammarNamespace: GrammarTypesNamespace? = null

    override fun clear() {
        _grammarNamespace = null
        _locationMap.clear()
        _issues.clear()
        _resolvedReferences.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: Expression,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContextAny>
    ): SemanticAnalysisResult {
        return SemanticAnalysisResultDefault(_resolvedReferences, _issues)
    }

}