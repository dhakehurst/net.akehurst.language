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

package net.akehurst.language.parser.scanondemand.whitespace

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

class test_rightRecursive_a : test_ScanOnDemandParserAbstract() {

    // S =  'a' | S1 ;
    // S1 = 'a' S ;
    // skip WS = "\s+" ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_S1 = b.rule("S1").concatenation(r_a, r_S)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleRhsItemsKind.CHOICE,RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, arrayOf(r_a, r_S1))
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        return b
    }

    @Test
    fun WSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a "

        val expected = """
            S { WS { "\s+" : ' ' } 'a' WS { "\s+" : ' ' }}
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


    @Test
    fun WSaWSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a a "

        val expected = """
            S|1 { WS { "\s+" : ' ' } S1 { 'a' WS { "\s+" : ' ' } S { 'a' WS { "\s+" : ' ' } } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWSaWSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a a a "

        val expected = """
            S { WS { "\s+" : ' ' }
                S1 {
                    'a' WS { "\s+" : ' ' }
                    S {
                        S1 {
                            'a' WS { "\s+" : ' ' }
                            S { 'a'  WS { "\s+" : ' ' } }
                        }
                    }
                }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWS500() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a ".repeat(500)

        val expected = "S { S1 { 'a' WS { \"\\s+\" : ' ' } ".repeat(499) + "S { 'a' WS { \"\\s+\" : ' ' } }" +" } }".repeat(499)


        super.test(rrb, goal, sentence, expected)
    }

}