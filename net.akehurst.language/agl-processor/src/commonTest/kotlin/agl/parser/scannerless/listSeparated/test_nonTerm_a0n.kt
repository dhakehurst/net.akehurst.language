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
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_nonTerm_a0n : test_LeftCornerParserAbstract() {

    // S = [a / ',' ]*
    // a = 'a'
    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "a", "','")
            concatenation("a") { literal("a") }
            literal("','", ",")
        }
    }

    @Test
    fun empty() {
        val goal = "S"
        val sentence = ""

        val expected = "S|1 { §empty }"

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = "S { a { 'a' } }"

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aa_fails() {
        val goal = "S"
        val sentence = "aa"

        val (sppt,issues) = super.testFail(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1
        )
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE, InputLocation(1,2,1,1),"a^a", setOf("<EOT>","','"))
        ),issues.errors)
    }

    @Test
    fun aca() {
        val goal = "S"
        val sentence = "a,a"

        val expected = "S {a{'a'} ',' a{'a'}}"

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acaa_fails() {
        val goal = "S"
        val sentence = "a,aa"

        val (sppt,issues) = super.testFail(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1
        )
        assertEquals(listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE, InputLocation(3,4,1,1),"a,a^a", setOf("<EOT>","','"))
        ),issues.errors)
    }

    @Test
    fun acaca() {
        val goal = "S"
        val sentence = "a,a,a"

        val expected = "S {a{'a'} ',' a{'a'} ',' a{'a'}}"

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acax100() {
        val goal = "S"
        val sentence = "a"+",a".repeat(99)

        val expected = "S {a{'a'}"+" ',' a{'a'}".repeat(99)+"}"

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

}