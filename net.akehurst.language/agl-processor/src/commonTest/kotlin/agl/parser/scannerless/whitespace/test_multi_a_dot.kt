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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_multi_a_dot : test_ScanOnDemandParserAbstract() {

    // skip WS = "\s+" ;
    // S = ad* ;
    // ad = a '.' ;
    // a = 'a' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        val r_a = b.rule("a").concatenation(b.literal("a"))
        val r_ad = b.rule("ad").concatenation(r_a, b.literal("."))
        b.rule("S").multi(0,-1,r_ad)
        return b
    }
    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a."

        val expected = """
            S {
                ad {
                    a { 'a' }
                    '.'
                }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a.a.a."

        val expected = """
            S {
                ad { a { 'a' } '.' }
                ad { a { 'a' } '.' }
                ad { a { 'a' } '.' }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun asDot_as_aWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a. "

        val expected = """
            S {
                ad { a { 'a' } '.' WS { "\s+" : ' ' } }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

}