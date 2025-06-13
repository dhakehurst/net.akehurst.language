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

package net.akehurst.language.parser.leftcorner.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_ab_cOa_bc : test_LeftCornerParserAbstract() {

    // S = C
    // C = ab_c > a_bc;
    // ab_c = ab 'c'
    // a_bc = 'a' bc
    // ab = 'a' 'b'
    // bc = 'b' 'c'
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C") }
            choice("C", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("ab_c")
                ref("a_bc")
            }
            concatenation("ab_c") { ref("ab"); literal("c") }
            concatenation("a_bc") { literal("a"); ref("bc") }
            concatenation("ab") { literal("a"); literal("b") }
            concatenation("bc") { literal("b"); literal("c") }
            preferenceFor("'a'") {
                left(listOf("ab"), setOf("'b'"))
                left(listOf("a_bc"), setOf("'b'"))
            }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {

        val sentence = ""

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"), setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1, 2, 1, 1, null),sentence, setOf("<GOAL>"), setOf("'b'"))
        ),issues.errors)
    }

    @Test
    fun b_fails() {
        val sentence = "b"

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"), setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun c_fails() {
        val sentence = "c"

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"), setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun ab_fails() {
        val sentence = "ab"

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2, 3, 1, 1, null),sentence, setOf("a_bc"), setOf("'c'"))
        ),issues.errors)
    }

    @Test
    fun abc() {
        val sentence = "abc"

        val expected = """
         S { C { a_bc {
            'a'
            bc { 'b' 'c' }
          } } }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

}