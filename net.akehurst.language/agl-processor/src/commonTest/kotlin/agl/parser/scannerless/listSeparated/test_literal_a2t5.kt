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

package net.akehurst.language.parser.scanondemand.listSeparated

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.group.test_group
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_literal_a2t5 : test_ScanOnDemandParserAbstract() {

    // S = [a / 'b'][2..5]
    // a = 'a'

    private companion object {
        val rrs = runtimeRuleSet {
            sList("S",2,5,"'a'","'b'")
            literal("'a'","a")
            literal("'b'","b")
        }

        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test(rrs, goal, inputText,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun a_fails() {
        val inputText = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            test(rrs, goal, inputText,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
        assertEquals(setOf("'b'"), e.expected)
    }

    @Test
    fun ab_fails() {
        val inputText = "ab"

        val e = assertFailsWith(ParseFailedException::class) {
            test(rrs, goal, inputText,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(3, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun aba() {
        val sentence = "aba"

        val expected = "S {'a' 'b' 'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ababa() {
        val sentence = "ababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun abababa() {
        val sentence = "abababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a' 'b' 'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ababababa() {
        val sentence = "ababababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a' 'b' 'a' 'b' 'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun literal_ab25__r__a6_fails() {
        val inputText = "abababababa"

        val e = assertFailsWith(ParseFailedException::class) {
            test(rrs, goal, inputText,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(10, e.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT_TAG), e.expected)
    }
}