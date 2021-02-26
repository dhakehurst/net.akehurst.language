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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

class test_expessions_bodmas2_Longest : test_ScanOnDemandParserAbstract() {

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
    companion object {
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
            multi("I1",1, -1, "I2")
            choice("op", RuntimeRuleChoiceKind.LONGEST_PRIORITY){
                literal("/")
                literal("*")
                literal("+")
                literal("-")
            }
        }
    }
/*
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_var = b.rule("var").concatenation(b.pattern("[a-z]+"))
        val r_op = b.rule("op").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,b.literal("/"), b.literal("*"),b.literal("+"),b.literal("-"))
        val r_I = b.rule("I").build()
        val r_par = b.rule("par").build()
        val r_E = b.rule("E").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_var, r_I, r_par)
        b.rule(r_par).concatenation(b.literal("("), r_E, b.literal(")"))
        val r_I2 = b.rule("I2").concatenation(r_op, r_E)
        val r_I1 = b.rule("I1").multi(1,-1,r_I2)
        b.rule(r_I).concatenation(r_E, r_I1)
        val r_S = b.rule("S").concatenation(r_E)
        return b
    }
*/
    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { E { var { "[a-z]+":'a' } } }
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

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
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

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

}