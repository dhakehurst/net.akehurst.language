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

package net.akehurst.language.parser.scanondemand.leftRecursive

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class  test_a : test_ScanOnDemandParserAbstract() {

    companion object {
        // S =  'a' | S1 ;
        // S1 = S 'a' ;
        val S = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
    }
    @Test
    fun a() {
        val rrs = S
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }


    @Test
    fun aa() {
        val rrs = S
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S|1 { S1 { S { 'a' } 'a' } }
        """.trimIndent()

        println(rrs.printFullAutomaton("S", true))

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun aaa() {
        val rrs = S
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S|1 {
                S1 {
                    S|1 {
                        S1 {
                            S { 'a' }
                            'a'
                        }
                    }
                    'a'
                }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a50() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(50)

        val expected = "S|1 { S1 { ".repeat(49) + "S { 'a' }" + "'a' } }".repeat(49)

        val actual = super.test(rrs, goal, sentence, expected)

        println(rrs.printUsedAutomaton("S"))

        assertEquals(1, actual.maxNumHeads)

    }

    @Test
    fun a150() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(150)

        val expected = "S|1 { S1 { ".repeat(149) + "S { 'a' }" + "'a' } }".repeat(149)

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a500() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(500)

        val expected = "S|1 { S1 { ".repeat(499) + "S { 'a' }" + "'a' } }".repeat(499)

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a2000() {
        val rrs = S
        val goal = "S"
        val sentence = "a".repeat(2000)

        val expected = "S|1 { S1 { ".repeat(1999) + "S { 'a' }" + "'a' } }".repeat(1999)

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }
}