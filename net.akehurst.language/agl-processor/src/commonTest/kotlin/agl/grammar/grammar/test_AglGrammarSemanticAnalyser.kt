/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.grammar.Choice
import net.akehurst.language.api.grammar.ChoiceLongest
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_AglGrammarSemanticAnalyser {

    companion object {
        val aglProc = Agl.registry.agl.grammar.processor!!
    }

    @Test
    fun nonTerminalNotFound() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b ;
            }
        """.trimIndent()
        val result = aglProc.process(grammarStr)
        val expected = setOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(38, 9, 3, 1), "Rule 'b' not found in grammar 'Test'")
        )
        assertEquals(expected, result.issues.all)
    }

    @Test
    fun duplicateRule() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b ;
                b = 'a' ;
                b = 'b' ;
            }
        """.trimIndent()
        val result = aglProc.process(grammarStr)
        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(position = 60, column = 5, line = 5, length = 9),
                "More than one rule named 'b' found in grammar 'Test'"
            ),
//            LanguageIssue(
//                LanguageIssueKind.ERROR,
//                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
//                InputLocation(position = 38, column = 9, line = 3, length = 1),
//                "More than one rule named 'b' found in grammar 'Test'"
//            ),
        )
        assertEquals(expected, result.issues.all)
    }

    @Test
    fun ruleUnused() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = 'b' ;
                c = 'd' ;
            }
        """.trimIndent()
        val result = aglProc.process(grammarStr)
        val expected = setOf(
            LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(48, 5, 4, 9), "Rule 'c' is not used in grammar Test.")
        )
        assertEquals(expected, result.issues.all)
    }

    @Test
    fun ambiguity() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b1 | b2 ;
                b1 = 'b' ;
                b2 = 'b' ;
            }
        """.trimIndent()
        //val proc = Agl.processor(grammarStr)
        val result = aglProc.process(grammarStr, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
            }
        })
        val expected = setOf(
            LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(57, 10, 4, 3), "Ambiguity on [<EOT>] with b2"),
            LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(72, 10, 5, 3), "Ambiguity on [<EOT>] with b1")
        )
        result.issues.forEach {
            println(it)
        }
        assertEquals(expected, result.issues.all)
    }

    @Test
    fun extends_one_reuse_leaf_from_base() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            
            grammar Test extends Base {
              S = A ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val asm = res.asm!!
        assertEquals(2, asm.size)
        val baseG = asm[0]
        val testG = asm[1]

        assertTrue(baseG.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("A") != null)
        assertEquals(1, testG.grammarRule.size)
        assertEquals(2, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringDefault(sentence).processor!!
        assertTrue(proc.parse("a").issues.errors.isEmpty())
    }

    @Test
    fun extends_two_reuse_leaves_from_bases() {
        val sentence = """
            namespace ns.test
            grammar Base1 {
                leaf A = 'a' ;
            }
            grammar Base2 {
                leaf B = 'b' ;
            }
            grammar Test extends Base1, Base2 {
              S = A B ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val asm = res.asm!!
        assertEquals(3, asm.size)
        val base1G = asm[0]
        val base2G = asm[1]
        val testG = asm[2]

        assertTrue(base1G.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(base2G.findAllResolvedNonTerminalRule("B") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("B") != null)
        assertEquals(1, testG.grammarRule.size)
        assertEquals(3, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringDefault(sentence).processor!!
        assertTrue(proc.parse("ab").issues.errors.isEmpty())
    }

    @Test
    fun extends_one_override_leaf_not_override() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            
            grammar Test extends Base {
              S = A ;
              A = 'aa' ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(89, 7, 7, 1),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test', you need to 'override' to resolve"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(95, 3, 8, 10),
                "More than one rule named 'A' found in grammar 'Test'"
            )
        )
        assertEquals(expIssues, res.issues.all)
    }

    @Test
    fun extends_one_override_leaf_from_base() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            
            grammar Test extends Base {
              S = A ;
              override A = 'aa' ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val asm = res.asm!!
        assertEquals(2, asm.size)
        val baseG = asm[0]
        val testG = asm[1]

        assertTrue(baseG.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("A") != null)
        assertEquals(2, testG.grammarRule.size)
        assertEquals(2, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringDefault(sentence).processor!!
        assertTrue(proc.parse("aa").issues.errors.isEmpty())
    }

    @Test
    fun extends_two_override_leaves_from_bases() {
        val sentence = """
            namespace ns.test
            grammar Base1 {
                leaf A = 'a' ;
            }
            grammar Base2 {
                leaf B = 'b' ;
            }
            grammar Test extends Base1, Base2 {
              S = A B ;
              override leaf A = 'aa' ;
              override leaf B = 'bb' ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val asm = res.asm!!
        assertEquals(3, asm.size)
        val base1G = asm[0]
        val base2G = asm[1]
        val testG = asm[2]

        assertTrue(base1G.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(base2G.findAllResolvedNonTerminalRule("B") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("B") != null)
        assertEquals(3, testG.grammarRule.size)
        assertEquals(3, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringDefault(sentence).processor!!
        assertTrue(proc.parse("aabb").issues.errors.isEmpty())
    }

    @Test
    fun extends_two_same_rule_name_and_rhs_in_bases_fails() {
        val sentence = """
            namespace ns.test
            grammar Base1 {
                leaf A = 'a' ;
            }
            grammar Base2 {
                leaf A = 'a' ;
            }
            grammar Test extends Base1, Base2 {
              S = A ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(134, 7, 9, 1),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(75, 5, 6, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(38, 5, 3, 14), "More than one rule named 'A' found in grammar 'Test', you need to 'override' to resolve"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(75, 5, 6, 14), "More than one rule named 'A' found in grammar 'Test', you need to 'override' to resolve"
            )
        )

        assertEquals(expIssues, res.issues.all)
    }

    @Test
    fun extends_diamond() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 extends Base {
                leaf B = 'b' ;
            }
            grammar Mid2 extends Base {
                leaf C = 'c' ;
            }
            grammar Test extends Mid1, Mid2 {
              S = A B C;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val asm = res.asm!!
        assertEquals(4, asm.size)
        val baseG = asm[0]
        val mid1G = asm[1]
        val mid2G = asm[2]
        val testG = asm[3]

        assertTrue(baseG.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(mid1G.findAllResolvedNonTerminalRule("B") != null)
        assertTrue(mid1G.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(mid2G.findAllResolvedNonTerminalRule("C") != null)
        assertTrue(mid1G.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("A") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("B") != null)
        assertTrue(testG.findAllResolvedNonTerminalRule("C") != null)
        assertEquals(1, baseG.allResolvedGrammarRule.size)
        assertEquals(2, mid1G.allResolvedGrammarRule.size)
        assertEquals(2, mid2G.allResolvedGrammarRule.size)
        assertEquals(4, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringDefault(sentence).processor!!
        assertTrue(proc.parse("abc").issues.errors.isEmpty())
    }

    @Test
    fun extends_one_override_appendChoice_to_base() {
        val sentence = """
            namespace ns.test
            grammar Base {
                C = 'a' | 'b' ;
            }
            
            grammar Test extends Base {
              S = C ;
              override C +=| 'c' ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val asm = res.asm!!
        assertEquals(2, asm.size)
        val baseG = asm[0]
        val testG = asm[1]

        assertNotNull(baseG.findAllResolvedNonTerminalRule("C"))
        assertNotNull(testG.findAllResolvedNonTerminalRule("C"))
        assertEquals(2, testG.grammarRule.size)
        assertEquals(2, testG.allResolvedGrammarRule.size)
        assertTrue(baseG.findAllNonTerminalRule("C")[0].rhs is Choice)
        assertEquals(2, (baseG.findAllNonTerminalRule("C")[0].rhs as Choice).alternative.size)
        assertTrue(testG.findAllNonTerminalRule("C")[0].rhs is ChoiceLongest)
        assertEquals(3, (testG.findAllResolvedNonTerminalRule("C")!!.rhs as ChoiceLongest).alternative.size)

        val proc = Agl.processorFromStringDefault(sentence).processor!!
        assertTrue(proc.parse("a").issues.errors.isEmpty())
        assertTrue(proc.parse("b").issues.errors.isEmpty())
        assertTrue(proc.parse("c").issues.errors.isEmpty())
    }

    @Test
    fun extends_diamond_repeate_no_override_fails() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 extends Base {
                leaf B = 'b' ;
            }
            grammar Mid2 extends Base {
                leaf A = 'c' ;
            }
            grammar Test extends Mid1, Mid2 {
              S = A B;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)

        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(135, 5, 9, 14),
                "More than one rule named 'A' found in grammar 'Mid2'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(192, 7, 12, 1),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test', you need to 'override' to resolve"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(135, 5, 9, 14),
                "More than one rule named 'A' found in grammar 'Test', you need to 'override' to resolve"
            )
        )

        assertEquals(expIssues, res.issues.all)
    }

    @Test
    fun extends_diamond_repeate_with_override() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 extends Base {
                leaf B = 'b' ;
            }
            grammar Mid2 extends Base {
                leaf B = 'c' ;
            }
            grammar Test extends Mid1, Mid2 {
              S = A B;
              override B = 'd' ;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)

        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun extends_diamond_repeate_with_override2_fails() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 extends Base {
                leaf B = 'b' ;
            }
            grammar Mid2 extends Base {
                leaf A = 'c' ;
            }
            grammar Test extends Mid1, Mid2 {
              S = B;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)

        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun extends_diamond_repeate_with_override2() {
        val sentence = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 extends Base {
                leaf B = 'b' ;
            }
            grammar Mid2 extends Base {
                override leaf A +=| 'c' ;
            }
            grammar Test extends Mid1, Mid2 {
              S = A B;
            }
        """.trimIndent()
        val res = aglProc.process(sentence)

        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }
}