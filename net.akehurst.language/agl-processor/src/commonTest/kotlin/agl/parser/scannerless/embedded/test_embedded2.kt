/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand.embedded

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_embedded2 : test_ScanOnDemandParserAbstract() {

    private companion object {

        val Inner = runtimeRuleSet {
            pattern("WS","\\s+",true)
            pattern("COMMENT","/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", true)
            concatenation("TR") { ref("optST"); ref("optRE"); ref("optTP")  }
            multi("optST",0,1,"ST")
            concatenation("ST") { ref("ID") }
            multi("optRE",0,1,"RE")
            concatenation("RE") { literal("/"); ref("EX") }
            multi("optTP",0,1,"TP")
            concatenation("TP") { literal("#"); ref("ID") }

            choice("EX", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ASS")
                ref("ID")
            }
            concatenation("ASS") { ref("ID"); literal("="); ref("ID")  }

            pattern("ID","[a-zA-Z]+")
        }

        // S = 's' '{' I '}' ;
        // I = Inner::TR ;
        val S = runtimeRuleSet {
            pattern("WS","\\s+",true)
            concatenation("S") { literal("s"); literal("{"); ref("I"); literal("}"); }
            embedded("I", Inner, Inner.findRuntimeRule("TR"))
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'s'"))
            ), issues.error)
    }

    @Test
    fun d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(S, Companion.goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^d", setOf("'s'"))
            ), issues.error)
    }

    @Test
    fun s_fails() {
        val sentence = "s"

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "s^", setOf("'{'"))
            ), issues.error)
    }

    @Test
    fun so_fails() {
        val sentence = "s{"

        val (sppt, issues) = super.testFail(S, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1), "s{^", setOf("ID","'/'","'#'","'}'"))
            ), issues.error)
    }

    @Test
    fun soc() {
        val goal = "S"
        val sentence = "s{}"

        val expected = """
            S {
              's' '{'
              I { Inner::TR {
                optST{ §empty }
                optRE{ §empty }
                optTP{ §empty }
              } }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun so_c() {
        val goal = "S"
        val sentence = "s{ }"

        val expected = """
            S {
              's' '{' WS : ' '
              I { Inner::TR {
                optST{ §empty }
                optRE{ §empty }
                optTP{ §empty }
              } }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun soac() {
        val goal = "S"
        val sentence = "s{a}"

        val expected = """
            S {
              's' '{'
              I { Inner::TR {
                optST{ ST { ID : 'a' } }
                optRE{ §empty }
                optTP{ §empty }
              } }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun soMc() {
        val goal = "S"
        val sentence = "s{/*xx*/}"

        val expected = """
            S {
              's' '{'
              I { Inner::TR {
                COMMENT:'/*xx*/'
                optST{ §empty }
                optRE{ §empty }
                optTP{ §empty }
              } }
              '}'
            }
        """.trimIndent()

        super.test2(
            rrs = S,
            embeddedRuntimeRuleSets = mapOf(
                "Inner" to Inner
            ),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }
}