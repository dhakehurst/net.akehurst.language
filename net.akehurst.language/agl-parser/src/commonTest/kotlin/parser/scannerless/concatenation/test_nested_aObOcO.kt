/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_nested_aObOcO : test_LeftCornerParserAbstract() {
    /*
        S = Opts t
        Opts = a? b? c?
     */
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("Opts"); literal("t") }
            concatenation("Opts") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
            optional("aOpt", "'a'")
            optional("bOpt", "'b'")
            optional("cOpt", "'c'")
            literal("'a'", "a")
            literal("'b'", "b")
            literal("'c'", "c")
        }
        const val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), sentence, setOf("<GOAL>"), setOf("'a'", "'b'", "'c'", "'t'"))
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
                parseError(InputLocation(0, 1, 1, 1), sentence, setOf("<GOAL>"), setOf("'a'", "'b'", "'c'", "'t'"))
            ), issues.errors
        )
    }

    @Test
    fun t() {
        val sentence = "t"

        val expected = """
            S {
              Opts {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt|1 { §empty }
              }
              't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun at() {
        val sentence = "at"

        val expected = """
            S { Opts {
                aOpt { 'a' }
                bOpt|1 { §empty }
                cOpt|1 { §empty }
                }
                't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun abt() {
        val sentence = "abt"

        val expected = """
            S { Opts {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt|1 { §empty }
                }
                't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ba_fails() {
        val sentence = "ba"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), sentence, setOf("Opts"), setOf("'c'", "'t'"))
            ), issues.errors
        )
    }

    @Test
    fun act() {
        val sentence = "act"

        val expected = """
            S { Opts {
                aOpt { 'a' }
                bOpt|1 { §empty }
                cOpt { 'c' }
                }
                't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun abct() {
        val sentence = "abct"

        val expected = """
            S { Opts {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt { 'c' }
                }
                't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun adc_fails() {
        val sentence = "adc"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), sentence, setOf("<GOAL>"), setOf("'b'", "'c'", "'t'"))
            ), issues.errors
        )
    }

    @Test
    fun abd_fails() {
        val sentence = "abd"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1), sentence, setOf("Opts"), setOf("'c'", "'t'"))
            ), issues.errors
        )
    }

    @Test
    fun abcd_fails() {
        val sentence = "abcd"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(3, 4, 1, 1), sentence, setOf("Opts"), setOf("'t'"))
            ), issues.errors
        )
    }

    @Test
    fun bt() {
        val sentence = "bt"

        val expected = """
            S { Opts {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt|1 { §empty }
                }
                't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun bct() {
        val sentence = "bct"

        val expected = """
            S { Opts {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt { 'c' }
                }
                't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ct() {
        val goal = "S"
        val sentence = "ct"

        val expected = """
            S { Opts {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt { 'c' }
               }
               't'
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun cb_fails() {
        val goal = "S"
        val sentence = "cb"

        val (sppt, issues) = super.testFail(rrs, Companion.goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), sentence, setOf("Opts"), setOf("'t'"))
            ), issues.errors
        )
    }
}