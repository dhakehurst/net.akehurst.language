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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_ifThenElse_Priority : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var < conditional ;
    // conditional = ifthen < ifthenelse ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_expr = b.rule("expr").build()
        val r_if = b.literal("if")
        val r_then = b.literal("then")
        val r_else = b.literal("else")
        val r_var = b.rule("var").concatenation(b.pattern("[a-zA-Z]+"))
        val r_ifthen = b.rule("ifthen").concatenation(r_if, r_expr, r_then, r_expr)
        val r_ifthenelse = b.rule("ifthenelse").concatenation(r_if, r_expr, r_then, r_expr, r_else, r_expr)
        val r_conditional = b.rule("conditional").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST, r_ifthen, r_ifthenelse)
        r_expr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE, RuntimeRuleChoiceKind.LONGEST_PRIORITY, -1, 0, arrayOf(r_var, r_conditional))
        b.rule("S").concatenation(r_expr)
        b.pattern("WS", "\\s+", isSkip = true)
        return b
    }

    private val SS = runtimeRuleSet {
        skip("WS") { pattern("\\s+") }
        concatenation("S") { ref("expr") }
        choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("var"); ref("conditional") }
        choice("conditional", RuntimeRuleChoiceKind.PRIORITY_LONGEST) { ref("ifThen"); ref("ifThenElse") }
        concatenation("ifThen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
        concatenation("ifThenElse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
        pattern("var", "[a-zA-Z]+")
    }

    @Test
    fun empty_fails() {
        val rrb = this.SS
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ifthenelse() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "if a then b else c"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifthenelse {
                      'if' WS  : ' ' 
                      expr { var { "[a-zA-Z]+" : 'a' WS  : ' '  } }
                      'then' WS  : ' ' 
                      expr { var { "[a-zA-Z]+" : 'b' WS  : ' '  } }
                      'else' WS  : ' ' 
                      expr { var { "[a-zA-Z]+" : 'c' } }
                    }
                }
              }
            }
        """.trimIndent()

        //NOTE: season 35, long expression is dropped in favour of the shorter one!

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthen() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "if a then b"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifthen {
                      'if' WS : ' '
                      expr { var { "[a-zA-Z]+" : 'a' WS : ' ' } }
                      'then' WS : ' ' 
                      expr { var { "[a-zA-Z]+" : 'b' } }
                    }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthenelseifthen() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "if a then b else if c then d"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifthenelse {
                      'if' WS  : ' ' 
                      expr { var { "[a-zA-Z]+" : 'a' WS  : ' '  } }
                      'then' WS  : ' ' 
                      expr { var { "[a-zA-Z]+" : 'b' WS : ' '  } }
                      'else' WS : ' ' 
                      expr|1 {
                        conditional {
                            ifthen {
                              'if' WS  : ' ' 
                              expr { var { "[a-zA-Z]+" : 'c' WS : ' '  } }
                              'then' WS:' '
                              expr { var { "[a-zA-Z]+" : 'd' } }
                            }
                        }
                      }
                    }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthenifthenelse() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "if a then if b then c else d"

        val expected1 = """
            S {
              expr|1 {
                conditional {
                    ifthen {
                      'if' WS:' '
                      expr { var { "[a-zA-Z]+" : 'a' WS:' ' } }
                      'then' WS:' '
                      expr|1 {
                        conditional {
                            ifthenelse {
                              'if' WS:' '
                              expr { var { "[a-zA-Z]+" : 'b' WS:' ' } }
                              'then' WS:' '
                              expr { var { "[a-zA-Z]+" : 'c' WS:' ' } }
                              'else' WS:' '
                              expr { var { "[a-zA-Z]+" : 'd' } }
                            }
                        }
                      }
                    }
                }
              }
            }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected1)
        //super.testStringResult(rrb, goal, sentence, expected1, expected2)
    }


}