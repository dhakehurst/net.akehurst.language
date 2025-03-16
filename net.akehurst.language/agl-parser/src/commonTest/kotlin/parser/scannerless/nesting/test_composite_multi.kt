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

package net.akehurst.language.parser.leftcorner.nesting

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_composite_multi : test_LeftCornerParserAbstract() {

    // S = NUM (',' NUM)* ;
    // NUM = "[0-9]+"

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("NUM"); ref("cmNumMulti") }
            multi("cmNumMulti", 0, -1, "cmNum")
            concatenation("cmNum") { ref("CMR"); ref("NUM"); }
            literal("CMR", ",")
            pattern("NUM", "[0-9]+")
        }
    }

    @Test
    fun _1() {
        val goal = "S"
        val sentence = "1"

        val expected = """
            S {
                NUM : '1'
                cmNumMulti|1 { <EMPTY_LIST> }
            }
        """.trimIndent()

        val actual = super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun _1c2() {
        val goal = "S"
        val sentence = "1,2"

        val expected = """
            S {
                NUM : '1'
                cmNumMulti { cmNum {
                  CMR:','
                  NUM : '2'
                } }
            }
        """.trimIndent()

        val actual = super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }


}