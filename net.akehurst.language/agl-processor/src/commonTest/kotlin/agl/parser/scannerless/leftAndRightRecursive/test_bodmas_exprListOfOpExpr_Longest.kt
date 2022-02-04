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

package net.akehurst.language.parser.scanondemand.leftAndRightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_bodmas_exprListOfOpExpr_Longest : test_ScanOnDemandParserAbstract() {

    // S = E
    /* E = var | I | '(' E ')' */
    // E = var | I | par
    // par = '(' E ')'
    // E = var | I | '(' E ')'
    /* I = E (op E)+ */
    // I = E I1
    // I1 = I+
    // I2 = op E
    // op = '/' | 'M' | '+' | '-'
    // var = "[a-z]+"
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("var")
                ref("I")
                ref("par")
            }
            concatenation("var") { pattern("[a-z]+") }
            concatenation("par") { literal("("); ref("E"); literal(")") }
            concatenation("I") { ref("E"); ref("I1") }
            concatenation("I2") { ref("op"); ref("E") }
            multi("I1", 1, -1, "I2")
            choice("op", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("/")
                literal("*")
                literal("+")
                literal("-")
            }
        }
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { E { var { "[a-z]+":'a' } } }
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
    fun vav() {
        val goal = "S"
        val sentence = "v+v"

        val expected = """
         S { E|1 { I {
              E { var { "[a-z]+" : 'v' } }
              I1 { I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                } }
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
    fun vavav() {
        val goal = "S"
        val sentence = "v+v+v"

        val expected = """
         S { E|1 { I {
              E { var { "[a-z]+" : 'v' } }
              I1 {
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
              }
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
    fun vavavav() {
        val goal = "S"
        val sentence = "v+v+v+v"

        val expected = """
         S { E|1 { I {
              E { var { "[a-z]+" : 'v' } }
              I1 {
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
              }
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
    fun vavavavav() {
        val goal = "S"
        val sentence = "v+v+v+v+v"

        val expected = """
         S { E|1 { I {
              E { var { "[a-z]+" : 'v' } }
              I1 {
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
              }
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
    fun vdvmvavsv() {
        val goal = "S"
        val sentence = "v/v*v+v-v"

        val expected = """
         S { E|1 { I {
              E { var { "[a-z]+" : 'v' } }
              I1 {
                I2 {
                  op { '/' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|1 { '*' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|2 { '+' }
                  E { var { "[a-z]+" : 'v' } }
                }
                I2 {
                  op|3 { '-' }
                  E { var { "[a-z]+" : 'v' } }
                }
              }
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