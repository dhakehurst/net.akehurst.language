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

package net.akehurst.language.parser.leftcorner.group

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_group : test_LeftCornerParserAbstract() {

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("R") { ref("grp") }
            concatenation("grp") { ref("A"); ref("Am") }
            multi("Am", 0, -1, "A")
            literal("A", "A")
        }
    }

    @Test
    fun t1() {
        val goal = "R"
        val sentence = "A"

        val expected = """
            R {
              grp { A:'A' Am|1 { <EMPTY_LIST> } }
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
    fun t2() {
        val goal = "R"
        val sentence = "AA"

        val expected = """
            R {
              grp { A:'A' Am { A:'A' } }
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
    fun t3() {
        val goal = "R"
        val sentence = "AAA"

        val expected = """
            R {
              grp { A:'A' Am { A:'A' A:'A' } }
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