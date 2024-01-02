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

internal class test_aOpt : test_LeftCornerParserAbstract() {

    // skip WS = "\s+" ;
    // S = 'a'? ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { ref("aOpt") }
            optional("aOpt", "'a'")
            literal("'a'", "a")
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val expected = """
            S { aOpt|1 { §empty } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun WS() {
        val sentence = " "

        val expected = """
            S {
              WS { "\s+" : ' ' }
              aOpt|1 { §empty }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { aOpt { 'a' } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun WSa() {
        val sentence = " a"

        val expected = """
            S { WS { "\s+" : ' ' } aOpt { 'a' } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aWS() {
        val sentence = "a "

        val expected = """
            S { aOpt { 'a' WS { "\s+" : ' ' } } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun WSaWS() {
        val sentence = " a "

        val expected = """
            S {
              WS { "\s+" : ' ' }
              aOpt { 'a' WS { "\s+" : ' ' } }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

}