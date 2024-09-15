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

package net.akehurst.language.parser.leftcorner.whitespace

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_sList_a_WS : test_LeftCornerParserAbstract() {

    // skip WS = "\s+" ;
    // S = [a / ',']* ;
    // a = 'a' ;

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
    }

    @Test
    fun empty() {
        val goal = "S"
        val sentence = ""

        val expected = """
            S|1 { <EMPTY_LIST> }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun WSa() {
        val goal = "S"
        val sentence = " a"

        val expected = """
            S {
                WS : ' ' 
                'a' 
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acaca() {
        val goal = "S"
        val sentence = "a,a,a"

        val expected = """
            S {
              'a'
              ','
              'a'
              ','
              'a'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acWSacWSa() {
        val goal = "S"
        val sentence = "a, a, a"

        val expected = """
            S {
              'a'
              ',' WS : ' '
              'a'
              ',' WS : ' '
              'a'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aWScaWSca() {
        val goal = "S"
        val sentence = "a ,a ,a"

        val expected = """
            S {
              'a'  WS : ' '
              ','
              'a'  WS : ' '
              ','
              'a'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aWScWSaWScWSa() {
        val goal = "S"
        val sentence = "a , a , a"

        val expected = """
            S {
              'a'  WS : ' '
              ','  WS : ' '
              'a'  WS : ' '
              ','  WS : ' '
              'a'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun WSaWScWSaWScWSa() {
        val goal = "S"
        val sentence = " a , a , a"

        val expected = """
            S {
              WS : ' '
              'a'  WS : ' '
              ','  WS : ' '
              'a'  WS : ' '
              ','  WS : ' '
              'a'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aWScWSaWScWSaWS() {
        val goal = "S"
        val sentence = "a , a , a "

        val expected = """
            S {
              'a'  WS : ' '
              ','  WS : ' '
              'a'  WS : ' '
              ','  WS : ' '
              'a'  WS : ' '
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun WSaWScWSaWScWSaWS() {
        val goal = "S"
        val sentence = " a , a , a "

        val expected = """
            S {
              WS : ' '
              'a'  WS : ' '
              ','  WS : ' '
              'a'  WS : ' '
              ','  WS : ' '
              'a'  WS : ' '
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

}