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
import net.akehurst.language.agl.processor.contextFromRegistryStyles
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.style.api.AglStyleTagRule
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_SemanticAnalyser {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!

        fun grammarFor(grammarStr: String): GrammarDomain {
            return Agl.registry.agl.grammar.processor?.process(grammarStr)?.asm!!
        }

        fun test(grammarStr: String, sentence: String, position: Int, expected: List<LanguageIssue>) {
            val testGrammar = grammarFor(grammarStr)
            val context = contextFromGrammar(testGrammar)
            val actual = aglProc.process(sentence, Agl.options {
                semanticAnalysis {
                    sentenceContext(context)
                }
            })
            assertTrue(actual.allIssues.errors.isEmpty(), actual.allIssues.toString())
            assertEquals(expected.size, actual.allIssues.size)
            assertEquals(expected.toSet(), actual.allIssues.toSet())
        }

        fun testFail(grammarStr: String, sentence: String, position: Int, expected: List<LanguageIssue>) {
            val testGrammar = grammarFor(grammarStr)
            val context = contextFromGrammar(testGrammar)
            val actual = aglProc.process(sentence, Agl.options {
                semanticAnalysis {
                    sentenceContext(context)
                }
            })
            assertTrue(actual.allIssues.errors.isNotEmpty())
            assertEquals(expected.size, actual.allIssues.size)
            assertEquals(expected.toSet(), actual.allIssues.toSet())
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
            namespace test
            styles Test {
              S {}
            }
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
            namespace test
            styles Test {
              X {}
            }
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(31, 3, 3, 1, null),
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
            namespace test
            styles Test {
              'a' {}
            }
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
            namespace test
            styles Test {
              'x' {}
            }
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(31, 3, 3, 3, null), "Terminal Literal 'x' not found for style rule", null)
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
            namespace test
            styles Test {
              "a" {}
            }
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
            namespace test
            styles Test {
              "x" {}
            }
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(31, 3, 3, 3, null), "Terminal Pattern \"x\" not found for style rule", null)
        )
        testFail(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun metaRule_matches_terminal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' '{' ;
            }
        """
        val sentence = $$"""
            namespace test
            styles Test {
              $$ "'[a-z]+'" {}
            }
        """.trimIndent()
        test(grammarStr, sentence, sentence.length, emptyList())
    }

    @Test
    fun metaRule_matches_no_terminal_warning() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' '{' ;
            }
        """
        val sentence = $$"""
            namespace test
            styles Test {
              $$ "'[0-9]+'" {}
            }
        """.trimIndent()
        val testGrammar = grammarFor(grammarStr)
        val actual = aglProc.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammar(testGrammar)) } })
        assertTrue(actual.allIssues.errors.isEmpty(), actual.allIssues.toString())
        assertEquals(listOf($$"Meta rule pattern \"'[0-9]+'\" matches no terminal"), actual.allIssues.warnings.map { it.message })
    }

    // issue #20, ER5: a style set named after no grammar gives a warning, unless another style set extends it
    @Test
    fun styleSet_not_named_after_a_grammar__warning() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test
            styles Tset {
              S {}
            }
        """.trimIndent()
        val expected = listOf(
            LanguageIssue(
                LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(15, 1, 2, 22, null),
                "Style set 'test.Tset' is not named after a grammar of the language, so its rules are not used. Expected one of 'test.Test'"
            )
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun styleSet_not_named_after_a_grammar_but_extended__no_warning() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = """
            namespace test
            styles Base {
              S {}
            }
            styles Test : Base {
            }
        """.trimIndent()
        // 'Base' is found in the same style file; no style set name warning is given for it
        val context = contextFromGrammar(grammarFor(grammarStr))
        val actual = aglProc.process(sentence, Agl.options { semanticAnalysis { sentenceContext(context) } })
        assertTrue(actual.allIssues.isEmpty(), actual.allIssues.toString())
    }

    // an extended style set is found in the same style file, not only in the sentence context
    private fun processWithGrammar(grammarStr: String, sentence: String) =
        aglProc.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammar(grammarFor(grammarStr))) } })

    private val grammarAB = """
        namespace test
        grammar Test {
            S = A B ;
            A = 'a' ;
            B = 'b' ;
        }
    """

    @Test
    fun extends_styleSet_in_same_file() {
        val sentence = """
            namespace test
            styles Base {
              A { foreground: red; }
            }
            styles Test : Base {
              B { foreground: blue; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertTrue(actual.allIssues.isEmpty(), actual.allIssues.toString())
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals(listOf("A", "B"), test.allRules.filterIsInstance<AglStyleTagRule>().map { it.selector.first().value })
    }

    @Test
    fun extends_styleSet_in_same_file__defined_after() {
        val sentence = """
            namespace test
            styles Test : Base {
              B { foreground: blue; }
            }
            styles Base {
              A { foreground: red; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertTrue(actual.allIssues.isEmpty(), actual.allIssues.toString())
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals(listOf("A", "B"), test.allRules.filterIsInstance<AglStyleTagRule>().map { it.selector.first().value })
    }

    @Test
    fun extends_styleSet_in_same_file__qualified() {
        // a style file has one namespace
        val sentence = """
            namespace test
            styles Base {
              A { foreground: red; }
            }
            styles Test : test.Base {
              B { foreground: blue; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertTrue(actual.allIssues.isEmpty(), actual.allIssues.toString())
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals(listOf("A", "B"), test.allRules.filterIsInstance<AglStyleTagRule>().map { it.selector.first().value })
    }

    @Test
    fun extends_styleSet_chain_in_same_file() {
        val sentence = """
            namespace test
            styles Base1 {
              A { foreground: red; }
            }
            styles Base2 : Base1 {
              S { foreground: green; }
            }
            styles Test : Base2 {
              B { foreground: blue; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertTrue(actual.allIssues.isEmpty(), actual.allIssues.toString())
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals(listOf("A", "S", "B"), test.allRules.filterIsInstance<AglStyleTagRule>().map { it.selector.first().value })
    }

    @Test
    fun extends_styleSet_not_found__error() {
        val sentence = """
            namespace test
            styles Test : Base {
              B { foreground: blue; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertEquals(listOf("StyleSet 'Base' not found"), actual.allIssues.errors.map { it.message })
    }

    @Test
    fun extends_itself__error() {
        val sentence = """
            namespace test
            styles Test : Test {
              B { foreground: blue; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertEquals(listOf("StyleSet 'test.Test' extends itself (via 'Test')"), actual.allIssues.errors.map { it.message })
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals(listOf("B"), test.allRules.filterIsInstance<AglStyleTagRule>().map { it.selector.first().value })
    }

    @Test
    fun extends_cycle__error() {
        val sentence = """
            namespace test
            styles Base : Test {
              A { foreground: red; }
            }
            styles Test : Base {
              B { foreground: blue; }
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertEquals(listOf("StyleSet 'test.Test' extends itself (via 'Base')"), actual.allIssues.errors.map { it.message })
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals(listOf("B"), test.allRules.filterIsInstance<AglStyleTagRule>().map { it.selector.first().value })
    }

    // each name of an 'extends' has its own location
    @Test
    fun extends_not_found__location_of_each_name() {
        val sentence = """
            namespace test
            styles Test : A,
               B {
            }
        """.trimIndent()
        val actual = processWithGrammar(grammarAB, sentence)
        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(29, 15, 2, 1, null), "StyleSet 'A' not found"),
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(35, 4, 3, 1, null), "StyleSet 'B' not found"),
            ),
            actual.allIssues.errors.toList()
        )
    }

    // SD3: a simple name in an 'extends' is also looked up in the imports of the namespace, in order
    @Test
    fun extends_styleSet_via_namespace_import() {
        val sentence = """
            namespace test
              import net.akehurst.language
            styles Test : Base {
              B { foreground: blue; }
            }
        """.trimIndent()
        val context = contextFromGrammar(grammarFor(grammarAB)).union(contextFromRegistryStyles())
        val actual = aglProc.process(sentence, Agl.options { semanticAnalysis { sentenceContext(context) } })
        assertTrue(actual.allIssues.errors.isEmpty(), actual.allIssues.toString())
        val test = actual.asm!!.findDefinitionByQualifiedNameOrNull(QualifiedName("test.Test"))!!
        assertEquals("net.akehurst.language.Base", test.extends[0].resolved?.qualifiedName?.value)
        assertTrue(test.allRules.size > 1, test.allRules.toString())
    }

    @Test
    fun extends_styleSet_without_import__not_found() {
        val sentence = """
            namespace test
            styles Test : Base {
              B { foreground: blue; }
            }
        """.trimIndent()
        val context = contextFromGrammar(grammarFor(grammarAB)).union(contextFromRegistryStyles())
        val actual = aglProc.process(sentence, Agl.options { semanticAnalysis { sentenceContext(context) } })
        assertEquals(listOf("StyleSet 'Base' not found"), actual.allIssues.errors.map { it.message })
    }

    // SD3: the options and imports of a style namespace are kept
    @Test
    fun namespace_options_and_imports_kept() {
        val sentence = """
            namespace test
              #myOption: 'x'
              import net.akehurst.language
            styles Test {
            }
        """.trimIndent()
        val actual = aglProc.process(sentence)
        assertTrue(actual.allIssues.errors.isEmpty(), actual.allIssues.toString())
        val ns = actual.asm!!.namespace[0]
        assertEquals(listOf("net.akehurst.language"), ns.import.map { it.value })
        assertEquals("x", ns.options.get<String>("myOption"))
        assertTrue(actual.asm!!.asString().contains("import net.akehurst.language"), actual.asm!!.asString())
    }
}
