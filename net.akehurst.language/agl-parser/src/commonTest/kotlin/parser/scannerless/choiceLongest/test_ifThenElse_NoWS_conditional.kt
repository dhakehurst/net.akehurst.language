/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

class test_ifThenElse_NoWS_conditional : test_LeftCornerParserAbstract() {

    // S =  expr ;
    // expr = var | conditional ;
    // conditional = ifthenelse | ifthen;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // var = W | X | Y | Z ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("VAR")
                ref("conditional")
            }
            choice("conditional", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ifthen")
                ref("ifthenelse")
            }
            concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
            concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
            pattern("VAR","U|V|W|X|Y|Z")
            preferenceFor("expr") {
                right(listOf("ifthen"), setOf("'then'"))
                right(listOf("ifthenelse"), setOf("'then'","'else'"))
            }
        }.also {
            //it.buildFor("S",AutomatonKind.LOOKAHEAD_1)
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),sentence, setOf("<GOAL>"),setOf("VAR","'if'"))
        ),issues.errors)
    }

    @Test
    fun ifthenelse() {
        val sentence = "ifWthenXelseY"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifthenelse {
                      'if'
                      expr { VAR:'W' }
                      'then'
                      expr { VAR:'X' }
                      'else'
                      expr { VAR:'Y' }
                    }
                }
              }
            }
        """.trimIndent()

        //NOTE: season 35, long expression is dropped in favour of the shorter one!

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ifthen() {
        val sentence = "ifWthenX"

        val expected = """
            S {
              expr {
                conditional {
                    ifthen {
                      'if'
                      expr { VAR:'W' }
                      'then'
                      expr { VAR:'X' }
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
    fun ifthenelseifthen() {
        val sentence = "ifWthenXelseifYthenZ"

        val expected = """
            S {
              expr {
                conditional {
                    ifthenelse {
                      'if'
                      expr { VAR:'W' }
                      'then'
                      expr { VAR:'X' }
                      'else'
                      expr {
                        conditional {
                            ifthen {
                              'if'
                              expr { VAR:'Y' }
                              'then'
                              expr { VAR:'Z' }
                            }
                        }
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
    fun ifthenifthenelse() {
        val sentence = "ifWthenifXthenYelseZ"

        val expected = """
            S {
              expr {
                conditional {
                    ifthen {
                      'if'
                      expr { VAR:'W' }
                      'then'
                      expr {
                        conditional {
                            ifthenelse {
                              'if'
                              expr { VAR:'X' }
                              'then'
                              expr { VAR:'Y' }
                              'else'
                              expr { VAR:'Z' }
                            }
                        }
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
    fun ifthenifthenifthenelse() {
        val sentence = "ifUthenifWthenifXthenYelseZ"

        val expected = """
            S {
              expr {
                conditional {
                  ifthen {
                    'if'
                    expr { VAR:'U' }
                    'then'
                    expr {
                      conditional {
                        ifthen {
                          'if'
                          expr { VAR:'W' }
                          'then'
                          expr {
                            conditional {
                                ifthenelse {
                                  'if'
                                  expr { VAR:'X' }
                                  'then'
                                  expr { VAR:'Y' }
                                  'else'
                                  expr { VAR:'Z' }
                                }
                            }
                          }
                        }
                      }
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

}
