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

package net.akehurst.language.parser.scanondemand

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.choicePriority.test_ifThenElse_Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull


internal class test_ErrorLocation : test_ScanOnDemandParserAbstract() {

    @Test
    fun parse_success() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val sentence = "a"

        val expected = "S{ 'a' }"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )

    }

    @Test
    fun emptyInput_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"^", setOf("'a'"))
        ),issues)
    }

    @Test
    fun concatenation_start_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val goal = "S"
        val sentence = "b"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'"), ex.expected)
    }

    @Test
    fun concatenation_afterFirstLiteral_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
        val sentence = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
        assertEquals(setOf("'b'"), e.expected)
    }

    @Test
    fun concatenation_afterSecondLiteral_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
        val sentence = "abc"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(3, e.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT_TAG), e.expected)
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
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(9, e.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT_TAG), e.expected)
    }

    @Test
    fun concatenation_afterEOL_WS_fail() {
        val rrs = runtimeRuleSet {
            skip("WS") { pattern("\\s+") }
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
        val sentence = """
            a
            b
            c
        """.trimIndent()

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        println(e)
        assertEquals(3, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT_TAG), e.expected)
    }

    @Test
    fun multi1n_empty_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 1, -1, "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun multi2n_empty_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 2, -1, "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun multi2n_a_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 2, -1, "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
        val sentence = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun multi25_a6_fail() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("as") }
            multi("as", 2, 5, "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
        val sentence = "aaaaaa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        println(e)
        assertEquals(1, e.location.line)
        assertEquals(6, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }
}
