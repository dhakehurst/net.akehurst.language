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

package net.akehurst.language.parser.scannerless.ambiguity

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity3 : test_ScannerlessParserAbstract() {
    /**
     * From [https://pdfs.semanticscholar.org/eeac/392e02671b0edcd81ae080a5117e5f9584f5.pdf]
     * Generalised Parsing: Some Costs. Adrian Johnstone, Elizabeth Scott, and Giorgios Economopoulos
     * S = A b | A' c ;
     * A' = A' a | a ;
     * A  = a A | A a | a ;
     */
    /**
     * S = S1 | S2 ;
     * S1 = P 'b' ;
     * S2 = Q 'c' ;
     * Q = Q1 | 'a' ;
     * Q1 = Q 'a'
     * P  = P1 | P2 | 'a' ;
     * P1 = a P ;
     * P2 = P a ;
     */
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_b = b.literal("b")
        val r_c = b.literal("c")
        val r_P = b.rule("P").build()
        val r_P1 = b.rule("P1").concatenation(r_a, r_P)
        val r_P2 = b.rule("P2").concatenation(r_P, r_a)
         b.rule(r_P).choiceEqual(r_P1, r_P2, r_a)
        val r_Q = b.rule("Q").build()
        val r_Q1 = b.rule("Q1").concatenation(r_Q, r_a)
         b.rule(r_Q).choiceEqual(r_Q1, r_a)
        val r_S2 = b.rule("S2").concatenation(r_Q, r_c)
        val r_S1 = b.rule("S1").concatenation(r_P, r_b)
        b.rule("S").choiceEqual(r_S1, r_S2)
        return b
    }

    @Test
    fun empty() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S { 'a' }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun ab() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "ab"

        val expected1 = """
            S { S1 {
                P { 'a' }
                'b'
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }

    @Test
    fun ac() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "ac"

        val expected1 = """
            S { S2 {
                Q { 'a' }
                'c'
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }

    @Test
    fun a10b() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(10) +"b"

        val expected1 = """
            S { S1 {
                P { 'a' }
                'b'
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }

    //@Test takes too long at present
    fun a50b() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(50) +"b"

        val expected1 = """
            S { S1 {
                P { 'a' }
                'b'
            } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected1)
    }
}