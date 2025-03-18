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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_embedded2 : test_LeftCornerParserAbstract() {

    private companion object {

        val Inner = runtimeRuleSet("test.Inner") {
            pattern("WS", "\\s+", true)
            pattern("COMMENT", "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", true)
            concatenation("TR") { ref("optST"); ref("optRE"); ref("optTP") }
            optional("optST", "ST")
            concatenation("ST") { ref("ID") }
            optional("optRE", "RE")
            concatenation("RE") { literal("/"); ref("EX") }
            optional("optTP", "TP")
            concatenation("TP") { literal("#"); ref("ID") }

            choice("EX", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ASS")
                ref("ID")
            }
            concatenation("ASS") { ref("ID"); literal("="); ref("ID") }

            pattern("ID", "[a-zA-Z]+")
        }

        // S = 's' '{' I '}' ;
        // I = Inner::TR ;
        val S = runtimeRuleSet("test.S") {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("s"); literal("{"); ref("I"); literal("}"); }
            embedded("I", Inner, "TR")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'s'"))
            ), issues.errors
        )
    }

    @Test
    fun d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(S, Companion.goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'s'"))
            ), issues.errors
        )
    }

    @Test
    fun s_fails() {
        val sentence = "s"

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'{'"))
            ), issues.errors
        )
    }

    @Test
    fun so_fails() {
        val sentence = "s{"

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1, null), sentence, setOf("S"), setOf("ID", "'/'", "'#'", "'}'"))
            ), issues.errors
        )
    }

    @Test
    fun soc() {
        val goal = "S"
        val sentence = "s{}"

        val expected = """
            S {
              's' '{'
              I : Inner::TR {
                optST{ §empty }
                optRE{ §empty }
                optTP{ §empty }
              }
              '}'
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
    fun so_c() {
        val goal = "S"
        val sentence = "s{ }"

        val expected = """
            S {
              's' '{' WS : ' '
              I : Inner::TR {
                optST{ §empty }
                optRE{ §empty }
                optTP{ §empty }
              }
              '}'
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
    fun soac() {
        val goal = "S"
        val sentence = "s{a}"

        val expected = """
            S {
              's' '{'
              I : Inner::TR {
                optST{ ST { ID : 'a' } }
                optRE{ §empty }
                optTP{ §empty }
              }
              '}'
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
    fun soMc() {
        val goal = "S"
        val sentence = "s{/*xx*/}"

        val expected = """
            S {
              's' '{'
              I : Inner::TR {
                COMMENT:'/*xx*/'
                optST{ §empty }
                optRE{ §empty }
                optTP{ §empty }
              }
              '}'
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