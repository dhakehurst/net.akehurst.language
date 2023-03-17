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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_ifThenElse_Priority : test_ScanOnDemandParserAbstract() {

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var < conditional ;
    // conditional = ifthen < ifthenelse ;
    // var = "[a-zA-Z]+" ;
    // WS = "\s+" ;

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS","\\s+",true)
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("var")
                ref("conditional")
            }
            choice("conditional", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("ifThen")
                ref("ifThenElse")
            }
            concatenation("ifThen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
            concatenation("ifThenElse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
            pattern("var", "[a-zA-Z]+")
        }
    }

    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"^", setOf("var","'if'"))
        ),issues)
    }

    @Test
    fun ifThenElse() {
        val goal = "S"
        val sentence = "if a then b else c"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifThenElse {
                      'if' WS : ' ' 
                      expr { var : 'a' WS  : ' '  }
                      'then' WS  : ' ' 
                      expr { var : 'b' WS  : ' '  } 
                      'else' WS  : ' ' 
                      expr { var : 'c' }
                    }
                }
              }
            }
        """.trimIndent()

        //NOTE: season 35, long expression is dropped in favour of the shorter one!
        //TODO: if we can implement better combination of states, then this should need only 1 head
        // currently the "if var then" bit ends up on diff states

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ifThen() {
        val goal = "S"
        val sentence = "if a then b"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifThen {
                      'if' WS : ' '
                      expr { var : 'a' WS : ' ' }
                      'then' WS : ' ' 
                      expr { var : 'b' }
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
    fun ifThenElseifThen() {
        val goal = "S"
        val sentence = "if a then b else if c then d"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifThenElse {
                      'if' WS  : ' ' 
                      expr { var : 'a' WS  : ' '  }
                      'then' WS  : ' ' 
                      expr { var : 'b' WS : ' '  }
                      'else' WS : ' ' 
                      expr|1 {
                        conditional {
                            ifThen {
                              'if' WS  : ' ' 
                              expr { var : 'c' WS : ' '  }
                              'then' WS:' '
                              expr { var : 'd' }
                            }
                        }
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
        val goal = "S"
        val sentence = "if a then if b then c else d"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifThen {
                      'if' WS:' '
                      expr { var : 'a' WS:' ' }
                      'then' WS:' '
                      expr|1 {
                        conditional|1 {
                            ifThenElse {
                              'if' WS:' '
                              expr { var : 'b' WS:' ' }
                              'then' WS:' '
                              expr { var  : 'c' WS:' ' }
                              'else' WS:' '
                              expr { var : 'd' }
                            }
                        }
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
    fun ifthenifthenifthenelse() {
        val goal = "S"
        val sentence = "if x then if a then if b then c else d"

        val expected = """
            S {
              expr {
                conditional {
                  ifThen {
                    'if' WS:' '
                      expr { var : 'a' WS:' ' }
                      'then' WS:' '
                      expr {
                        conditional {
                            ifThen {
                              'if' WS:' '
                              expr { var : 'a' WS:' ' }
                              'then' WS:' '
                              expr {
                                conditional {
                                    ifThenElse {
                                      'if' WS:' '
                                      expr { var : 'b' WS:' ' }
                                      'then' WS:' '
                                      expr { var  : 'c' WS:' ' }
                                      'else' WS:' '
                                      expr { var : 'd' }
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

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

}