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

package net.akehurst.language.parser.scanondemand.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_AB_ESB : test_LeftCornerParserAbstract() {

    //  S = E S B | A B
    //  A = a | a A
    //  B = b | E
    //  E = e | <e>
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("E"); ref("S"); ref("B") }
                concatenation { ref("A"); ref("B") }
            }
            choice("A", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                concatenation { literal("a"); ref("A") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("b"); }
                concatenation { ref("E") }
            }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("e"); }
                concatenation { empty() }
            }
        }
        val goal = "S"
    }

    @Test
    fun c_fails() {
        val sentence = "c"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^c", setOf("'a'", "'e'"))
            ), issues.errors
        )
    }

    @Test
    fun d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^d", setOf("'a'", "'e'"))
            ), issues.errors
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
              A { 'a' }
              B { E { <EMPTY> } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)

    }

    @Test
    fun eab() {
        // fail("when converting to String get java.lang.OutOfMemoryError: Java heap space: failed reallocation of scalar replaced objects")
        val sentence = "eab"

        val expected = """
            S {
              E { 'e' }
              S { 
                A { 'a' }
                B { E { <EMPTY> } }
              }
              B { 'b' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}