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

package net.akehurst.language.parser.scanondemand.choiceAmbiguous

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_a1bOa2 : test_ScanOnDemandParserAbstract() {

    private companion object {
        // S = S1 < a
        // S1 = a b?
        val deterministic = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("S1")
                literal("a")
            }
            concatenation("S1") { literal("a"); ref("bOpt") }
            optional("bOpt", "'b'")
            literal("'b'", "b")
            preferenceFor("'a'") {
                left("S1", setOf("<EOT>"))
                leftOption("S", 1, setOf("<EOT>"))
            }
        }

        // S = S1 || a
        // S1 = a b?
        private val ambiguous = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
                ref("S1")
                literal("a")
            }
            concatenation("S1") { literal("a"); ref("bOpt") }
            optional("bOpt", "'b'")
            literal("'b'", "b")
        }

        const val goal = "S"
    }

    @Test
    fun deterministic_empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(deterministic, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun deterministic_a() {
        val sentence = "a"

        val expected = """
            S {
              'a'
            }
        """.trimIndent()

        super.test(
            rrs = deterministic,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )

    }

    @Test
    fun deterministic_ab() {
        val sentence = "ab"

        val expected = """
            S { S1 {
              'a'
              bOpt { 'b' }
            } }
        """.trimIndent()

        super.test(
            rrs = deterministic,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )

    }

    //TODO: more tests

    /////////////////////
    @Test
    fun ambiguous_empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(ambiguous, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun ambiguous_a() {
        val sentence = "a"

        val expected1 = """
            S|1 {
              'a'
            }
        """.trimIndent()

        val expected2 = """
         S|0 { S1 {
            'a'
            bOpt { §empty }
          } }
        """.trimIndent()

        super.test(
            rrs = ambiguous,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected2, expected1)
        )

    }

    @Test
    fun ambiguous_ab() {
        val sentence = "ab"

        val expected = """
            S { S1 {
              'a'
              bOpt { 'b' }
            } }
        """.trimIndent()

        super.test(
            rrs = ambiguous,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )

    }

}