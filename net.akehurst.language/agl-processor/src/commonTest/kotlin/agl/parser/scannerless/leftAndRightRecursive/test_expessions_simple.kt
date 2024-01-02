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
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_expessions_simple : test_LeftCornerParserAbstract() {

    // S = E
    // E = 'a' | I
    // I = E 'o' E ;

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("I")
            }
            concatenation("I") { ref("E"); literal("o"); ref("E")  }
            preferenceFor("E") {
                left("I",setOf("'o'"))
            }
        }
        val goal = "S"
    }


    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { E {'a'} }
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
            S { E { I { E{'a'} 'o' E{'a'} } }}
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
            S { E { I {
                E { I {
                    E { 'a' }
                    'o'
                    E { 'a' }
                  } }
                'o'
                E { 'a' }
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
             S { E { I {
                  E { I {
                      E { I {
                          E { 'a' }
                          'o'
                          E { 'a' }
                        } }
                      'o'
                      E { 'a' }
                    } }
                  'o'
                  E { 'a' }
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