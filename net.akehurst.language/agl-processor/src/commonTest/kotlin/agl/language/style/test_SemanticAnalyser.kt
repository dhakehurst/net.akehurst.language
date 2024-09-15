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

package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.api.language.grammar.GrammarModel
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_SemanticAnalyser {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!

        fun grammarFor(grammarStr: String): GrammarModel {
            return Agl.registry.agl.grammar.processor?.process(grammarStr)?.asm!!
        }

        fun test(grammarStr: String, sentence: String, position: Int, expected: List<LanguageIssue>) {
            val testGrammar = grammarFor(grammarStr)
            val context = ContextFromGrammar.createContextFrom(testGrammar)
            val actual = aglProc.process(sentence, Agl.options {
                semanticAnalysis {
                    context(context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(expected.size, actual.issues.size)
            assertEquals(expected.toSet(), actual.issues.toSet())
        }

        fun testFail(grammarStr: String, sentence: String, position: Int, expected: List<LanguageIssue>) {
            val testGrammar = grammarFor(grammarStr)
            val context = ContextFromGrammar.createContextFrom(testGrammar)
            val actual = aglProc.process(sentence, Agl.options {
                semanticAnalysis {
                    context(context)
                }
            })
            assertTrue(actual.issues.errors.isNotEmpty())
            assertEquals(expected.size, actual.issues.size)
            assertEquals(expected.toSet(), actual.issues.toSet())
        }

    }


    @Test
    fun missing_style_grammarRule() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test.Test
            S {}
        """.trimIndent()
        val expected = emptyList<LanguageIssue>()
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun missing_style_grammarRule_fail() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test.Test
            X {}
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(20, 1, 2, 1),
                "Grammar Rule 'X' not found for style rule",
                null
            )
        )
        testFail(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun missing_style_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test.Test
            'a' {}
        """.trimIndent()
        val expected = emptyList<LanguageIssue>()
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun missing_style_literal_fail() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test.Test
            'x' {}
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(20, 1, 2, 3), "Terminal Literal 'x' not found for style rule", null)
        )
        testFail(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun missing_style_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "a" ;
            }
        """
        val sentence = """
            namespace test.Test
            "a" {}
        """.trimIndent()
        val expected = emptyList<LanguageIssue>()
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun missing_style_pattern_fail() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test.Test
            "x" {}
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(20, 1, 2, 3), "Terminal Pattern \"x\" not found for style rule", null)
        )
        testFail(grammarStr, sentence, sentence.length, expected)
    }
}