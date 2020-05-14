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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class  test_a : test_ScanOnDemandParserAbstract() {

    // S =  'a' | S1 ;
    // S1 = S 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_S1 = b.rule("S1").concatenation(r_S, r_a)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE,RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, arrayOf(r_a, r_S1))
        return b
    }

    @Test
    fun a() {
        val rrb = this.S()
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
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S|1 { S1 { S { 'a' } 'a' } }
        """.trimIndent()

        val actual = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun aaa() {
        val rrb = this.S()
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

        val actual = super.testStringResult(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a50() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(50)

        val expected = "S|1 { S1 { ".repeat(49) + "S { 'a' }" + "'a' } }".repeat(49)

        val actual = super.test(rrb, goal, sentence, expected)

        println(rrb.ruleSet().printUsedAutomaton("S"))

        assertEquals(1, actual.maxNumHeads)

    }

    @Test
    fun a150() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(150)

        val expected = "S|1 { S1 { ".repeat(149) + "S { 'a' }" + "'a' } }".repeat(149)

        val actual = super.test(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a500() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(500)

        val expected = "S|1 { S1 { ".repeat(499) + "S { 'a' }" + "'a' } }".repeat(499)

        val actual = super.test(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun a2000() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(2000)

        val expected = "S|1 { S1 { ".repeat(1999) + "S { 'a' }" + "'a' } }".repeat(1999)

        val actual = super.test(rrb, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }
}