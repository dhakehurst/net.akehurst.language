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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_multi_4_literal : test_LeftCornerParserAbstract() {

    // S = 'a'4
    private companion object {
        val rrs = runtimeRuleSet {
            multi("S",4,4,"'a'")
            literal("'a'","a")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^", setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"a^", setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2,3,1,1),"aa^", setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun aaa_fails() {
        val sentence = "aaa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(3,4,1,1),"aaa^", setOf("'a'"))
        ),issues.errors)
    }


    @Test
    fun aaaa() {
        val sentence = "aaaa"

        val expected = """
            S { 'a' 'a' 'a' 'a' }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aaaaa_fails() {
        val sentence = "aaaaa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(4,5,1,1),"aaaa^a", setOf("<EOT>"))
        ),issues.errors)
    }
}