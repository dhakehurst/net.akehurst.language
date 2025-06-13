/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner.embedded

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_embeddedSupersetSkip : test_LeftCornerParserAbstract() {

    private companion object {
        // two grammars, B embedded in S
        // Bs = B+ ;
        // B = b ;
        val Inner = runtimeRuleSet("test.Inner") {
            pattern("WSi", "\\s+", true)
            concatenation("COMMENT", true) { pattern("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/") }
            multi("Bs", 1, -1, "B")
            concatenation("B") { literal("b") }
        }

        // S = a gB c ;
        // gB = Inner::B ;
        val S = runtimeRuleSet("test.S") {
            pattern("WSo", "\\s+", true)
            concatenation("S") { literal("a"); ref("gB"); literal("c"); }
            embedded("gB", Inner, "Bs")
        }
        val goal = "S"
    }

    @Test
    fun S_empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun S_d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(S, Companion.goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun S_a_fails() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'b'"))
            ), issues.errors
        )
    }

    @Test
    fun S_ab_fails() {
        val sentence = "ab"

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1, null), sentence, setOf("S"), setOf("'b'", "'c'"))
            ), issues.errors
        )
    }

    @Test
    fun abc() {
        val goal = "S"
        val sentence = "abc"

        val expected = """
            S {
              'a'
              gB : Inner::Bs { B{ 'b' } }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun abbc() {
        val goal = "S"
        val sentence = "abbc"

        val expected = """
            S {
              'a'
              gB : Inner::Bs { B{ 'b' } B { 'b' } }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ab_bc() {
        val goal = "S"
        val sentence = "ab bc"

        val expected = """
            S {
              'a'
              gB : Inner::Bs { B{ 'b' WSi:' ' } B { 'b' } }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_b_bc() {
        val goal = "S"
        val sentence = " a b bc"

        val expected = """
            S {
              WSo : ' '
              'a'
              WSo : ' '
              gB : Inner::Bs { B{ 'b' WSi:' ' } B { 'b' } }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_b_b_c() {
        val goal = "S"
        val sentence = " a b b c"

        val expected = """
            S {
              WSo : ' '
              'a'
              WSo : ' '
              gB : Inner::Bs { B{ 'b' WSi:' ' } B { 'b' WSi:' ' } }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aCb_b_c() {
        val goal = "S"
        val sentence = " a/*c*/b b c"

        val expected = """
            S {
              WSo : ' '
              'a'
              gB : Inner::Bs {
                     COMMENT { "/\*[^*]*\*+(?:[^/*][^*]*\*+)*/" : '/*c*/' }
                     B{ 'b' WSi:' ' } B { 'b' WSi:' ' }
                   }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_bCb_c() {
        val goal = "S"
        val sentence = " a b/*c*/b c"

        val expected = """
            S {
              WSo : ' '
              'a'
              WSo : ' '
              gB : Inner::Bs {
                     B {
                       'b'
                       COMMENT { "/\*[^*]*\*+(?:[^/*][^*]*\*+)*/" : '/*c*/' }
                     }
                     B { 'b' WSi : ' ' }
                   }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_b_bCc() {
        val goal = "S"
        val sentence = " a b b/*c*/c"

        val expected = """
            S {
              WSo : ' '
              'a'
              WSo : ' '
              gB : Inner::Bs {
                B{ 'b' WSi:' ' }
                B { 'b' COMMENT { "/\*[^*]*\*+(?:[^/*][^*]*\*+)*/" : '/*c*/' } }
              }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

}