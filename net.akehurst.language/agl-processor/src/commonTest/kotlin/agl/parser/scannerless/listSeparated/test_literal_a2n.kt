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
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

internal class test_literal_a2n : test_ScanOnDemandParserAbstract() {

    // S = [a / ',']2+
    // a = 'a'
    private companion object {
        val rrs = runtimeRuleSet {

        }
        val goal = "S"
    }
    private fun literal_a0n(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r0 = b.literal("a")
        val r1 = b.rule("S").separatedList(2, -1, b.literal(","), r0)
        return b
    }

    @Test
    fun empty() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, Companion.goal, sentence,1)
        assertNotNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"",arrayOf("?"))
        ),issues)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNotNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"",arrayOf("?"))
        ),issues)
    }

    @Test
    fun ac_fails() {
        val sentence = "a,"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNotNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"",arrayOf("?"))
        ),issues)
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNotNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"",arrayOf("?"))
        ),issues)
    }

    @Test
    fun aca() {
        val sentence = "a,a"

        val expected = "S {'a' ',' 'a'}"

        super.test(rrs, Companion.goal, sentence,1,expected)
    }

    @Test
    fun acaa_fails() {
        val sentence = "a,aa"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNotNull(sppt)
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"",arrayOf("?"))
        ),issues)
    }

    @Test
    fun acaca() {
        val sentence = "a,a,a"

        val expected = "S {'a' ',' 'a' ',' 'a'}"

        super.test(rrs, Companion.goal, sentence,1,expected)
    }

    @Test
    fun acax100() {
        val sentence = "a"+",a".repeat(99)

        val expected = "S {'a'"+" ',' 'a'".repeat(99)+"}"

        super.test(rrs, Companion.goal, sentence,1,expected)
    }

}