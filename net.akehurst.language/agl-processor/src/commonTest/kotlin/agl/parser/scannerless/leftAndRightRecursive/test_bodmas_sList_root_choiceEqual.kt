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

package net.akehurst.language.parser.scanondemand.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_bodmas_sList_root_choiceEqual : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // expr = root | group | div | mul | add | sub ;
    // root = var < bool
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
            choice("expr",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("root")
                ref("group")
                ref("div")
                ref("mul")
                ref("add")
                ref("sub")
            }
            choice("root",RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
                ref("bool")
            }
            sList("div",2,-1,"expr","'/'")
            sList("mul",2,-1,"expr","'*'")
            sList("add",2,-1,"expr","'+'")
            sList("sub",2,-1,"expr","'-'")
            concatenation("group") { literal("("); ref("expr"); literal(")") }
            choice("bool",RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("true");literal("false") }
            concatenation("var") { pattern("[a-zA-Z]+") }
            literal("'/'","/")
            literal("'*'","*")
            literal("'+'","+")
            literal("'-'","-")
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
        ),issues)
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
            S {
              expr { root|1 {
                bool { 'true' }
              } }
            }
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
    fun a_add_b_add_c_add_d() {
        val sentence = "a+b+c+d"

        val expected = """
             S { expr|4 { add {
                  expr { root { var { "[a-zA-Z]+" : 'a' } } }
                  '+'
                  expr { root { var { "[a-zA-Z]+" : 'b' } } }
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
        val sentence = "a+b+c+d+e+f"

        val expected = """
         S { expr|4 { add {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
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
    fun a_add_b_add_c_mul_d_add_e_add_f() {
        val sentence = "a+b+c*d+e+f"

        val expected = """
         S { expr|4 { add {
              expr { root { var { "[a-zA-Z]+" : 'a' } } }
              '+'
              expr { root { var { "[a-zA-Z]+" : 'b' } } }
              '+'
              expr|3 { mul {
                expr { root { var { "[a-zA-Z]+" : 'c' } } }
                '*'
                expr { root { var { "[a-zA-Z]+" : 'd' } } }
              } }
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