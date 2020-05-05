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

package net.akehurst.language.parser.scannerless.examples

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_AhoSetiUlman_Ex_4_7_5 : test_ScannerlessParserAbstract() {

    // This grammar is LR(1) but not LALR(1)

    // S = A a | b A c | B c | b B a ;
    // A = d ;
    // B = d ;
    //
    // S = S1 | S2 | S3 | S4
    // S1 = A a ;
    // S2 = b A c ;
    // S3 = B c ;
    // S4 = b B a ;
    // A = d ;
    // B = d ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_b = b.literal("b")
        val r_c = b.literal("c")
        val r_d = b.literal("d")
        val r_A = b.rule("A").concatenation(r_d)
        val r_B = b.rule("B").concatenation(r_d)
        val r_S1 = b.rule("S1").concatenation(r_A, r_a)
        val r_S2 = b.rule("S2").concatenation(r_b, r_A, r_c)
        val r_S3 = b.rule("S3").concatenation(r_B, r_c)
        val r_S4 = b.rule("S4").concatenation(r_b, r_B, r_a)
        val r_S = b.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r_S1, r_S2, r_S3, r_S4)
        return b
    }

    @Test //TODO: remove this its temporary
    fun printAutomaton() {
        val rrb = this.S()
        val goal = "S"

        println(rrb.ruleSet().printFullAutomaton(goal))
    }

    @Test
    fun a_fails() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(1, ex.location.column, "column is wrong")
        assertEquals(setOf("'b'", "'d'"), ex.expected)
    }

    @Test
    fun d_fails() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(2, ex.location.column, "column is wrong")
        assertEquals(setOf("'c'", "'a'"), ex.expected)
    }

    @Test
    fun da() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "da"

        val expected = """
            S { S1 { A { 'd' } 'a' } }
        """.trimIndent()

        val tree = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, tree.maxNumHeads)
    }

    @Test
    fun bdc() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "bdc"

        val expected = """
            S { S2 { 'b' A { 'd' } 'c' } }
        """.trimIndent()

        val tree = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(2, tree.maxNumHeads)

    }

    @Test
    fun dc() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "dc"

        val expected = """
            S { S3 { B { 'd' } 'c' } }
        """.trimIndent()

        val tree = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, tree.maxNumHeads)
    }

    @Test
    fun bda() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "bda"

        val expected = """
            S { S4 { 'b' B { 'd' } 'a' } }
        """.trimIndent()

        val tree = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(2, tree.maxNumHeads)
    }


}