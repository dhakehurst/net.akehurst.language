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

package net.akehurst.language.parser.leftcorner.midRecursion

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_b_aSc : test_LeftCornerParserAbstract() {

    /*
        S = b | a S c ;

        S = b | S1
        S1 = a S c
     */
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S"); literal("c") }
        }
    }

    @Test
    fun b() {
        val sentence = "b"
        val goal = "S"

        val expected = """
         S { 'b' }
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
    fun abc() {
        val sentence = "abc"
        val goal = "S"

        val expected = """
         S|1 { S1 { 'a' S { 'b' } 'c' } }
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
    fun aabcc() {
        val sentence = "aabcc"
        val goal = "S"

        val expected = """
         S|1 { S1 { 'a' S|1 { S1 { 'a' S { 'b' } 'c' } } 'c' } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            printAutomaton = true,
            expectedTrees = arrayOf(expected)
        )
    }

}