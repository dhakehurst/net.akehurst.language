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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

class test_sList_a_WS : test_ScanOnDemandParserAbstract() {

    // skip WS = "\s+" ;
    // S = [a / ',']* ;
    // a = 'a' ;
    private val S = runtimeRuleSet {
        pattern("WS", "\\s+", true)
        sList("S",0,-1,"'a'", "','")
        literal("'a'", "a")
        literal("','", ",")
    }

    @Test
    fun empty() {
        val rrs = this.S
        val goal = "S"
        val sentence = ""

        val expected = """
            S|1 { §empty }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }

    @Test
    fun a() {
        val rrs = this.S
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }

    @Test
    fun WSa() {
        val rrb = this.S
        val goal = "S"
        val sentence = " a"

        val expected = """
            S {
                WS : ' ' 
                'a' 
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun acaca() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun acWSacWSa() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWScaWSca() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWScWSaWScWSa() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWScWSaWScWSa() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWScWSaWScWSaWS() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWScWSaWScWSaWS() {
        val rrb = this.S
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

        super.test(rrb, goal, sentence, expected)
    }

}