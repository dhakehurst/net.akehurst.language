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

package net.akehurst.language.parser.scanondemand.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_da_sList_root_choicePriority : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // expr = root < div < add ;
    // root = var ;
    // add = [ expr / '+' ]2+ ;
    // div = [ expr / '/' ]2+ ;
    // var = 'v' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("root")
                ref("div")
                ref("add")
            }
            choice("root", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
            }
            sList("div", 2, -1, "expr", "'/'")
            sList("add", 2, -1, "expr", "'+'")
            concatenation("var") { literal("v") }
            literal("'/'", "/")
            literal("'+'", "+")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0,1,1,1),"^", setOf("'v'"))
            ), issues
        )
    }

    @Test
    fun v() {
        val sentence = "v"

        val expected = """
            S {
              expr { root{ var { 'v' } } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun v_div_v() {
        val sentence = "v/v"

        val expected = """
            S {
              expr|1 {
                div {
                  expr { root { var { 'v' } } }
                  '/'
                  expr { root { var { 'v' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }
    @Test
    fun v_add_v() {
        val sentence = "v+v"

        val expected = """
            S {
              expr|2 {
                add {
                  expr { root { var { 'v' } } }
                  '+'
                  expr { root { var { 'v' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun v_add_v_div_v() {
        val sentence = "v+v/v"

        val expected = """
            S {
             expr|2 {
              add {
                expr { root { var { 'v' } } }
                '+'
                expr|1 {
                  div {
                    expr { root { var { 'v' } } }
                    '/'
                    expr { root { var { 'v' } } }
                  }
                }
              }
             }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun v_div_v_add_v() {
        val sentence = "v/v+v"

        val expected = """
            S {
             expr|4 {
              add {
                expr|1 {
                  div {
                    expr { root { var { 'v' } } }
                    '/'
                    expr { root { var { 'v' } } }
                  }
                }
               '+'
               expr { root { var { 'v' } } }
              }
             }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun v_div_v_div_v_add_v() {
        val sentence = "v/v/v+v"

        val expected = """
            S {
             expr|2 {
              add {
                expr|3 {
                  div {
                    expr { root { var { 'v' } } }
                    '/'
                    expr { root { var { 'v' } } }
                    '/'
                    expr { root { var { 'v' } } }
                  }
                }
               '+'
               expr { root { var { 'v' } } }
              }
             }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun v_add_v_div_v_div_v_add_v_add_v() {
        val sentence = "v+v/v/v+v+v"

        val expected = """
         S { expr|2 { add {
              expr { root { var { 'v' } } }
              '+'
              expr|1 { div {
                  expr { root { var { 'v' } } }
                  '/'
                  expr { root { var { 'v' } } }
                  '/'
                  expr { root { var { 'v' } } }
                } }
              '+'
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun v_add_v_add_v_add_v() {
        val sentence = "v+v+v+v+v"

        val expected = """
             S { expr|4 { add {
                  expr { root { var { 'v' } } }
                  '+'
                  expr { root { var { 'v' } } }
                  '+'
                  expr { root { var { 'v' } } }
                  '+'
                  expr { root { var { 'v' } } }
                  '+'
                  expr { root { var { 'v' } } }
                } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b_add_c_add_d_add_e_add_f() {
        val sentence = "v+v+v+v+v+v+v"

        val expected = """
         S { expr|4 { add {
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
              '+'
              expr { root { var { 'v' } } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}