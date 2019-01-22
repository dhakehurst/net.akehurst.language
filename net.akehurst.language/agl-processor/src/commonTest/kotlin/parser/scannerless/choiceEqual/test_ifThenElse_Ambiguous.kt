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

package net.akehurst.language.parser.scannerless.choiceEqual

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_ifThenElse_Ambiguous : test_ScannerlessParserAbstract() {

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var | ifthenelse | ifthen ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_expr = b.rule("expr").build()
        val r_if = b.literal("if")
        val r_then = b.literal("then")
        val r_else = b.literal("else")
        val r_var = b.rule("var").concatenation(b.pattern("[a-zA-Z]+"))
        val r_ifthen = b.rule("ifthen").concatenation(r_if,r_expr,r_then,r_expr)
        val r_ifthenelse = b.rule("ifthenelse").concatenation(r_if,r_expr,r_then,r_expr,r_else,r_expr)
        r_expr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL, -1, 0, arrayOf(r_var, r_ifthen, r_ifthenelse))
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
        assertEquals(0, ex.location.column)
    }

    @Test
    fun ifthenelse() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "if a then b else c"

        val expected = """
            S {
              expr {
                ifthenelse {
                  'if' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'a' WS { '\s+' : ' ' } } }
                  'then' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'b' WS { '\s+' : ' ' } } }
                  'else' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'c' } }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthen() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "if a then b"

        val expected = """
            S {
              expr {
                ifthen {
                  'if' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'a' WS { '\s+' : ' ' } } }
                  'then' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'b' } }
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
              expr {
                ifthenelse {
                  'if' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'a' WS { '\s+' : ' ' } } }
                  'then' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'b' WS { '\s+' : ' ' } } }
                  'else' WS { '\s+' : ' ' }
                  expr {
                    ifthen {
                      'if' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'c' WS { '\s+' : ' ' } } }
                      'then' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'd' } }
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
              expr {
                ifthen {
                  'if' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'a' WS { '\s+' : ' ' } } }
                  'then' WS { '\s+' : ' ' }
                  expr {
                    ifthenelse {
                      'if' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'b' WS { '\s+' : ' ' } } }
                      'then' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'c' WS { '\s+' : ' ' } } }
                      'else' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'd' } }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val expected2 = """
            S {
              expr {
                ifthenelse {
                  'if' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'a' WS { '\s+' : ' ' } } }
                  'then' WS { '\s+' : ' ' }
                  expr {
                    ifthen {
                      'if' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'b' WS { '\s+' : ' ' } } }
                      'then' WS { '\s+' : ' ' }
                      expr { var { '[a-zA-Z]+' : 'c' WS { '\s+' : ' ' } } }
                    }
                  }
                  'else' WS { '\s+' : ' ' }
                  expr { var { '[a-zA-Z]+' : 'd' } }
                }
              }
            }
        """.trimIndent()

        //super.test(rrb, goal, sentence, expected1, expected2)
        super.testStringResult(rrb, goal, sentence, expected1, expected2)
    }


}