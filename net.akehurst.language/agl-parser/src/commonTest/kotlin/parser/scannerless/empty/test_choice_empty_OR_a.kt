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

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_choice_empty_OR_a : test_LeftCornerParserAbstract() {

    //  S = 'a' | e
    //  e = <empty>
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("e")
            }
            concatenation("e"){ empty() }
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val expected = """
         S|1 { e { §empty } }
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
         S { 'a' }
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
    fun b_fails() {
        val sentence = "b"

        val (sppt,issues) = super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        // fails because a? matches empty and then is no longer an option when parsing with no lookahead for error situation
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'a'","<EOT>"))
        ),issues.errors)
    }

}