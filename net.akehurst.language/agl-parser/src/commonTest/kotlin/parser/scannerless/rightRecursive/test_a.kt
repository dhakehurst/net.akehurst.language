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

package net.akehurst.language.parser.leftcorner.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_a : test_LeftCornerParserAbstract() {

    // S =  'a' | S1 ;
    // S1 = 'a' S ;
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S") }
        }
    }

    @Test
    fun a() {
        val goal = "S"
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
    fun aa() {
        val goal = "S"
        val sentence = "aa"

        val expected = """
            S|1 { S1 { 'a' S { 'a' } } }
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
    fun aaa() {
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S|1 {
                S1 {
                    'a'
                    S|1 {
                        S1 {
                            'a'
                            S { 'a' }
                        }
                    }
                }
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
    fun a50() {
        val goal = "S"
        val sentence = "a".repeat(50)

        val expected = "S|1 { S1 { 'a' ".repeat(49) + "S { 'a' }" +" } }".repeat(49)

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a150() {
        val goal = "S"
        val sentence = "a".repeat(150)

        val expected = "S|1 { S1 { 'a' ".repeat(149) + "S { 'a' }" +" } }".repeat(149)

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a500() {
        val goal = "S"
        val sentence = "a".repeat(500)

        val expected = "S|1 { S1 { 'a' ".repeat(499) + "S { 'a' }" +" } }".repeat(499)

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    //@Test
    fun a2000() {
        val goal = "S"
        val sentence = "a".repeat(2000)

        val expected = "S|1 { S1 { 'a' ".repeat(1999) + "S { 'a' }" +" } }".repeat(1999)

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

}