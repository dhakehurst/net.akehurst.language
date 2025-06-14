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

package net.akehurst.language.parser.leftcorner.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_nested_optionals : test_LeftCornerParserAbstract() {
    /*
        Derived from:

        grammar = 'grammar' IDENTIFIER extends? '{' rules '}' ;
        extends = 'extends' [qualifiedName / ',']+ ;
        rules = rule+ ;
        rule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
        ruleTypeLabels = ...
     */
    /*
        S = 'i' 'a' Rs 'z' ;
        Rs = R+ ;
        R = Os 'i' 't' ;
        Os = Bo Co Do ;
        Bo = 'b'?
        Co = 'c'?
        Do = 'd'?
     */
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("i"); literal("a"); ref("Rs"); literal("z") }
            multi("Rs", 1, -1, "R")
            concatenation("R") { ref("Os"); literal("i"); literal("t") }
            concatenation("Os") { ref("Bo"); ref("Co"); ref("Do") }
            optional("Bo", "'b'")
            optional("Co", "'c'")
            optional("Do", "'d'")
            literal("'b'", "b")
            literal("'c'", "c")
            literal("'d'", "d")
        }
        const val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'i'"))
            ), issues.errors
        )
    }

    @Test
    fun iaitz() {
        val sentence = "iaitz"

        val expected = """
            S {
              'i'
              'a'
              Rs {
              R {
                Os {
                  Bo|1 { §empty }
                  Co|1 { §empty }
                  Do|1 { §empty }
                }
                'i'
                't'
              }
              }
              'z'
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
    fun iabitz() {
        val sentence = "iabitz"

        val expected = """
            S {
             'i'
              'a'
              Rs{
                  R {
                    Os {
                      Bo { 'b' }
                      Co|1 { §empty }
                      Do|1 { §empty }
                    }
                    'i'
                    't'
                  }
              }
              'z'
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
    fun iacitz() {
        val sentence = "iacitz"

        val expected = """
            S {
              'i'
              'a'
              Rs {
                  R {
                    Os {
                      Bo|1 { §empty }
                      Co{ 'c' }
                      Do|1 { §empty }
                    }
                    'i'
                    't'
                  }
              }
              'z'
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

    //TODO: more tests
}