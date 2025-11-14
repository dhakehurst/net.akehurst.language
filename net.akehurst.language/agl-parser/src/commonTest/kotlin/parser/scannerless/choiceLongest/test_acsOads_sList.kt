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
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_acsOads_sList : test_LeftCornerParserAbstract() {

    // S = acs | ads
    // acs = [a / 'c' ]+
    // ads = [a / 'd' ]+
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("acs")
                ref("ads")
            }
            sList("acs", 1, -1, "'a'", "'c'")
            sList("ads", 1, -1, "'a'", "'d'")
            literal("'a'", "a")
            literal("'c'", "c")
            literal("'d'", "d")
            preferenceFor("'a'") {
                leftOption(listOf("acs"), RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, setOf("<EOT>"))
                leftOption(listOf("ads"), RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, setOf("<EOT>"))
            }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun aca() {
        val sentence = "aca"

        val expected = """
            S {
                acs { 'a' 'c' 'a' }
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
    fun ada() {
        val sentence = "ada"

        val expected = """
            S {
                ads { 'a' 'd' 'a' }
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
    fun a() {
        val sentence = "a"

        val expected = """
            S {
                ads { 'a' }
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
}