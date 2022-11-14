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

internal class test_multi_a_WS : test_ScanOnDemandParserAbstract() {

    // skip WS = "\s+" ;
    // S = a* ;
    // a = 'a' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            multi("S",0,-1,"a")
            concatenation("a") { literal("a") }
        }
        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { a { 'a' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun WSa() {
        val sentence = " a"

        val expected = """
            S {
                WS { "\s+" : ' ' }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S {
                a { 'a' }
                a { 'a' }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aWSaWSa() {
        val sentence = "a a a"

        val expected = """
            S {
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun WSaWSaWSa() {
        val sentence = " a a a"

        val expected = """
            S {
                WS { "\s+" : ' ' }
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aWSaWSaWS() {
        val sentence = "a a a "

        val expected = """
            S {
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' WS { "\s+" : ' ' } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun WSaWSaWSaWS() {
        val sentence = " a a a "

        val expected = """
            S {
                WS { "\s+" : ' ' }
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' WS { "\s+" : ' ' } }
                a { 'a' WS { "\s+" : ' ' } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
        //super.testStringResult(rrb, goal, sentence, expected) //works
        //super.test(rrb, goal, sentence, expected) //fails ?
    }

}