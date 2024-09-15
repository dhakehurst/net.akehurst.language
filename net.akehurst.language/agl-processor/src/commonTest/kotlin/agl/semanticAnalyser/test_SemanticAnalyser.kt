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

package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import kotlin.test.Test

class test_SemanticAnalyser {

    class TestSemanticAnalyser : SemanticAnalyser<Any, Any> {
        override fun clear() {
            TODO("not implemented")
        }

//        override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//            TODO("not implemented")
//        }

        override fun analyse(asm: Any, locationMap: Map<Any, InputLocation>?, context: Any?, options: SemanticAnalysisOptions<Any, Any>): SemanticAnalysisResult {
            val ih = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
            when (asm) {
                "error" -> ih.error(null, "error")
                "warning" -> ih.warn(null, "warning")
                else -> throw RuntimeException("Test Error")
            }
            return SemanticAnalysisResultDefault(ih)
        }
    }

    @Test
    fun error() {
        val asm = Any()
        val sut = TestSemanticAnalyser()
        //val actual = sut.analyse("error", options=Agl.options<Any,Any> {  })
        TODO()
    }
}