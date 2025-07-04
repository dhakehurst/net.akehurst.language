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

package net.akehurst.language.parser.leftcorner.choiceAmbiguous

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_e_acsOads : test_LeftCornerParserAbstract() {

    // S = 'e' ambig
    // ambig = acs || ads
    // acs = 'a' | acs1
    // acs1 = acs 'c' 'a'
    // ads = 'a' | ads1
    // ads1 = acs 'd' 'a'
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("e"); ref("ambig")  }
            choice("ambig", RuntimeRuleChoiceKind.AMBIGUOUS) {
                ref("acs")
                ref("ads")
            }
            choice("acs", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("a"); ref("acs1") }
            concatenation("acs1") { ref("acs"); literal("c"); literal("a") }
            choice("ads", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("a"); ref("ads1") }
            concatenation("ads1") { ref("ads"); literal("d"); literal("a") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'e'"))
        ),issues.errors)
    }

    @Test
    fun eaca() {
        val sentence = "eaca"

        val expected = """
            S {
             'e'
             ambig {
                acs|1 {
                    acs1 {
                        acs { 'a' }
                        'c'
                        'a'
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
    fun eada() {
        val sentence = "eada"

        val expected = """
            S {
              'e'
              ambig|1 {
                ads|1 {
                    ads1 {
                        ads { 'a' }
                        'd'
                        'a'
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
    fun ea() {
        val sentence = "ea"

        val expected1 = """
            S { 'e' ambig|0 { acs { 'a' } } }
        """.trimIndent()

        val expected2 = """
            S { 'e' ambig|1 { ads { 'a' } } }
        """.trimIndent()

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected1,expected2)
        )
    }
}