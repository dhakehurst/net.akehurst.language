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

package net.akehurst.language.parser.scanondemand.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_acsOads_recursive : test_ScanOnDemandParserAbstract() {

    // S = C
    // C = acs | ads
    // acs = 'a' | acs1
    // acs1 = acs 'c' 'a'
    // ads = 'a' | ads1
    // ads1 = ads 'd' 'a'
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C") }
            choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
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

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
        ),issues)
    }

    @Test
    fun aca() {
        val sentence = "aca"

        val expected = """
            S { C {
                acs {
                    acs1 {
                        acs { 'a' }
                        'c'
                        'a'
                    }
                }
            } }
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
    fun ada() {
        val sentence = "ada"

        val expected = """
            S { C {
                ads {
                    ads1 {
                        ads { 'a' }
                        'd'
                        'a'
                    }
                }
            } }
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
    fun a() {
        val sentence = "a"

        val expected = """
            S { C { ads { 'a' } } }
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