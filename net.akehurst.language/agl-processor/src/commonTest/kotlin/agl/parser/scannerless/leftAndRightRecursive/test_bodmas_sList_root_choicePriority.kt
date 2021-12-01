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

internal class test_bodmas_sList_root_choicePriority : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // expr = root < group < div < mul < add < sub ;
    // root = var < bool ;
    // sub = [ expr / '-' ]2+ ;
    // add = [ expr / '+' ]2+ ;
    // mul = [ expr / '*' ]2+ ;
    // div = [ expr / '/' ]2+ ;
    // group = '(' expr ')' ;
    // bool = 'true' | 'false' ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private companion object {
        val rrs = runtimeRuleSet {
            skip("WS") { pattern("\\s+") }
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("root")
                ref("group")
                ref("div")
                ref("mul")
                ref("add")
                ref("sub")
            }
            choice("root", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
                ref("bool")
            }
            sList("div", 2, -1, "expr", "'/'")
            sList("mul", 2, -1, "expr", "'*'")
            sList("add", 2, -1, "expr", "'+'")
            sList("sub", 2, -1, "expr", "'-'")
            concatenation("group") { literal("("); ref("expr"); literal(")") }
            choice("bool", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("true");literal("false") }
            concatenation("var") { pattern("[a-zA-Z]+") }
            literal("'/'", "/")
            literal("'*'", "*")
            literal("'+'", "+")
            literal("'-'", "-")
        }
        val goal = "S"
    }

    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_expr = b.rule("expr").build()
        val r_var = b.rule("var").concatenation(b.pattern("[a-zA-Z]+"))
        val r_bool = b.rule("bool").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, b.literal("true"), b.literal("false"))
        val r_group = b.rule("group").concatenation(b.literal("("), r_expr, b.literal(")"))
        val r_div = b.rule("div").separatedList(2, -1, b.literal("/"), r_expr)
        val r_mul = b.rule("mul").separatedList(2, -1, b.literal("*"), r_expr)
        val r_add = b.rule("add").separatedList(2, -1, b.literal("+"), r_expr)
        val r_sub = b.rule("sub").separatedList(2, -1, b.literal("-"), r_expr)
        val r_root = b.rule("root").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST, r_var, r_bool)
        b.rule(r_expr).choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST, r_root, r_group, r_div, r_mul, r_add, r_sub)
        b.rule("S").concatenation(r_expr)
        b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        return b
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0,1,1,1),"^", setOf("\"[a-zA-Z]+\"","'true'","'false'","'('"))
            ), issues
        )
    }


    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
              expr { root{ var { "[a-zA-Z]+" : 'a' } } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun expr_true() {
        val sentence = "true"

        val expected = """
              S{ expr { root|1 {
                bool { 'true' }
              } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun S_true() {
        val sentence = "true"

        val expected = """
            S {
              expr { root|1 {
                bool { 'true' }
              } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun S_var() {
        val sentence = "var"

        val expected = """
            S {
              expr { root {
                var { "[a-zA-Z]+" : 'var' }
              } }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun S_group_a() {
        val sentence = "(a)"

        val expected = """
            S {
              expr|1 {
                group {
                  '('
                  expr { root { var { "[a-zA-Z]+" : 'a' } } }
                  ')'
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_div_b() {
        val sentence = "a / b"

        val expected = """
            S {
              expr|2 {
                div {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  '/' WS { "\s+" : ' ' }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_mul_b() {
        val sentence = "a * b"

        val expected = """
            S {
              expr|3 {
                mul {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  '*' WS { "\s+" : ' ' }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b() {
        val sentence = "a + b"

        val expected = """
            S {
              expr|4 {
                add {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  '+' WS { "\s+" : ' ' }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_sub_b() {
        val sentence = "a - b"

        val expected = """
            S {
              expr|5 {
                sub {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  '-' WS { "\s+" : ' ' }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b_mul_c() {
        val sentence = "a+b*c"

        val expected = """
            S {
             expr|4 {
              add {
                expr { root { var { "[a-zA-Z]+" : 'a' } } }
                '+'
                expr|3 {
                  mul {
                    expr { root { var { "[a-zA-Z]+" : 'b' } } }
                    '*'
                    expr { root { var { "[a-zA-Z]+" : 'c' } } }
                  }
                }
              }
             }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_mul_b_add_c() {
        val sentence = "a*b+c"

        val expected = """
            S {
             expr|4 {
              add {
                expr|3 {
                  mul {
                    expr { root { var { "[a-zA-Z]+" : 'a' } } }
                    '*'
                    expr { root { var { "[a-zA-Z]+" : 'b' } } }
                  }
                }
               '+'
               expr { root { var { "[a-zA-Z]+" : 'c' } } }
              }
             }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_mul_b_mul_c_add_d() {
        val sentence = "a*b*c+d"

        val expected = """
            S {
             expr|4 {
              add {
                expr|3 {
                  mul {
                    expr { root { var { "[a-zA-Z]+" : 'a' } } }
                    '*'
                    expr { root { var { "[a-zA-Z]+" : 'b' } } }
                    '*'
                    expr { root { var { "[a-zA-Z]+" : 'c' } } }
                  }
                }
               '+'
               expr { root { var { "[a-zA-Z]+" : 'd' } } }
              }
             }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b_mul_c_mul_d_add_f_add_g() {
        val sentence = "a+b*c*d+f+g"

        val expected = """
         S { expr|4 { add {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              '+'
              expr|3 { mul {
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                  '*'
                  expr { root { var { "[a-zA-Z]+" : 'c' } } }
                  '*'
                  expr { root { var { "[a-zA-Z]+" : 'd' } } }
                } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'f' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'g' } } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b_add_c_add_d() {
        val sentence = "a+b+c+c+d"

        val expected = """
             S { expr|4 { add {
                  expr { root { var { "[a-zA-Z]+" : 'a' } } }
                  '+'
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                  '+'
                  expr { root { var { "[a-zA-Z]+" : 'c' } } }
                  '+'
                  expr { root { var { "[a-zA-Z]+" : 'c' } } }
                  '+'
                  expr { root { var { "[a-zA-Z]+" : 'd' } } }
                } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_add_b_add_c_add_d_add_e_add_f() {
        val sentence = "a+b+c+c+d+e+f"

        val expected = """
         S { expr|4 { add {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'd' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'e' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'f' } } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }


    @Test
    fun Og_a_add_b_Cg_mul_c() {
        val sentence = "(a+b)*c"

        val expected = """
            S { expr|3 { mul {
              expr|1 {
                group {
                  '('
                    expr|4 {
                      add {
                        expr { root { var { "[a-zA-Z]+" : 'a' } } }
                        '+'
                        expr { root { var { "[a-zA-Z]+" : 'b' } } }
                      }
                    }
                  ')'
                }
              }
              '*'
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
            } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }


}