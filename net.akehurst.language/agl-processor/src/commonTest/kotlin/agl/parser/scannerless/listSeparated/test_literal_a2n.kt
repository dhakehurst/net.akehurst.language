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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_literal_a2n : test_ScanOnDemandParserAbstract() {

    // S = [a / ',']2+
    // a = 'a'
    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 2, -1, "a", "','")
            concatenation("a") { literal("a") }
            literal("','",",")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, Companion.goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
            ), issues
        )
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1,2,1,1),"a^",setOf("','"))
            ), issues
        )
    }

    @Test
    fun ac_fails() {
        val sentence = "a,"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2,3,1,1),"a,^",setOf("'a'"))
            ), issues
        )
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1,2,1,1),"a^a",setOf("','"))
            ), issues
        )
    }

    @Test
    fun aca() {
        val sentence = "a,a"

        val expected = "S { a{'a'} ',' a{'a'}}"

        super.test(rrs, Companion.goal, sentence, 1, expected)
    }

    @Test
    fun acaa_fails() {
        val sentence = "a,aa"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(3,4,1,1),"a,a^a",setOf("','","<EOT>"))
            ), issues
        )
    }

    @Test
    fun acaca() {
        val sentence = "a,a,a"

        val expected = "S {a{'a'} ',' a{'a'} ',' a{'a'}}"

        super.test(rrs, Companion.goal, sentence, 1, expected)
    }

    @Test
    fun acax100() {
        val sentence = "a" + ",a".repeat(99)

        val expected = "S {'a'" + " ',' 'a'".repeat(99) + "}"

        super.test(rrs, Companion.goal, sentence, 1, expected)
    }

}