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
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_ab : test_LeftCornerParserAbstract() {

    // skip WS = "\s+" ;
    // S = 'a' 'b' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
    }

    @Test
    fun Sab_S_ab() {
        val sentence = "ab"

        val expected = """
            S { 'a' 'b' }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun Sab_WSab() {
        val sentence = " ab"

        val expected = """
            S { WS { "\s+" : ' ' } 'a' 'b' }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun Sab_aWSb() {
        val sentence = "a b"

        val expected = """
            S { 'a' WS { "\s+" : ' ' } 'b' }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun Sab_WSaWSbWS() {
        val sentence = " a b "

        val expected = """
            S { WS { "\s+" : ' ' } 'a' WS { "\s+" : ' ' } 'b' WS { "\s+" : ' ' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }
}