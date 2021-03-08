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

package net.akehurst.language.parser.scanondemand.examples

import net.akehurst.language.agl.automaton.AutomatonKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_AhoSetiUlman_Ex_4_58 : test_ScanOnDemandParserAbstract() {

    // This grammar is LR(1) but not LALR(1)

    // S = a A d | b B d | a B e | b A e
    // A = c
    // B = c
    //
    // S = S1 | S2 | S3 | S4
    // S1 = a A d
    // S2 = b B d
    // S3 = a B e
    // S4 = b A e
    // A = c
    // B = c
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                ref("S2")
                ref("S3")
                ref("S4")
            }
            concatenation("S1") { literal("a") ; ref("A"); literal("d") }
            concatenation("S2") { literal("b"); ref("B"); literal("d") }
            concatenation("S3") { literal("a"); ref("B"); literal("e")}
            concatenation("S4") { literal("b"); ref("A"); literal("e") }
            concatenation("A") { literal("c") }
            concatenation("B") { literal("c") }
        }
    }

    @BeforeTest
    fun before() {
        rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
    }

    @Test
    fun a_fails() {
        val goal = "S"
        val sentence = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(1, ex.location.column, "column is wrong")
        assertEquals(setOf("'b'", "'d'"), ex.expected)
    }

    @Test
    fun d_fails() {
        val goal = "S"
        val sentence = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(2, ex.location.column, "column is wrong")
        assertEquals(setOf("'c'", "'a'"), ex.expected)
    }

    @Test
    fun da() {
        val goal = "S"
        val sentence = "da"

        val expected = """
            S { S1 { A { 'd' } 'a' } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun bdc() {
        val goal = "S"
        val sentence = "bdc"

        val expected = """
            S|1 { S2 { 'b' A { 'd' } 'c' } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun dc() {
        val goal = "S"
        val sentence = "dc"

        val expected = """
            S|2 { S3 { B { 'd' } 'c' } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun bda() {
        val goal = "S"
        val sentence = "bda"

        val expected = """
            S|3 { S4 { 'b' B { 'd' } 'a' } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

}