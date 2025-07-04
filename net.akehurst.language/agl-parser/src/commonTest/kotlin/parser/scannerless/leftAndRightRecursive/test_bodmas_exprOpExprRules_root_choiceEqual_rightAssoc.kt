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

package net.akehurst.language.parser.leftcorner.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_bodmas_exprOpExprRules_root_choiceEqual_rightAssoc : test_LeftCornerParserAbstract() {

    // S =  expr ;
    // expr = root | mul | add ;
    // root = 'v'
    // mul = expr '*' expr ;
    // add = expr '+' expr ;
    //
    // precedence
    // add right
    // mul right

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("root")
                ref("mul")
                ref("add")
            }
            concatenation("root") { literal("v") }
            concatenation("mul") { ref("expr"); literal("*"); ref("expr") }
            concatenation("add") { ref("expr"); literal("+"); ref("expr") }
            preferenceFor("expr") {
                right(listOf("add"), setOf("'+'"))
                right(listOf("mul"), setOf("'*'"))
            }
        }

        const val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'v'"))
            ), issues.errors)
    }

    @Test
    fun v() {
        val sentence = "v"

        val expected = """
            S {
              expr { root { 'v' } }
            }
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
    fun v_add_v() {
        val sentence = "v+v"

        val expected = """
            S {
              expr {
                add {
                  expr { root { 'v' } }
                  '+'
                  expr { root { 'v' } }
                }
              }
            }
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
    fun v_mul_v() {
        val sentence = "v*v"

        val expected = """
            S {
              expr {
                mul {
                  expr { root { 'v' } }
                  '*'
                  expr { root { 'v' } }
                }
              }
            }
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
    fun v_add_v_mul_v() {
        val sentence = "v+v*v"

        val expected = """
            S {
             expr {
              add {
                expr { root { 'v' } }
                '+'
                expr {
                  mul {
                    expr { root { 'v' } }
                    '*'
                    expr { root { 'v' } }
                  }
                }
              }
             }
            }
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
    fun v_mul_v_add_v() {
        val sentence = "v*v+v"

        val expected = """
            S {
             expr {
              add {
                expr {
                  mul {
                    expr { root { 'v' } }
                    '*'
                    expr { root { 'v' } }
                  }
                }
               '+'
               expr { root { 'v' } }
              }
             }
            }
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
    fun v_add_v_add_v_add_v() {
        val sentence = "v+v+v+v+v"

        val expected = """
         S { expr { add {
              expr { root { 'v' } }
              '+'
              expr { add {
                  expr { root { 'v' } }
                  '+'
                  expr { add {
                      expr { root { 'v' } }
                      '+'
                      expr { add {
                          expr { root { 'v' } }
                          '+'
                          expr { root { 'v' } }
                        } }
                    } }
                } }
            } } }
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
    fun v_add_v_add_v_add_v_add_v_add_v() {
        val sentence = "v+v+v+v+v+v+v"

        val expected = """
         S { expr { add {
              expr { root { 'v' } }
              '+'
              expr { add {
                  expr { root { 'v' } }
                  '+'
                  expr { add {
                      expr { root { 'v' } }
                      '+'
                      expr { add {
                          expr { root { 'v' } }
                          '+'
                          expr { add {
                              expr { root { 'v' } }
                              '+'
                              expr { add {
                                  expr { root { 'v' } }
                                  '+'
                                  expr { root { 'v' } }
                                } }
                            } }
                        } }
                    } }
                } }
            } } }
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
    fun v_add_v_div_v_div_v_add_v_add_v() {
        val sentence = "v+v*v*v+v+v"

        val expected = """
            S { expr { add {
              expr { root { 'v' } }
              '+'
              expr { add {
                expr { mul {
                  expr { root { 'v' } }
                  '*'
                  expr { mul {
                    expr { root { 'v' } }
                    '*'
                    expr { root { 'v' } }
                  } }
                } }
                '+'
                expr { add {
                  expr { root { 'v' } }
                  '+'
                  expr { root { 'v' } }
                } }
              } }
            } } }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }
}