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

package net.akehurst.language.parser.scannerless.whitespace

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.fail

class test_multi_a_WS : test_ScannerlessParserAbstract() {

    // skip WS = "\s+" ;
    // S = a* ;
    // a = 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        val r_a = b.rule("a").concatenation(b.literal("a"))
        b.rule("S").multi(0,-1,r_a)
        return b
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { a { 'a' } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun WSa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a"

        val expected = """
            S {
                WS { '\s+' : ' ' }
                a { 'a' }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S {
                a { 'a' }
                a { 'a' }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWSaWSa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a a a"

        val expected = """
            S {
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWSaWSa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a a a"

        val expected = """
            S {
                WS { '\s+' : ' ' }
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWSaWSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a a a "

        val expected = """
            S {
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' WS { '\s+' : ' ' } }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWSaWSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a a a "

        val expected = """
            S {
                WS { '\s+' : ' ' }
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' WS { '\s+' : ' ' } }
                a { 'a' WS { '\s+' : ' ' } }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

}