/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_ab_cOa_bc : test_ScanOnDemandParserAbstract() {

    // S = ab_c | a_bc;
    // ab_c = ab 'c'
    // a_bc = 'a' bc
    // ab = 'a' 'b'
    // bc = 'b' 'c'
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ab_c")
                ref("a_bc")
            }
            concatenation("ab_c") { ref("ab"); literal("c") }
            concatenation("a_bc") { literal("a"); ref("bc") }
            concatenation("ab") { literal("a"); literal("b") }
            concatenation("bc") { literal("b"); literal("c") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("'a'"))
        ),issues)
    }

    @Test
    fun a_fails() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(rrs, Companion.goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"a^",setOf("'b'"))
        ),issues)
    }

    @Test
    fun b_fails() {
        val sentence = "b"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^b",setOf("'a'"))
        ),issues)
    }

    @Test
    fun c_fails() {
        val sentence = "c"

        val (sppt, issues) = super.testFail(rrs, Companion.goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^c",setOf("'a'"))
        ),issues)
    }

    @Test
    fun ab_fails() {
        val sentence = "ab"

        val (sppt, issues) = super.testFail(rrs, Companion.goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2,3,1,1),"ab^",setOf("'c'"))
        ),issues)
    }

    @Test
    fun abc() {
        val sentence = "abc"

        val expected = """
         S|1 { a_bc {
            'a'
            bc { 'b' 'c' }
          } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: might be possible to make this 1...need to combine H-to ab and a_bc
                expectedTrees = *arrayOf(expected)
        )
    }

}