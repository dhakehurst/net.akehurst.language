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

package net.akehurst.language.parser.leftcorner.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_TELE_VC : test_LeftCornerParserAbstract() {

    // S = E ;
    // E = V | C             // expr = var | conditional
    // C = t E | t E s E     // conditional = ifThenExpr | ifThenExprElseExpr
    // V = v
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("V")
                ref("C")
            }
            choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("t"); ref("E"); literal("s"); ref("E") }
                concatenation { literal("t"); ref("E") }
            }
            concatenation("V") { literal("v") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'v'","'t'"))
        ),issues.errors)
    }

    @Test
    fun tvsv() { // if then v else v
        val sentence = "tvsv"

        val expected = """
            S { E { C { 't' E { V { 'v' } } 's' E { V { 'v' } }  } } }
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
    fun tv() { // if then v
        val sentence = "tv"

        val expected = """
            S { E { C { 't' E { V { 'v' } } } } }
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
    fun tvstv() {  // if then v else if then v
        val sentence = "tvstv"

        val expected = """
            S { E { C {
              't'
              E { V { 'v' } }
              's'
              E { C {
                't'
                E { V { 'v' } }
              } }
            } } }
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
    fun ttvsv() {  // if then if then v else v
        val sentence = "ttvsv"

        val expected = """
            S { E { C {
              't'
              E { C {
                't'
                E { V { 'v' } }
                's'
                E { V { 'v' } }
              } }
            } } }
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
    fun tttvsv() {
        val sentence = "tttvsv"

        val expected = """
            S { E { C {
              't'
              E { C {
                  't'
                  E { C {
                    't'
                    E { V { 'v' } }
                    's'
                    E { V { 'v' } }
                  } }
              } }
            } } }
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
