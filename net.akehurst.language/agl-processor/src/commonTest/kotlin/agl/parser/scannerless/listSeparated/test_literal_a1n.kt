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
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_literal_a1n : test_ScanOnDemandParserAbstract() {

    // S = [a / ',']+
    // a = 'a'

    private companion object {
        val rrs = runtimeRuleSet {
            sList("S",1,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence, 1)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = "S {'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
        assertEquals(setOf("','", RuntimeRuleSet.END_OF_TEXT_TAG), e.expected)
    }

    @Test
    fun aca() {
        val sentence = "a,a"

        val expected = "S {'a' ',' 'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = Companion.goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun acaa_fails() {
        val sentence = "a,aa"

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }

        assertEquals(1, e.location.line)
        assertEquals(4, e.location.column)
        assertEquals(setOf("','", RuntimeRuleSet.END_OF_TEXT_TAG), e.expected)
    }

    @Test
    fun acaca() {
        val sentence = "a,a,a"

        val expected = "S {'a' ',' 'a' ',' 'a'}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun acax100() {
        val sentence = "a"+",a".repeat(99)

        val expected = "S {'a'"+" ',' 'a'".repeat(99)+"}"

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

}