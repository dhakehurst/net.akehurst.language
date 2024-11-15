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

package net.akehurst.language.parser.leftcorner.embedded

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_embedded1 : test_LeftCornerParserAbstract() {

    private companion object {
        // one grammar
        //S = a B c
        //B = b
        val Sn = runtimeRuleSet("test.Sn") {
            concatenation("S") { literal("a"); ref("B"); literal("c"); }
            concatenation("B") { literal("b") }
        }

        // two grammars, B embedded in S
        // B = b ;
        val B = runtimeRuleSet("test.B") {
            concatenation("B") { literal("b") }
        }

        // S = a gB c ;
        // gB = B::B ;
        val S = runtimeRuleSet("test.S") {
            concatenation("S") { literal("a"); ref("gB"); literal("c"); }
            embedded("gB", B, "B")
        }
        val goal = "S"
    }

    @Test
    fun Sn_empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(Sn, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), sentence, setOf("<GOAL>"), setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun Sn_a_fails() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(Sn, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), sentence, setOf("<GOAL>"), setOf("'b'"))
            ), issues.errors
        )
    }

    @Test
    fun Sn_abc() {
        val sentence = "abc"

        val expected = """
            S {
              'a'
              B { 'b' }
              'c'
            }
        """.trimIndent()

        super.test_pass(
            rrs = Sn,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun S_empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), sentence, setOf("<GOAL>"), setOf("'a'"))
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
                parseError(InputLocation(0, 1, 1, 1), sentence, setOf("<GOAL>"), setOf("'a'"))
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
                parseError(InputLocation(1, 2, 1, 1), sentence, setOf("<GOAL>"), setOf("'b'"))
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
                parseError(InputLocation(2, 3, 1, 1), sentence, setOf("S"), setOf("'c'"))
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
              gB : B::B { 'b' }
              'c'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "test.B" to B
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }
}