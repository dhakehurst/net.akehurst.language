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
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_literal_a1n : test_LeftCornerParserAbstract() {

    // S = ['a' / ',']+

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

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
        ),issues.errors)
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
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun ac_fails() {
        val sentence = "a,"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2,3,1,1),"a,^",setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"a^a",setOf("','","<EOT>"))
        ),issues.errors)
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
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acaa_fails() {
        val sentence = "a,aa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(3,4,1,1),"a,a^a",setOf("','","<EOT>"))
        ),issues.errors)
    }

    @Test
    fun acac_fails() {
        val sentence = "a,a,"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        println(rrs.usedAutomatonToString(goal))
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(4,5,1,1),"a,a,^",setOf("'a'"))
        ),issues.errors)
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
                expectedTrees = arrayOf(expected)
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
                expectedTrees = arrayOf(expected)
        )
    }

}