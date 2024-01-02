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
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_arglist : test_LeftCornerParserAbstract() {

    // S = 'a' ( 'c' 'a' )* ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("tail") }
            multi("tail", 0, -1, "ca")
            concatenation("ca") { literal("c"); literal("a"); }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { 'a' tail { §empty } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "a^a", setOf("<EOT>", "'c'"))
            ), issues.errors
        )
    }

    @Test
    fun aca() {
        val sentence = "aca"

        val expected = """
            S { 'a' tail { ca { 'c' 'a' } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }


    @Test
    fun acacacaca() {
        val sentence = "acacacaca"

        val expected = """
            S { 'a' tail { ca { 'c' 'a' } ca { 'c' 'a' } ca { 'c' 'a' } ca { 'c' 'a' } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a50() {
        val sentence = "a" + "ca".repeat(50)

        val expected = "S { 'a' tail { " + "ca { 'c' 'a' } ".repeat(50) + " } }"

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a500() {
        val sentence = "a" + "ca".repeat(500)

        val expected = "S { 'a' tail { " + "ca { 'c' 'a' } ".repeat(500) + " } }"

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a2000() {
        val sentence = "a" + "ca".repeat(2000)

        val expected = "S { 'a' tail { " + "ca { 'c' 'a' } ".repeat(2000) + " } }"

        super.test(rrs, goal, sentence, 1, expected)
    }
}