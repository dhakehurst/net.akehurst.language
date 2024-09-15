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

package net.akehurst.language.parser.leftcorner.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_AhoSetiUlman_Ex_4_7_4 : test_LeftCornerParserAbstract() {

    // This grammar is LALR(1) but not SLR(1)

    // S = A a | b A c | d c | b d a ;
    // A = d ;
    //
    // S = S1 | S2 | S3 | S4
    // S1 = A a ;
    // S2 = b A c ;
    // S3 = d c ;
    // S4 = b d a ;
    // A = d ;
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                ref("S2")
                ref("S3")
                ref("S4")
            }
            concatenation("S1") { ref("A"); literal("a") }
            concatenation("S2") { literal("b"); ref("A"); literal("c") }
            concatenation("S3") { literal("d"); literal("c") }
            concatenation("S4") { literal("b"); literal("d"); literal("a") }
            concatenation("A") { literal("d") }
        }
        val goal = "S"
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt,issues) = super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^a",setOf("'d'","'b'"))
        ),issues.errors)
    }

    @Test
    fun d_fails() {
        val sentence = "d"

        val (sppt,issues) = super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"d^",setOf("'a'","'c'"))
        ),issues.errors)
    }

    @Test
    fun da() {
        val sentence = "da"

        val expected = """
            S { S1 { A { 'd' } 'a' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun bdc() {
        val sentence = "bdc"

        val expected = """
            S|1 { S2 { 'b' A { 'd' } 'c' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun dc() {
        val sentence = "dc"

        val expected = """
            S|2 { S3 { 'd' 'c' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun bda() {
        val sentence = "bda"

        val expected = """
            S|3 { S4 { 'b' 'd' 'a' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}