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

package net.akehurst.language.parser.scannerless.multi

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.ScannerlessParser
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import net.akehurst.language.parser.sppt.SPPTParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_multi_3_5_literal : test_ScannerlessParserAbstract() {

    // S = 'a'3..5
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r0 = b.literal("a")
        val r1 = b.rule("S").multi(3, 5, r0)
        return b
    }

    @Test
    fun empty_fails() {
        val rrb = S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun a_fails() {
        val rrb = S()
        val goal = "S"
        val sentence = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun aa_fails() {
        val rrb = S()
        val goal = "S"
        val sentence = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(3, e.location.column)
    }

    @Test
    fun aaa() {
        val rrb = S()
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S { 'a' 'a' 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a4() {
        val rrb = S()
        val goal = "S"
        val sentence = "a".repeat(4)

        val expected = "S { "+"'a' ".repeat(4)+" }"

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a5() {
        val rrb = S()
        val goal = "S"
        val sentence = "a".repeat(5)

        val expected = "S { "+"'a' ".repeat(5)+" }"

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a6_fails() {
        val rrb = S()
        val goal = "S"
        val sentence = "a".repeat(6)

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(6, e.location.column)
    }
}