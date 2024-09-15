/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.agl.language.asmTransform

import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.language.asmTransform.TransformModel
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.parser.api.InputLocation

class AsmTransformSemanticAnalyser() : SemanticAnalyser<TransformModel, ContextFromGrammar> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)

    override fun clear() {

    }

    override fun analyse(
        asm: TransformModel,
        locationMap: Map<Any, InputLocation>?,
        context: ContextFromGrammar?,
        options: SemanticAnalysisOptions<TransformModel, ContextFromGrammar>
    ): SemanticAnalysisResult {
        return SemanticAnalysisResultDefault(_issues)
    }
}