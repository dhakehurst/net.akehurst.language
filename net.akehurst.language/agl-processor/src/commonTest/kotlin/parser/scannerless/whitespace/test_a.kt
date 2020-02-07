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

class test_a : test_ScannerlessParserAbstract() {

    // skip WS = "\s+" ;
    // S = 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        b.rule("S").concatenation(b.literal("a"))
        return b
    }

    @Test
    fun S_S_a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun WSa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a"

        val expected = """
            S { WS { "\s+" : ' ' } 'a' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun aWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a "

        val expected = """
            S { 'a' WS { "\s+" : ' ' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a "

        val expected = """
            S { WS { "\s+" : ' ' } 'a' WS { "\s+" : ' ' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }


    // skip WS = "\s+" ;
    // S = 'a' 'b' ;
    private fun Sab(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_b = b.literal("b")
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        b.rule("S").concatenation(r_a, r_b)
        return b
    }

    @Test
    fun Sab_S_ab() {
        val rrb = this.Sab()
        val goal = "S"
        val sentence = "ab"

        val expected = """
            S { 'a' 'b' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun Sab_WSab() {
        val rrb = this.Sab()
        val goal = "S"
        val sentence = " ab"

        val expected = """
            S { WS { "\s+" : ' ' } 'a' 'b' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun Sab_aWSb() {
        val rrb = this.Sab()
        val goal = "S"
        val sentence = "a b"

        val expected = """
            S { 'a' WS { "\s+" : ' ' } 'b' }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun Sab_WSaWSbWS() {
        val rrb = this.Sab()
        val goal = "S"
        val sentence = " a b "

        val expected = """
            S { WS { "\s+" : ' ' } 'a' WS { "\s+" : ' ' } 'b' WS { "\s+" : ' ' } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }
}