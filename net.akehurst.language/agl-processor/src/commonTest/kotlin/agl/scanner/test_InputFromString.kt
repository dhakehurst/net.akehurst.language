/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.scanner

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import net.akehurst.language.api.parser.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_InputFromString {

    companion object {

        fun test(sentence: String, rrs: RuntimeRuleSet, position: Int, rule: Rule, expected: CompleteTreeDataNode?) {
            val terms = rrs.terminalRules.filterNot { it.isEmptyTerminal }
            val scanners = listOf(
                InputFromString(terms.size, sentence),
                ScannerClassic(sentence, terms)
            )

            for (sc in scanners) {
                println(sc::class.simpleName)
                val actual = sc.findOrTryCreateLeaf(position, rule)
                assertEquals(expected, actual)
            }
        }

    }

    @Test
    fun construct() {
        val inputText = ""
        val sut = InputFromString(0, inputText)

        assertNotNull(sut)
    }

    @Test
    fun blank_literal_a_0_EMPTY() {
        val sentence = ""
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val position = 0
        val rule = RuntimeRuleSet.EMPTY

        val expected = CompleteTreeDataNode(RuntimeRuleSet.EMPTY, 0, 0, 0, 0)

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun blank_literal_abc_0_abc__fails() {
        val sentence = ""
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("abc") }
        }
        val position = 0
        val rule = rrs.findRuntimeRule("'abc'")

        val expected = null

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_empty() {
        val inputText = ""
        val sut = InputFromString(10, inputText)

        val rr = RuntimeRule(0, 1, "", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, ""))
        }
        val actual = sut.tryMatchText(0, rr)

        assertEquals(-1, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_abc() {
        val inputText = ""
        val sut = InputFromString(10, inputText)

        val rr = RuntimeRule(0, 1, "'abc'", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, "abc"))
        }
        val actual = sut.tryMatchText(0, rr)

        assertEquals(-1, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_a_to_c() {
        val inputText = ""
        val sut = InputFromString(10, inputText)

        val rr = RuntimeRule(0, 1, "'[a-c]'", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, "[a-c]"))
        }
        val actual = sut.tryMatchText(0, rr)

        assertEquals(-1, actual)
    }

    @Test
    fun tryMatchText_abc_at_start_pattern_abc() {
        val inputText = "abc"
        val sut = InputFromString(10, inputText)

        val rr = RuntimeRule(0, 1, "'abc'", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, "abc"))
        }
        val actual = sut.tryMatchText(0, rr)

        assertEquals(3, actual)
    }

    @Test
    fun tryMatchText_abc_at_start_pattern_a_to_c() {
        val inputText = "abc"
        val sut = InputFromString(10, inputText)

        val rr = RuntimeRule(0, 1, "'[a-c]'", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, "[a-c]"))
        }
        val actual = sut.tryMatchText(0, rr)

        assertEquals(1, actual)
    }

    @Test
    fun tryMatchText_abc_at_1_pattern_a_to_c() {
        val inputText = "abc"
        val sut = InputFromString(10, inputText)

        val rr = RuntimeRule(0, 1, "'[a-c]'", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, "[a-c]"))
        }
        val actual = sut.tryMatchText(1, rr)

        assertEquals(1, actual)
    }
    //TODO:....tryMatchText

    @Test
    fun locationFor_singleLine() {
        val inputText = "abc"
        val sut = InputFromString(10, inputText)

        for (p in inputText.indices) {
            val actual = sut.locationFor(p, 1)
            val col = p + 1
            val line = 1
            val expected = InputLocation(p, col, line, 1)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun locationFor_muiltLine() {
        val inputText = """
            abc
            def
            ghi
        """.trimIndent()
        val sut = InputFromString(10, inputText)

        for (p in inputText.indices) {
            val actual = sut.locationFor(p, 1)
            val col = (p % 4) + 1
            val line = (p / 4) + 1
            val expected = InputLocation(p, col, line, 1)
            assertEquals(expected, actual)
        }

    }
}