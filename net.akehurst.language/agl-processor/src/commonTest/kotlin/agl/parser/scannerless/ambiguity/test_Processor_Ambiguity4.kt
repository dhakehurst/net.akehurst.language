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

package net.akehurst.language.parser.scanondemand.ambiguity

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.embedded.test_embeddedSupersetSkip
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_Processor_Ambiguity4 : test_ScanOnDemandParserAbstract() {
    /*
    S = 's' n '{' P? RList '}' ;
    P = 'p' '{' Inner::SP? '}' ;
    RList = R* ;
    R = 'r' n '{' SList '}' ;
    SList = S* ;

    leaf x = 'x'
    leaf n = "'[^']+'" ;
     */
    /* Inner
     * SP = x?
     */
    private companion object {
        val Inner = runtimeRuleSet {
            concatenation("SP") { ref("xopt") }
            multi("xopt", 0, 1, "x")
            literal("x", "x")
        }
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("s"); ref("n"); literal("{"); ref("Popt"); ref("RList"); literal("}") }
            multi("Popt", 0, 1, "P")
            concatenation("P") { literal("p"); literal("{"); ref("SPopt"); literal("}") }
            multi("SPopt", 0, 1, "eSP")
            embedded("eSP", Inner, Inner.findRuntimeRule("SP"))
            multi("RList", 0, -1, "R")
            concatenation("R") { literal("r"); ref("n"); literal("{"); ref("SList"); literal("}") }
            multi("SList", 0, -1, "S")
            pattern("n", "'[^']+'")
        }
        val goal = "S"
    }


    @Test
    fun n_empty_fails() {
        val goal = "n"
        val sentence = "''"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^''", setOf("n"))
            ), issues.error
        )
    }

    @Test
    fun n_content() {
        val goal = "n"
        val sentence = "'nn'"

        val expected5 = """
            n:'\'nn\''
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected5)
        )

    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'s'"))
            ), issues.error
        )
    }

    @Test
    fun S_no_content() {
        val sentence = "s'n'{}"

        val expected = """
            S {
              's'
              n : '\'n\''
              '{'
              Popt { §empty }
              RList { §empty }
              '}'
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
    fun S_with_empty_p() {
        val sentence = "s'n'{p{}}"

        val expected = """
            S {
              's'
              n : '\'n\''
              '{'
              Popt { P {
                'p'
                '{'
                SPopt { eSP { Inner::SP { xopt { §empty } } } }
                '}'
              } }
              RList { §empty }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = rrs,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 2,
            printAutomaton = false,
            expectedTrees = arrayOf(expected)
        )

    }

    @Test
    fun S_with_non_empty_p() {
        val sentence = "s'n'{p{x}}"

        val expected = """
            S {
              's'
              n : '\'n\''
              '{'
              Popt { P {
                'p'
                '{'
                SPopt {  eSP { Inner::SP { xopt { x:'x' } } } }
                '}'
              } }
              RList { §empty }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = rrs,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = false,
            expectedTrees = arrayOf(expected)
        )

    }

    @Test
    fun S_with_non_empty_RList() {
        val sentence = "s'n'{r'n'{}}"

        val expected = """
            S {
              's'
              n : '\'n\''
              '{'
              Popt { §empty }
              RList { R {
                'r'
                n : '\'n\''
                '{'
                SList { §empty }
                '}'
              } }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = rrs,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = false,
            expectedTrees = arrayOf(expected)
        )

    }

    @Test
    fun S_with_non_empty_RList_bad_n_fails() {
        val sentence = "s'n'{r''{}}"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(6, 7, 1, 1), "s'n'{r^''{}}", setOf("n"))
            ), issues.error
        )
    }

    @Test
    fun S_with_non_empty_p_and_non_empty_RList() {
        val sentence = "s'n'{p{}r'n'{}}"

        val expected = """
            S {
              's'
              n : '\'n\''
              '{'
              Popt { P {
                'p'
                '{'
                SPopt { eSP { Inner::SP { xopt { §empty } } } }
                '}'
              } }
              RList { R {
                'r'
                n : '\'n\''
                '{'
                SList { §empty }
                '}'
              } }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = rrs,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 2,
            printAutomaton = false,
            expectedTrees = arrayOf(expected)
        )

    }

    @Test
    fun S_with_non_empty_p_and_non_empty_RList_bad_n_fails() {
        val sentence = "s'n'{p{}r''{}}"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(9, 10, 1, 1), "s'n'{p{}r^''{}}", setOf("n"))
            ), issues.error
        )

    }
}