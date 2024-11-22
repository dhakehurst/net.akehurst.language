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

import net.akehurst.language.agl.processor.SemanticAnalysisOptionsDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_SemanticAnalyser {

    class TestSemanticAnalyser : SemanticAnalyser<Any, Any> {
        override fun clear() {
        }

        override fun analyse(asm: Any, locationMap: Map<Any, InputLocation>?, context: Any?, options: SemanticAnalysisOptions< Any>): SemanticAnalysisResult {
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
    fun warning() {
        val asm = "warning"
        val sut = TestSemanticAnalyser()
        val sares = sut.analyse(asm, options = SemanticAnalysisOptionsDefault())
        assertFalse(sares.issues.isEmpty())
        val expected = listOf(
            LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS,null,"warning")
        )
        assertEquals(expected, sares.issues.toList())
    }

    @Test
    fun error() {
        val asm = "error"
        val sut = TestSemanticAnalyser()
        val sares = sut.analyse(asm, options = SemanticAnalysisOptionsDefault())
        assertFalse(sares.issues.isEmpty())
        val expected = listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,null,"error")
        )
        assertEquals(expected, sares.issues.toList())
    }
}