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

internal class test_sList_compositeMulti : test_LeftCornerParserAbstract() {

    // S = [nl / ';']*
    // nl = N cnm
    // cnm = cn*
    // cn = ',' N
    // N = "[0-9]+"

    private companion object {
        val rrs = runtimeRuleSet {
            //pattern("WS", "\\s+", true)
            sList("S", 0, -1, "nl", "SMI")
            concatenation("nl") { ref("N"); ref("cnm") }
            multi("cnm", 0, -1, "cn")
            concatenation("cn") { ref("CMR"); ref("N"); }
            //optional("cmOp","cm")
            literal("CMR", ",")
            literal("SMI", ";")
            pattern("N", "[0-9]+")
        }
    }

    @Test
    fun _1() {
        val goal = "S"
        val sentence = "1"

        val expected = """
              S { nl {
                  N : '1'
                  cnm|1 { <EMPTY_LIST> }
                } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun _1c2() {
        val goal = "S"
        val sentence = "1,2"

        val expected = """
              S { nl {
                  N : '1'
                  cnm { cn{ CMR:',' N:'2' }}
                } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun _1s2() {
        val goal = "S"
        val sentence = "1;2"

        val expected = """
         S {
          nl {
            N : '1'
            cnm|1 { <EMPTY_LIST> }
          }
          SMI:';'
          nl {
            N : '2'
            cnm|1 { <EMPTY_LIST> }
          }
        }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }
}