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

class test_a : test_LeftCornerParserAbstract() {

    // skip WS = "\s+" ;
    // S = 'a' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { literal("a") }
        }
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        super.test_pass(
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
            S { WS { "\s+" : ' ' } 'a' }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aWS() {
        val goal = "S"
        val sentence = "a "

        val expected = """
            S { 'a' WS { "\s+" : ' ' } }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun WSaWS() {
        val goal = "S"
        val sentence = " a "

        val expected = """
            S { WS { "\s+" : ' ' } 'a' WS { "\s+" : ' ' } }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

}