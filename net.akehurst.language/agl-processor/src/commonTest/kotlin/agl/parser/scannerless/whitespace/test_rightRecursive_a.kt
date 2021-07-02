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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_rightRecursive_a : test_ScanOnDemandParserAbstract() {

    // S =  'a' | S1 ;
    // S1 = 'a' S ;
    // skip WS = "\s+" ;
    private companion object {
        val S = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S") }
            skip("WS") { pattern("\\s+")}
        }
    }

    @Test
    fun WSaWS() {
        val goal = "S"
        val sentence = " a "

        val expected = """
            S { WS { "\s+" : ' ' } 'a' WS { "\s+" : ' ' }}
        """.trimIndent()

        super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }


    @Test
    fun WSaWSaWS() {
        val goal = "S"
        val sentence = " a a "

        val expected = """
            S|1 { WS { "\s+" : ' ' } S1 { 'a' WS { "\s+" : ' ' } S { 'a' WS { "\s+" : ' ' } } } }
        """.trimIndent()

        super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun WSaWSaWSaWS() {
        val goal = "S"
        val sentence = " a a a "

        val expected = """
            S|1 { WS { "\s+" : ' ' }
                S1 {
                    'a' WS { "\s+" : ' ' }
                    S|1 {
                        S1 {
                            'a' WS { "\s+" : ' ' }
                            S { 'a'  WS { "\s+" : ' ' } }
                        }
                    }
                }
            }
        """.trimIndent()

        super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aWS500() {
        val goal = "S"
        val sentence = "a ".repeat(500)

        val expected = "S { S1 { 'a' WS { \"\\s+\" : ' ' } ".repeat(499) + "S { 'a' WS { \"\\s+\" : ' ' } }" +" } }".repeat(499)


        super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

}