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

internal class test_bodmas1_WS : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // expr = root | group | div | mul | add | sub ;
    // root = var < bool
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
            pattern("WS","\\s+",true)
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
            concatenation("group") { literal("("); ref("expr"); literal(")") }
            concatenation("div") { ref("expr"); literal("/"); ref("expr") }
            concatenation("mul") { ref("expr"); literal("*"); ref("expr") }
            concatenation("add") { ref("expr"); literal("+"); ref("expr") }
            concatenation("sub") { ref("expr"); literal("-"); ref("expr") }
            choice("bool",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("true")
                literal("false")
            }
            pattern("var","[a-zA-Z]+")
        }

        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("var","'true'","'false'","'('"))
        ),issues)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
              expr { root { var : 'a' } }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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

        val actual = super.test(
            rrs = rrs,
            goal = Companion.goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun S_var() {
        val sentence = "var"

        val expected = """
            S {
              expr { root {
                var : 'var'
              } }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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
                  expr { root { var : 'a' } }
                  ')'
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )

    }

    @Test
    fun a_div_b() {
        val sentence = "a / b"

        val expected = """
            S {
              expr|2 {
                div {
                  expr { root { var : 'a' WS : ' ' } }
                  '/' WS : ' '
                  expr { root { var : 'b' } }
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )

    }

    @Test
    fun a_mul_b() {
        val sentence = "a * b"

        val expected = """
            S {
              expr|3 {
                mul {
                  expr { root { var : 'a' WS : ' ' } }
                  '*' WS : ' '
                  expr { root { var : 'b' } }
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )

    }

    @Test
    fun a_add_b() {
        val sentence = "a + b"

        val expected = """
            S {
              expr|4 {
                add {
                  expr { root { var : 'a' WS : ' ' } }
                  '+' WS : ' '
                  expr { root { var : 'b' } }
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )

    }

    @Test
    fun a_sub_b() {
        val sentence = "a - b"

        val expected = """
            S {
              expr|5 {
                sub {
                  expr { root { var : 'a' WS : ' ' } }
                  '-' WS : ' '
                  expr { root { var : 'b' } }
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )

    }

    @Test
    fun a_add_b_mul_c() {
        val sentence = "a+b*c"

        val expected = """
            S {
             expr|4 {
              add {
                expr { root { var : 'a' } }
                '+'
                expr|3 {
                  mul {
                    expr { root { var : 'b' } }
                    '*'
                    expr { root { var : 'c' } }
                  }
                }
              }
             }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
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
                    expr { root { var : 'a' } }
                    '*'
                    expr { root { var : 'b' } }
                  }
                }
               '+'
               expr { root { var : 'c' } }
              }
             }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = Companion.goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a_add_b_add_c_add_d() {
        val sentence = "a+b+c+c+d"

        val expected = """
 S { expr|4 { add {
      expr|4 { add {
          expr|4 { add {
              expr|4 { add {
                  expr { root { var : 'a' } }
                  '+'
                  expr { root { var : 'b' } }
                } }
              '+'
              expr { root { var : 'c' } }
            } }
          '+'
          expr { root { var : 'c' } }
        } }
      '+'
      expr { root { var : 'd' } }
    } } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = Companion.goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a_add_b_add_c_add_d_add_e_add_f() {
        val sentence = "a+b+c+c+d+e+f"

        val expected = """
         S { expr|4 { add {
              expr|4 { add {
                  expr|4 { add {
                      expr|4 { add {
                          expr|4 { add {
                              expr|4 { add {
                                  expr { root { var : 'a' } }
                                  '+'
                                  expr { root { var : 'b' } }
                                } }
                              '+'
                              expr { root { var : 'c' } }
                            } }
                          '+'
                          expr { root { var : 'c' } }
                        } }
                      '+'
                      expr { root { var : 'd' } }
                    } }
                  '+'
                  expr { root { var : 'e' } }
                } }
              '+'
              expr { root { var : 'f' } }
            } } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = Companion.goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun Og_a_add_b_Cg_mul_c() {
        val sentence = "(a+b)*c"

        val expected = """
         S { expr|3 { mul {
              expr|1 { group {
                  '('
                  expr|4 { add {
                      expr { root { var : 'a' } }
                      '+'
                      expr { root { var : 'b' } }
                    } }
                  ')'
                } }
              '*'
              expr { root { var : 'c' } }
            } } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

}