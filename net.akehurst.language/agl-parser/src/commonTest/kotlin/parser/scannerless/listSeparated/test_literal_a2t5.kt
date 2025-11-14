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

package net.akehurst.language.parser.leftcorner.listSeparated

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_literal_a2t5 : test_LeftCornerParserAbstract() {

    // S = [a / 'b'][2..5]
    // a = 'a'

    private companion object {
        val rrs = runtimeRuleSet {
            sList("S",2,5,"'a'","'b'")
            literal("'a'","a")
            literal("'b'","b")
        }

        const val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1, 2, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'b'"))
        ),issues.errors)
    }

    @Test
    fun ab_fails() {
        val sentence = "ab"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2, 3, 1, 1, null),sentence, setOf("S"),setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun aba() {
        val sentence = "aba"

        val expected = "S {'a' 'b' 'a'}"

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ababa() {
        val sentence = "ababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a'}"

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun abababa() {
        val sentence = "abababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a' 'b' 'a'}"

       super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ababababa() {
        val sentence = "ababababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a' 'b' 'a' 'b' 'a'}"

        super.test_pass(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a6_fails() {
        val sentence = "abababababa"

        //println(rrs.fullAutomatonToString(goal,AutomatonKind.LOOKAHEAD_1))

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(9, 10, 1, 1, null),sentence, setOf("'a'"),setOf("<EOT>"))
        ),issues.errors)
    }
}