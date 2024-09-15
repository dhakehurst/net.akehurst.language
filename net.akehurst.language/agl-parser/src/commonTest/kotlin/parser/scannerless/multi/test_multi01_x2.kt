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

package net.akehurst.language.parser.leftcorner.multi

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_multi01_x2 : test_LeftCornerParserAbstract() {

    // S = A B V 'd'
    // A = 'a'?
    // B = 'b'?
    // V = "[a-c]"
    private companion object {
        private val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("V"); literal("d") }
            optional("A", "'a'")
            literal("'a'", "a")
            optional("B", "'b'")
            literal("'b'", "b")
            pattern("V", "[a-c]")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'a'", "'b'", "V"))
            ), issues.errors
        )
    }

    @Test
    fun abcd() {
        val sentence = "abcd"

        val expected = """
             S {
              A { 'a' }
              B { 'b' }
              V:'c'
              'd'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acd() {
        val sentence = "acd"

        val expected = """
             S {
              A { 'a' }
              B|1 { §empty }
              V:'c'
              'd'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun bcd() {
        val sentence = "bcd"

        val expected = """
             S {
              A|1 { §empty }
              B { 'b' }
              V:'c'
              'd'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun cd() {
        val sentence = "cd"

        val expected = """
             S {
              A|1 { §empty }
              B|1 { §empty }
              V:'c'
              'd'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ad() {
        val sentence = "ad"

        val expected = """
             S {
              A|1 { §empty }
              B|1 { §empty }
              V:'a'
              'd'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }
}