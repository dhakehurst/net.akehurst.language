/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.parser.leftcorner.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_ifThenElse_NoWS : test_LeftCornerParserAbstract() {

    // invert the dangling else

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var < conditional ;
    // conditional = ifthen < ifthenelse ;
    // var = 'W' | 'X' | 'Y' | 'Z' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
                ref("conditional")
            }
            choice("conditional", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("ifthen")
                ref("ifthenelse")
            }
            concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
            concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
            choice("var", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("W")
                literal("X")
                literal("Y")
                literal("Z")
            }
            preferenceFor("expr") {
                right("ifthen", setOf("'then'"))
                right("ifthenelse", setOf("'then'","'else'"))
            }
        }
        val goal="S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),sentence, setOf("<GOAL>"), setOf("'W'","'X'","'Y'","'Z'","'if'"))
        ),issues.errors)
    }

    @Test
    fun ifthenelse() {
        val goal = "S"
        val sentence = "ifWthenXelseY"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifthenelse {
                      'if'
                      expr { var { 'W' } }
                      'then'
                      expr { var|1 { 'X' } }
                      'else'
                      expr { var|2 { 'Y' } }
                    }
                }
              }
            }
        """.trimIndent()

        //NOTE: season 35, long expression is dropped in favour of the shorter one!

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
        val goal = "S"
        val sentence = "ifWthenX"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifthen {
                      'if'
                      expr { var { 'W' } }
                      'then'
                      expr { var|1 { 'X' } }
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
    fun ifthenelseifthen() {
        val goal = "S"
        val sentence = "ifWthenXelseifYthenZ"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifthenelse {
                      'if'
                      expr { var { 'W' } }
                      'then'
                      expr { var|1 { 'X' } }
                      'else'
                      expr|1 {
                        conditional {
                            ifthen {
                              'if'
                              expr { var|2 { 'Y'} }
                              'then'
                              expr { var|3 { 'Z' } }
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
        val sentence = "ifWthenifXthenYelseZ"

        val expected = """
         S { expr { conditional { ifthen {
                'if'
                expr { var { 'W' } }
                'then'
                expr { conditional { ifthenelse {
                    'if'
                    expr { var { 'X' } }
                    'then'
                    expr { var { 'Y' } }
                    'else'
                    expr { var { 'Z' } }                    
                } } }
        } } } }
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
