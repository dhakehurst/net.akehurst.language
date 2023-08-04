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
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_ifThenElse_LongestChoice : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var | ifthenelse | ifthen ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("var")
                ref("ifthenelse")
                ref("ifthen")
            }
            concatenation("var") { pattern("[a-zA-Z]+") }
            concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
            concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
        }.also {
            //it.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("\"[a-zA-Z]+\"", "'if'"))
            ), issues.errors
        )
    }

    @Test
    fun ifthenelse() {
        val sentence = "if a then b else c"

        val expected = """
            S {
              expr {
                ifthenelse {
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' WS { "\s+" : ' ' } } }
                  'else' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'c' } }
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
    fun ifthen() {
        val sentence = "if a then b"

        val expected = """
            S {
              expr|2 {
                ifthen {
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' } }
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
    fun ifthenelseifthen() {
        val sentence = "if a then b else if c then d"

        val expected = """
            S {
              expr|1 {
                ifthenelse {
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'b' WS { "\s+" : ' ' } } }
                  'else' WS { "\s+" : ' ' }
                  expr|2 {
                    ifthen {
                      'if' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'c' WS { "\s+" : ' ' } } }
                      'then' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'd' } }
                    }
                  }
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
    fun ifthenifthenelse() {
        val sentence = "if a then if b then c else d"

        val expected1 = """
            S {
              expr|2 {
                ifthen {
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr|1 {
                    ifthenelse {
                      'if' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'b' WS { "\s+" : ' ' } } }
                      'then' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'c' WS { "\s+" : ' ' } } }
                      'else' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'd' } }
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
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr {
                    ifthen {
                      'if' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'b' WS { "\s+" : ' ' } } }
                      'then' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'c' WS { "\s+" : ' ' } } }
                    }
                  }
                  'else' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'd' } }
                }
              }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected1)
        )
    }

    @Test
    fun ifthenifthenifthenelse() {
        val sentence = "if x then if a then if b then c else d"

        val expected1 = """
            S {
              expr {
                ifthen {
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'x' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr {
                      ifthen {
                          'if' WS { "\s+" : ' ' }
                          expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                          'then' WS { "\s+" : ' ' }
                          expr {
                            ifthenelse {
                              'if' WS { "\s+" : ' ' }
                              expr { var { "[a-zA-Z]+" : 'b' WS { "\s+" : ' ' } } }
                              'then' WS { "\s+" : ' ' }
                              expr { var { "[a-zA-Z]+" : 'c' WS { "\s+" : ' ' } } }
                              'else' WS { "\s+" : ' ' }
                              expr { var { "[a-zA-Z]+" : 'd' } }
                            }
                          }
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
                  'if' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'a' WS { "\s+" : ' ' } } }
                  'then' WS { "\s+" : ' ' }
                  expr {
                    ifthen {
                      'if' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'b' WS { "\s+" : ' ' } } }
                      'then' WS { "\s+" : ' ' }
                      expr { var { "[a-zA-Z]+" : 'c' WS { "\s+" : ' ' } } }
                    }
                  }
                  'else' WS { "\s+" : ' ' }
                  expr { var { "[a-zA-Z]+" : 'd' } }
                }
              }
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 4,
            expectedTrees = arrayOf(expected1)
        )
    }

}