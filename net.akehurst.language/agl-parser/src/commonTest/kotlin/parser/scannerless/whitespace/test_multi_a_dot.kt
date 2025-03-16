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

class test_multi_a_dot : test_LeftCornerParserAbstract() {

    // skip WS = "\s+" ;
    // S = ad* ;
    // ad = a '.' ;
    // a = 'a' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            multi("S",0,-1,"ad")
            concatenation("ad") { ref("a");literal(".") }
            concatenation("a") { literal("a") }
        }
        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a."

        val expected = """
            S {
                ad {
                    a { 'a' }
                    '.'
                }
            }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aaa() {
        val sentence = "a.a.a."

        val expected = """
            S {
                ad { a { 'a' } '.' }
                ad { a { 'a' } '.' }
                ad { a { 'a' } '.' }
            }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun asDot_as_aWS() {
        val sentence = "a. "

        val expected = """
            S {
                ad { a { 'a' } '.' WS { "\s+" : ' ' } }
            }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

}