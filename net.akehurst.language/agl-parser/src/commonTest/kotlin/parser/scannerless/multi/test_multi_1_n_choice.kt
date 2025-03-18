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

package net.akehurst.language.parser.leftcorner.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_multi_1_n_choice : test_LeftCornerParserAbstract() {

    // S = AB+
    // AB = a | b
    private companion object {
        val rrs = runtimeRuleSet {
            multi("S", 1, -1, "AB")
            choice("AB", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
            }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"), setOf("'a'","'b'"))
        ),issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { AB {'a'} }
        """.trimIndent()

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun b() {
        val sentence = "b"

        val expected = """
            S { AB|1 {'b'} }
        """.trimIndent()

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aa() {
        val sentence = "aa"

        val expected = """
            S { AB{'a'} AB{'a'} }
        """.trimIndent()

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S { AB{'a'} AB{'a'} AB{'a'} }
        """.trimIndent()

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a50() {
        val sentence = "a".repeat(50)

        val expected = "S { "+"AB{'a'} ".repeat(50)+" }"

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a500() {
        val sentence = "a".repeat(500)

        val expected = "S { "+"AB{'a'} ".repeat(500)+" }"

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun a2000() {
        val sentence = "a".repeat(2000)

        val expected = "S { "+"AB{'a'} ".repeat(2000)+" }"

        val actual = super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }
}