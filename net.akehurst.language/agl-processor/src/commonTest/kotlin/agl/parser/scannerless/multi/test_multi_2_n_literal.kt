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

package net.akehurst.language.parser.scanondemand.multi

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_multi_2_n_literal : test_ScanOnDemandParserAbstract() {

    // S = 'a'2+
    private companion object {
        val rrs = runtimeRuleSet {
            multi("S",2,-1,"'a'")
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
        ),issues.error)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"a^", setOf("'a'"))
        ),issues.error)
    }

    @Test
    fun aa() {
        val sentence = "aa"

        val expected = """
            S { 'a' 'a' }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S { 'a' 'a' 'a' }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
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
    fun a50() {
        val sentence = "a".repeat(50)

        val expected = "S { "+"'a' ".repeat(50)+" }"

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a500() {
        val sentence = "a".repeat(500)

        val expected = "S { "+"'a' ".repeat(500)+" }"

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a2000() {
        val sentence = "a".repeat(2000)

        val expected = "S { "+"'a' ".repeat(2000)+" }"

        super.test(rrs, goal, sentence, 1, expected)
    }
}