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

package net.akehurst.language.api.semanticAnalyser

import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.sentence.api.InputLocation

/**
 *
 * A Semantic Analyser, language specific functionality
 *
 */
interface SemanticAnalyser<in AsmType : Any, ContextType : Any> {

    fun clear()

    //fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue>

    fun analyse(
        sentenceIdentity:Any?,
        asm: AsmType,
        locationMap: LocationMap? = null,
        options: SemanticAnalysisOptions<ContextType>
    ): SemanticAnalysisResult
}

