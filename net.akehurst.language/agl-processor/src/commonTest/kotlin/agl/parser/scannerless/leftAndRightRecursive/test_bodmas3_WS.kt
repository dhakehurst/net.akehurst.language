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

package net.akehurst.language.parser.scanondemand.leftAndRightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_bodmas3_WS : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // expr = root | group | infix ;
    // root = var < bool
    // infix = [ expr / op ]2+ ;
    // group = '(' expr ')' ;
    // bool = 'true' | 'false' ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_expr = b.rule("expr").build()
        val r_var = b.rule("var").concatenation(b.pattern("[a-zA-Z]+"))
        val r_bool = b.rule("bool").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, b.literal("true"), b.literal("false"))
        val r_op = b.rule("op").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,b.literal("/"), b.literal("*"),b.literal("+"),b.literal("-"))
        val r_group = b.rule("group").concatenation(b.literal("("), r_expr, b.literal(")"))
        val r_infix = b.rule("infix").separatedList(2, -1, r_op, r_expr)
        val r_root = b.rule("root").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST,r_var, r_bool)
        b.rule(r_expr).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_root, r_group, r_infix)
        b.rule("S").concatenation(r_expr)
        b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        return b
    }

    @Test
    fun empty_fails() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }


    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected = """
            S {
              expr { root{ var { "[a-zA-Z]+" : 'a' } } }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)

    }

    @Test
    fun expr_true() {
        val rrb = this.S()
        val goal = "expr"
        val sentence = "true"

        val expected = """
              expr { root|1 {
                bool { 'true' }
              } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun S_true() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "true"

        val expected = """
            S {
              expr { root|1 {
                bool { 'true' }
              } }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun S_var() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "var"

        val expected = """
            S {
              expr { root {
                var { "[a-zA-Z]+" : 'var' }
              } }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun S_group_a() {
        val rrb = this.S()
        val goal = "S"
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

        super.testStringResult(rrb, goal, sentence, expected)

    }

    @Test
    fun a_div_b() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a / b"

        val expected = """
            S {
              expr|2 {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op { '/' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)

    }

    @Test
    fun a_mul_b() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a * b"

        val expected = """
            S {
              expr|2 {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op|1 { '*' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)

    }

    @Test
    fun a_add_b() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a + b"

        val expected = """
            S {
              expr {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op { '+' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)

    }

    @Test
    fun a_sub_b() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a - b"

        val expected = """
            S {
              expr {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op { '-' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)

    }

    @Test
    fun a_add_b_mul_c() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a+b*c"

        val expected = """
         S { expr|2 { infix {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              op|1 { '*' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a_mul_b_add_c() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a*b+c"

        val expected = """
         S { expr|2 { infix {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              op|1 { '*' }
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a_add_b_add_c_add_d() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a+b+c+d"

        val expected = """
         S { expr|2 { infix {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'd' } } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a_add_b_add_c_add_d_add_e_add_f() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a+b+c+d+e+f"

        val expected = """
         S { expr { infix {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'd' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'e' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'f' } } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun a_add_b_add_c_mul_d_add_e_add_f() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a+b+c*d+e+f"

        val expected = """
         S { expr|2 { infix {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
              op|1 { '*' }
              expr { root { var { "[a-zA-Z]+" : 'd' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'e' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'f' } } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun Og_a_add_b_Cg_mul_c() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "(a+b)*c"

        val expected = """
         S { expr|2 { infix {
              expr|1 { group {
                  '('
                  expr|2 { infix {
                      expr { root { var { "[a-zA-Z]+" : 'a' } } }
                      op|2 { '+' }
                      expr { root { var { "[a-zA-Z]+" : 'b' } } }
                    } }
                  ')'
                } }
              op|1 { '*' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
            } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

}