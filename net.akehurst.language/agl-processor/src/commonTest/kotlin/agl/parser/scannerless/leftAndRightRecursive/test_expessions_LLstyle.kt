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

internal class test_expessions_LLstyle : test_ScanOnDemandParserAbstract() {

    // S = E
    // E = P
    //   | E '+' P

    // S = E
    // E = P | E1
    // E1 = E o P
    // P = a

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("P")
                ref("E1")
            }
            concatenation("E1") { ref("E"); literal("o"); ref("P") }
            concatenation("P") { literal("a") }
        }
        const val goal = "S"
    }


    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { E { P {'a'} } }
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
    fun aoa() {
        val sentence = "aoa"

        val expected = """
            S { E|1 { E1 { E{ P {'a'} } 'o' P {'a'} } } }
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
    fun aoaoa() {
        val sentence = "aoaoa"

        val expected = """
            S { E|1 { E1 {
                E|1 { E1 {
                    E { P {'a'} }
                    'o'
                    P {'a'}
                  } }
                'o'
                P {'a'}
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
    fun aoaoaoa() {
        val sentence = "aoaoaoa"

        val expected = """
             S { E|1 { E1 {
                  E|1 { E1 {
                      E|1 { E1 {
                          E { P {'a'} }
                          'o'
                          P {'a'}
                        } }
                      'o'
                      P {'a'}
                    } }
                  'o'
                  P {'a'}
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