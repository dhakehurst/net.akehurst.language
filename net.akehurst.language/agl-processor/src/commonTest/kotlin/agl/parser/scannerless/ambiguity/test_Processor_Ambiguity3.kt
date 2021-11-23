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

package net.akehurst.language.parser.scanondemand.ambiguity

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_Processor_Ambiguity3 : test_ScanOnDemandParserAbstract() {
    /**
     * From [https://pdfs.semanticscholar.org/eeac/392e02671b0edcd81ae080a5117e5f9584f5.pdf]
     * Generalised Parsing: Some Costs. Adrian Johnstone, Elizabeth Scott, and Giorgios Economopoulos
     * S = A b | A' c ;
     * A' = A' a | a ;
     * A  = a A | A a | a ;
     */
    /**
     * S = S1 | S2 ;
     * S1 = P 'b' ;
     * S2 = Q 'c' ;
     * Q = Q1 | 'a' ;
     * Q1 = Q 'a'
     * P  = P1 | P2 | 'a' ;
     * P1 = a P ;
     * P2 = P a ;
     */
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                ref("S2")
            }
            concatenation("S1") { ref("P"); literal("b") }
            concatenation("S2") { ref("Q"); literal("c") }
            choice("Q",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("Q1")
                literal("a")
            }
            concatenation("Q1") { ref("Q"); literal("a") }
            choice("P",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("P1")
                ref("P2")
                literal("a")
            }
            concatenation("P1") { literal("a"); ref("P") }
            concatenation("P2") { ref("P"); literal("a") }
        }
        val goal = "S"
    }
    /*
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_b = b.literal("b")
        val r_c = b.literal("c")
        val r_P = b.rule("P").build()
        val r_P1 = b.rule("P1").concatenation(r_a, r_P)
        val r_P2 = b.rule("P2").concatenation(r_P, r_a)
        b.rule(r_P).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_P1, r_P2, r_a)
        val r_Q = b.rule("Q").build()
        val r_Q1 = b.rule("Q1").concatenation(r_Q, r_a)
        b.rule(r_Q).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_Q1, r_a)
        val r_S2 = b.rule("S2").concatenation(r_Q, r_c)
        val r_S1 = b.rule("S1").concatenation(r_P, r_b)
        b.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_S1, r_S2)
        return b
    }

     */

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
            ), issues
        )
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1,2,1,1),"a^",setOf("'a'","'b'","'c'"))
            ), issues
        )
    }

    @Test
    fun ab() {
        val sentence = "ab"

        val expected = """
            S { S1 {
                P|2 { 'a' }
                'b'
            } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ac() {
        val sentence = "ac"

        val expected = """
            S|1 { S2 {
                Q|1 { 'a' }
                'c'
            } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aab() {
        val sentence = "aab"

        val expected = """
             S { S1 {
                P|1 { P2 {
                    P|2 { 'a' }
                    'a'
                  } }
                'b'
              } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aaab() {
        val sentence = "aaab"

        val expected = """
             S { S1 {
                P|1 { P2 {
                    P|1 { P2 {
                        P|2 { 'a' }
                        'a'
                      } }
                    'a'
                  } }
                'b'
              } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a10b() {
        val sentence = "a".repeat(10) + "b"

        val expected = """
         S { S1 {
            P|1 { P2 {
                P|1 { P2 {
                    P|1 { P2 {
                        P|1 { P2 {
                            P|1 { P2 {
                                P|1 { P2 {
                                    P|1 { P2 {
                                        P|1 { P2 {
                                            P|1 { P2 {
                                                P|2 { 'a' }
                                                'a'
                                              } }
                                            'a'
                                          } }
                                        'a'
                                      } }
                                    'a'
                                  } }
                                'a'
                              } }
                            'a'
                          } }
                        'a'
                      } }
                    'a'
                  } }
                'a'
              } }
            'b'
          } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a50b() {
        val sentence = "a".repeat(50) + "b"

        val expected = "S { S1 {" + "P|1 { P2 {".repeat(49) + "P|2 {'a'}" + "'a' } }".repeat(49) + "'b'} }"

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }
}