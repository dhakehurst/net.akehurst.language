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

package net.akehurst.language.parser.leftcorner.leftAndRightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_bodmas_infix_sList_op_root_choiceEqual_WS : test_LeftCornerParserAbstract() {

    // S =  expr ;
    // expr = root | group | infix ;
    // root = var < bool
    // infix = [ expr / op ]2+ ;
    // op = '/' | '*' | '+' | '-' ;
    // group = '(' expr ')' ;
    // bool = 'true' | 'false' ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("root")
                ref("group")
                ref("infix")
            }
            choice("root", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
                ref("bool")
            }
            sList("infix", 2, -1, "expr", "op")
            choice("op", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("/");literal("*");literal("+");literal("-") }
            concatenation("group") { literal("("); ref("expr"); literal(")") }
            choice("bool",RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("true");literal("false") }
            concatenation("var") { pattern("[a-zA-Z]+") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("\"[a-zA-Z]+\"","'true'","'false'","'('"))
        ),issues.errors)
    }


    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
              expr { root{ var { "[a-zA-Z]+" : 'a' } } }
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

    @Test
    fun expr_true() {
        val sentence = "true"

        val expected = """
              expr { root|1 {
                bool { 'true' }
              } }
        """.trimIndent()

        super.test(
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
              expr { root|1 {
                bool { 'true' }
              } }
            }
        """.trimIndent()

        super.test(
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
              expr { root {
                var { "[a-zA-Z]+" : 'var' }
              } }
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

        super.test(
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
              expr|2 {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op { '/' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
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

    @Test
    fun a_mul_b() {
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

        super.test(
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
              expr|2 {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op|2 { '+' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
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

    @Test
    fun a_sub_b() {
        val sentence = "a - b"

        val expected = """
            S {
              expr|2 {
                infix {
                  expr { root { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } } }
                  op|3 { '-' WS { "\s+" : ' ' } }
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
                }
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

    @Test
    fun a_add_b_mul_c() {
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

        super.test(
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
         S { expr|2 { infix {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              op|1 { '*' }
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              op|2 { '+' }
              expr { root { var { "[a-zA-Z]+" : 'c' } } }
            } } }
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
    fun a_add_b_add_c_add_d() {
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

        super.test(
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
         S { expr|2 { infix {
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

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_add_b_add_c_mul_d_add_e_add_f() {
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

        super.test(
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

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

}