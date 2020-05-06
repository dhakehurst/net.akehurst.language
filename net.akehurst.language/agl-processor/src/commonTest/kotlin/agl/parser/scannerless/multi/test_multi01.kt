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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_multi01 : test_ScannerlessParserAbstract() {

    // S = 'a'?
    private fun multi01_a(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r0 = b.literal("a")
        val r1 = b.rule("S").multi(0, 1, r0)
        return b
    }

    @Test
    fun empty() {
        val rrb = multi01_a()
        val goal = "S"
        val sentence = ""

        val expected = """
            S|1 { §empty }
        """.trimIndent()

        val actual = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a() {
        val rrb = multi01_a()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }


    @Test
    fun aa_fails() {
        val rrb = multi01_a()
        val goal = "S"
        val sentence = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.testStringResult(rrb, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT.tag), e.expected)
    }

}