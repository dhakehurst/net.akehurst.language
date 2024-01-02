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

package net.akehurst.language.parser.scanondemand.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_aObOcO : test_LeftCornerParserAbstract() {
    /*
        S = a? b? c?;
     */
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
            optional("aOpt", "'a'")
            optional("bOpt", "'b'")
            optional("cOpt", "'c'")
            literal("'a'", "a")
            literal("'b'", "b")
            literal("'c'", "c")
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt|1 { §empty }
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
    fun d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^d", setOf("'a'", "'b'", "'c'", "<EOT>"))
            ), issues.errors
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt|1 { §empty }
                cOpt|1 { §empty }
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
    fun ab() {
        val sentence = "ab"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt|1 { §empty }
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
    fun ba_fails() {
        val sentence = "ba"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "b^a", setOf("'c'", "<EOT>"))
            ), issues.errors
        )
    }

    @Test
    fun ac() {
        val sentence = "ac"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt|1 { §empty }
                cOpt { 'c' }
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
    fun abc() {
        val sentence = "abc"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt { 'c' }
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
    fun adc_fails() {
        val sentence = "adc"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "a^dc", setOf("'b'", "'c'", "<EOT>"))
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
                parseError(InputLocation(2, 3, 1, 1), "ab^d", setOf("'c'", "<EOT>"))
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
                parseError(InputLocation(3, 4, 1, 1), "abc^d", setOf("<EOT>"))
            ), issues.errors
        )
    }

    @Test
    fun b() {
        val sentence = "b"

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt|1 { §empty }
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
    fun bc() {
        val sentence = "bc"

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt { 'c' }
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
    fun c() {
        val sentence = "c"

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt { 'c' }
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
    fun cb_fails() {
        val sentence = "cb"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "c^b", setOf("<EOT>"))
            ), issues.errors
        )
    }
}