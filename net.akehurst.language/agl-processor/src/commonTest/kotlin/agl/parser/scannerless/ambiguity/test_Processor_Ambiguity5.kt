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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_Processor_Ambiguity5 : test_ScanOnDemandParserAbstract() {
    /*
     * S = 'a' B 'd'
     * B = 'b' | "[b-c]"
     */
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("B"); literal("d") }
            choice("B",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                pattern("[b-c]")
            }

        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun acd() {
        val sentence = "acd"

        val expected = """
            S {
              'a'
              B { "[b-c]" : 'c' }
              'd'
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
    fun abd() {
        val sentence = "abd"

        val expected = """
            S {
              'a'
              B { "[b-c]" : 'b' }
              'd'
            }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 2,
            expectedTrees = arrayOf(expected)
        )
    }
}