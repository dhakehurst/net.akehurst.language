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
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_aObOcO : test_ScanOnDemandParserAbstract() {
    /*
        S = a? b? c?;
     */
    companion object {

        private val rrs = runtimeRuleSet {
            concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
            multi("aOpt", 0, 1, "'a'")
            multi("bOpt", 0, 1, "'b'")
            multi("cOpt", 0, 1, "'c'")
            literal("'a'", "a")
            literal("'b'", "b")
            literal("'c'", "c")
        }
    }

    @Test
    fun empty() {
        val goal = "S"
        val sentence = ""

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt|1 { §empty }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun d_fails() {
        val goal = "S"
        val sentence = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt|1 { §empty }
                cOpt|1 { §empty }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun ab() {
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt|1 { §empty }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun ba_fails() {
        val goal = "S"
        val sentence = "ba"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }

    @Test
    fun ac() {
        val goal = "S"
        val sentence = "ac"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt|1 { §empty }
                cOpt { 'c' }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun abc() {
        val goal = "S"
        val sentence = "abc"

        val expected = """
            S {
                aOpt { 'a' }
                bOpt { 'b' }
                cOpt { 'c' }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun adc_fails() {
        val goal = "S"
        val sentence = "adc"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }

    @Test
    fun abd_fails() {
        val goal = "S"
        val sentence = "abd"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(3, ex.location.column)
    }

    @Test
    fun abcd_fails() {
        val goal = "S"
        val sentence = "abcd"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(4, ex.location.column)
    }

    @Test
    fun b() {
        val goal = "S"
        val sentence = "b"

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt|1 { §empty }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun bc() {
        val goal = "S"
        val sentence = "bc"

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt { 'b' }
                cOpt { 'c' }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun c() {
        val goal = "S"
        val sentence = "c"

        val expected = """
            S {
                aOpt|1 { §empty }
                bOpt|1 { §empty }
                cOpt { 'c' }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)

        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun cb_fails() {
        val goal = "S"
        val sentence = "cb"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }
}