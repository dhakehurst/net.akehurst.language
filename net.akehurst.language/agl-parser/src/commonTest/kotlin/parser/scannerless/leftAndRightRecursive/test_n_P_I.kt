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

package net.akehurst.language.parser.leftcorner.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_n_P_I : test_LeftCornerParserAbstract() {

    // S = a | P | I         // name | propertyCall | infix
    // P = S 'p' a ;         // S '.' name
    // I = S 'o' S ;         // S '+' S

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("P")
                ref("I")
            }
            concatenation("I") { ref("S"); literal("o"); ref("S") }
            concatenation("P") { ref("S"); literal("p"); literal("a") }
        }
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S { 'a' }
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
    fun apa() {
        val goal = "S"
        val sentence = "apa"

        val expected = """
         S|1 { P {
            S { 'a' }
            'p'
            'a'
          } }
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
    fun aoa() {
        val goal = "S"
        val sentence = "aoa"

        val expected = """
             S|2 { I {
                S { 'a' }
                'o'
                S { 'a' }
              } }
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
    fun aoaoa() {
        //fail("this does not terminate!")
        val goal = "S"
        val sentence = "aoaoa"

        //think this should be excluded because of priority I < 'a'
        val expected = """
         S|2 { I {
            S|2 { I {
                S { 'a' }
                'o'
                S { 'a' }
              } }
            'o'
            S { 'a' }
          } }
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
    fun aoaoaoaoa() {
        //fail("this does not terminate!")
        val goal = "S"
        val sentence = "aoaoaoaoa"

        //think this should be excluded because of priority I < 'a'
        val expected = """
            S|2 { I {
                S|2 { I {
                    S|2 { I {
                        S|2 { I {
                            S { 'a' }
                            'o'
                            S { 'a' }
                        } }
                        'o'
                        S { 'a' }
                    } }
                    'o'
                    S { 'a' }
                } }
                'o'
                S { 'a' }
            } }
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
    fun apaoa() {
        val goal = "S"
        val sentence = "apaoa"

        val expected = """
             S|2 { I {
                S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
                'o'
                S { 'a' }
              } }
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
    fun aoapa() {
        val goal = "S"
        val sentence = "aoapa"

        val expected = """
             S|2 { I {
                S { 'a' }
                'o'
                S|1 { P {
                    S { 'a' }
                    'p'
                    'a'
                  } }
              } }
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
    fun apaoapaoapa() {
        val goal = "S"
        val sentence = "apaoapaoapa"

        val expected = """
         S|2 { I {
            S|2 { I {
                S|1 { P {
                    S { 'a' } 'p' 'a'
                  } }
                'o'
                S|1 { P {
                    S { 'a' } 'p' 'a'
                  } }
              } }
            'o'
            S|1 { P {
                S { 'a' } 'p' 'a'
              } }
          } }
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