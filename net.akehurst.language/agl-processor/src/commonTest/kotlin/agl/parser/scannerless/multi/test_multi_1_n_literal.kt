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

package net.akehurst.language.parser.scanondemand.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_multi_1_n_literal : test_ScanOnDemandParserAbstract() {

    // S = 'a'+
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r0 = b.literal("a")
        val r1 = b.rule("S").multi(1, -1, r0)
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
    fun a() {
        val rrb = S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        val actual = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun aa() {
        val rrb = S()
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S { 'a' 'a' }
        """.trimIndent()

        val actual = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
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
    fun aaaa() {
        val rrb = S()
        val goal = "S"
        val sentence = "aaaa"

        val expected = """
            S { 'a' 'a' 'a' 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a50() {
        val rrb = S()
        val goal = "S"
        val sentence = "a".repeat(50)

        val expected = "S { "+"'a' ".repeat(50)+" }"

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a500() {
        val rrb = S()
        val goal = "S"
        val sentence = "a".repeat(500)

        val expected = "S { "+"'a' ".repeat(500)+" }"

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a2000() {
        val rrb = S()
        val goal = "S"
        val sentence = "a".repeat(2000)

        val expected = "S { "+"'a' ".repeat(2000)+" }"

        super.testStringResult(rrb, goal, sentence, expected)
    }
}