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

package net.akehurst.language.parser.leftcorner.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_bodmas_exprOpExprRules_choicePriority : test_LeftCornerParserAbstract() {

    // S =  expr ;
    // expr = var < bool < group < div < mul < add < sub ;
    // sub = expr '-' expr ;
    // add = expr '+' expr ;
    // mul = expr '*' expr ;
    // div = expr '/' expr ;
    // group = '(' expr ')' ;
    // bool = 'true' | 'false' ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
                ref("bool")
                ref("group")
                ref("div")
                ref("mul")
                ref("add")
                ref("sub")
            }
            concatenation("div") { ref("expr"); literal("/"); ref("expr") }
            concatenation("mul") { ref("expr"); literal("*"); ref("expr") }
            concatenation("add") { ref("expr"); literal("+"); ref("expr") }
            concatenation("sub") { ref("expr"); literal("-"); ref("expr") }
            concatenation("group") { literal("("); ref("expr"); literal(")") }
            choice("bool", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("true");literal("false") }
            concatenation("var") { pattern("[a-zA-Z]+") }
            preferenceFor("expr") {
                left(listOf("sub"), setOf("'-'"))
                left(listOf("add"), setOf("'+'"))
                left(listOf("mul"), setOf("'*'"))
                left(listOf("div"), setOf("'/'"))
            }
        }.also {
            // it.buildFor("S", AutomatonKind.LOOKAHEAD_1)
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
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("\"[a-zA-Z]+\"", "'true'", "'false'", "'('"))
            ), issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
              expr { var { "[a-zA-Z]+" : 'a' } }
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
    fun expr_true() {
        val sentence = "true"

        val expected = """
              expr|1 {
                bool { 'true' }
              }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = "expr",
            sentence = sentence,
            expectedNumGSSHeads = 2,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun S_true() {
        val sentence = "true"

        val expected = """
            S {
              expr|1 {
                bool { 'true' }
              }
            }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 2,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun S_var() {
        val sentence = "var"

        val expected = """
            S {
              expr {
                var { "[a-zA-Z]+" : 'var' }
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
    fun Og_a_Cg() {
        val sentence = "(a)"

        val expected = """
            S {
              expr|2 {
                group {
                  '('
                  expr { var { "[a-zA-Z]+" : 'a' } }
                  ')'
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
    fun a_div_b() {
        val sentence = "a / b"

        val expected = """
            S {
              expr|3 {
                div {
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  '/' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' } }
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
    fun a_mul_b() {
        val sentence = "a * b"

        val expected = """
            S {
              expr|4 {
                mul {
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  '*' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' } }
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
    fun a_add_b() {
        val sentence = "a + b"

        val expected = """
            S {
              expr|5 {
                add {
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  '+' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' } }
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
    fun a_sub_b() {
        val sentence = "a - b"

        val expected = """
            S {
              expr|6 {
                sub {
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  '-' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' } }
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
    fun a_add_b_mul_c() {
        val sentence = "a+b*c"

        val expected = """
            S {
             expr|5 {
              add {
                expr { var { "[a-zA-Z]+" : 'a' } }
                '+'
                expr|4 {
                  mul {
                    expr { var { "[a-zA-Z]+" : 'b' } }
                    '*'
                    expr { var { "[a-zA-Z]+" : 'c' } }
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
    fun a_mul_b_add_c() {
        val sentence = "a*b+c"

        val expected = """
            S {
             expr {
              add {
                expr {
                  mul {
                    expr { var { "[a-zA-Z]+" : 'a' } }
                    '*'
                    expr { var { "[a-zA-Z]+" : 'b' } }
                  }
                }
               '+'
               expr { var { "[a-zA-Z]+" : 'c' } }
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
    fun a_add_b_add_c_add_d() {
        val sentence = "a+b+c+c+d"

        val expected = """
         S { expr { add {
              expr { add {
                  expr { add {
                      expr { add {
                          expr { var { "[a-zA-Z]+" : 'a' } }
                          '+'
                          expr { var { "[a-zA-Z]+" : 'b' } }
                        } }
                      '+'
                      expr { var { "[a-zA-Z]+" : 'c' } }
                    } }
                  '+'
                  expr { var { "[a-zA-Z]+" : 'c' } }
                } }
              '+'
              expr { var { "[a-zA-Z]+" : 'd' } }
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
    fun a_add_b_add_c_add_d_add_e_add_f() {
        val sentence = "a+b+c+d+e+f"

        val expected = """
         S { expr { add {
              expr { add {
                  expr { add {
                      expr { add {
                          expr { add {
                              expr { var { "[a-zA-Z]+" : 'a' } }
                              '+'
                              expr { var { "[a-zA-Z]+" : 'b' } }
                            } }
                          '+'
                          expr { var { "[a-zA-Z]+" : 'c' } }
                        } }
                      '+'
                      expr { var { "[a-zA-Z]+" : 'd' } }
                    } }
                  '+'
                  expr { var { "[a-zA-Z]+" : 'e' } }
                } }
              '+'
              expr { var { "[a-zA-Z]+" : 'f' } }
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
    fun a_add_b_div_c_mul_d_add_e_sub_f() {
        val sentence = "a+b/c*d+e-f"

        val expected = """
            S { expr { sub {
              expr { add {
                expr { add {
                  expr { var { "[a-zA-Z]+" : 'a' } }
                  '+'
                  expr { mul {
                    expr { div {
                      expr { var { "[a-zA-Z]+" : 'b' } }
                      '/'
                      expr { var { "[a-zA-Z]+" : 'c' } }
                    } }
                    '*'
                    expr { var { "[a-zA-Z]+" : 'd' } }
                  } }
                } }
                '+'
                expr { var { "[a-zA-Z]+" : 'e' } }
              } }
              '-'
              expr { var { "[a-zA-Z]+" : 'f' } }
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
    fun Og_a_add_b_Cg_mul_c() {
        val sentence = "(a+b)*c"

        val expected = """
            S { expr { mul {
              expr {
                group {
                  '('
                    expr {
                      add {
                        expr { var { "[a-zA-Z]+" : 'a' } }
                        '+'
                        expr { var { "[a-zA-Z]+" : 'b' } }
                      }
                    }
                  ')'
                }
              }
              '*'
              expr { var { "[a-zA-Z]+" : 'c' } }
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

}