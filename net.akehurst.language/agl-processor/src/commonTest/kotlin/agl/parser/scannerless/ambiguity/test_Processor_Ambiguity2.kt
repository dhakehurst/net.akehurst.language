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

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_Processor_Ambiguity2 : test_ScanOnDemandParserAbstract() {
    /**
     * S = 'a' | S S ;
     */
    /**
     * S = 'a' | S1 ;
     * S1 = S S ;
     */
    private companion object {
        val S = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); ref("S") }
        }
        val goal = "S"
    }


    @Test
    fun S_S_empty() {
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(S, goal, sentence, 1)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun S_S_a() {
        val sentence = "a"

        val expected = """
            S { 'a' }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun S_S_aa() {
        val sentence = "aa"

        val expected = """
            S|1 {
              S1 {
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun S_S_aaa() {
        val sentence = "aaa"

        val expected = """
            S|1 {
              S1 {
                S|1 {
                  S1 {
                    S { 'a' }
                    S { 'a' }
                  }
                }
                S { 'a' }
              }
            }
        """.trimIndent()

        val actual = super.test(
            rrs = S,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }


}