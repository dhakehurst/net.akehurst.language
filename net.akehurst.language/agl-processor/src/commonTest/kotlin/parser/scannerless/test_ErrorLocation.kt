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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class test_ErrorLocation : test_ScannerlessParserAbstract() {


    @Test
    fun parse_success() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val sentence = "a"

        val expected = "S{ 'a' }"

        super.test(rrs, goal, sentence, expected)

    }

    @Test
    fun emptyInput_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)

    }

    @Test
    fun concatenation_start_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val sentence = "b"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)

    }

    @Test
    fun concatenation_afterFirstLiteral_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
        val sentence = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)

    }

    @Test
    fun concatenation_afterSecondLiteral_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
        val sentence = "abc"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(3, e.location.column)

    }


    @Test
    fun concatenation_afterSecondLiteral_WS_fail() {
        val rrs = runtimeRuleSet {
            skip("WS") { pattern("\\s+") }
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
        val sentence = "a   b   c"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(9, e.location.column)

    }


    @Test
    fun multi1n_empty_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 1, -1) { literal("a") }
        }
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)

    }

    @Test
    fun multi2n_empty_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 2, -1) { literal("a") }
        }
        val goal = "S"
        val sentence = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)

    }

    @Test
    fun multi25_empty_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 2, 5) { literal("a") }
        }
        val goal = "S"
        val sentence = "aaaaaa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(6, e.location.column)

    }
}
