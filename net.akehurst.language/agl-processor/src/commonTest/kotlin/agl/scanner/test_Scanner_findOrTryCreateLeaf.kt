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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.sppt.CompleteTreeDataNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_Scanner_findOrTryCreateLeaf {

    companion object {

        fun test(text: String, rrs: RuntimeRuleSet, position: Int, rule: Rule, expected: CompleteTreeDataNode?) {
            val sentence = SentenceDefault(text)
            val terms = rrs.terminals.filterNot { it.isEmptyTerminal }
            val scanners = listOf(
                ScannerOnDemand(RegexEnginePlatform, terms),
                ScannerClassic(RegexEnginePlatform, terms)
            )

            for (sc in scanners) {
                println(sc::class.simpleName)
                val actual = sc.findOrTryCreateLeaf(sentence, position, rule)
                assertEquals(expected, actual)
            }
        }

    }

    @Test
    fun construct() {
        val inputText = ""
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        assertNotNull(sut)
    }

    // sentence_rrs_position_rule[__result]

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
    fun blank_pattern_abc_0_abc__fails() {
        val sentence = ""
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("abc") }
            pattern("abc", "abc")
        }
        val position = 0
        val rule = rrs.findRuntimeRule("abc")

        val expected = null

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun blank_pattern_a2c_0_a2c__fails() {
        val sentence = ""
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("a2c") }
            pattern("a2c", "[a-c]")
        }
        val position = 0
        val rule = rrs.findRuntimeRule("a2c")

        val expected = null

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun abc_literal_abc_0_abc__ok() {
        val sentence = "abc"
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("abc") }
        }
        val position = 0
        val rule = rrs.findRuntimeRule("'abc'")

        val expected = CompleteTreeDataNode(rule, 0, 3, 3, 0)

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun abc_pattern_a2c_0_a2c__ok() {
        val sentence = "abc"
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("a2c") }
            pattern("a2c", "[a-c]")
        }
        val position = 0
        val rule = rrs.findRuntimeRule("a2c")

        val expected = CompleteTreeDataNode(rule, 0, 1, 1, 0)

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun abc_pattern_a2c_1_a2c__ok() {
        val sentence = "abc"
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("a2c") }
            pattern("a2c", "[a-c]")
        }
        val position = 1
        val rule = rrs.findRuntimeRule("a2c")

        val expected = CompleteTreeDataNode(rule, 1, 2, 2, 0)

        test(sentence, rrs, position, rule, expected)
    }

    @Test
    fun OnDemand_class_A() {
        val sentence = SentenceDefault("class A;")
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-A]+")
        }
        val patWS = rrs.findRuntimeRule("WS")
        val litClass = rrs.findRuntimeRule("'class'")
        val patName = rrs.findRuntimeRule("NAME")
        val litSemi = rrs.findRuntimeRule("';'")

        val scanner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val l1 = scanner.findOrTryCreateLeaf(sentence, 0, litClass)!!
        val l2 = scanner.findOrTryCreateLeaf(sentence, l1.nextInputPosition, patWS)!!
        val l3 = scanner.findOrTryCreateLeaf(sentence, l2.nextInputPosition, patName)!!
        val l4 = scanner.findOrTryCreateLeaf(sentence, l3.nextInputPosition, litSemi)!!
        val actual = listOf(l1, l2, l3, l4)

        val expected = listOf(
            CompleteTreeDataNode(litClass, 0, 5, 5, 0),
            CompleteTreeDataNode(patWS, 5, 6, 6, 0),
            CompleteTreeDataNode(patName, 6, 7, 7, 0),
            CompleteTreeDataNode(litSemi, 7, 8, 8, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun Classic_class_A() {
        val sentence = SentenceDefault("class A;")
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-A]+")
        }
        val patWS = rrs.findRuntimeRule("WS")
        val litClass = rrs.findRuntimeRule("'class'")
        val patName = rrs.findRuntimeRule("NAME")
        val litSemi = rrs.findRuntimeRule("';'")

        val scanner = ScannerClassic(RegexEnginePlatform, rrs.terminals)
        val leaves = mutableListOf<CompleteTreeDataNode>()
        val l1 = scanner.findOrTryCreateLeaf(sentence, 0, litClass)!!
        val l2 = scanner.findOrTryCreateLeaf(sentence, l1.nextInputPosition, patWS)!!
        val l3 = scanner.findOrTryCreateLeaf(sentence, l2.nextInputPosition, patName)!!
        val l4 = scanner.findOrTryCreateLeaf(sentence, l3.nextInputPosition, litSemi)!!
        val actual = listOf(l1, l2, l3, l4)

        val expected = listOf(
            CompleteTreeDataNode(litClass, 0, 5, 5, 0),
            CompleteTreeDataNode(patWS, 5, 6, 6, 0),
            CompleteTreeDataNode(patName, 6, 7, 7, 0),
            CompleteTreeDataNode(litSemi, 7, 8, 8, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun OnDemand_class_class() {
        val sentence = SentenceDefault("class class;")
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-A]+")
        }
        val patWS = rrs.findRuntimeRule("WS")
        val litClass = rrs.findRuntimeRule("'class'")
        val patName = rrs.findRuntimeRule("NAME")
        val litSemi = rrs.findRuntimeRule("';'")

        val scanner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val l1 = scanner.findOrTryCreateLeaf(sentence, 0, litClass)!!
        val l2 = scanner.findOrTryCreateLeaf(sentence, l1.nextInputPosition, patWS)!!
        val l3 = scanner.findOrTryCreateLeaf(sentence, l2.nextInputPosition, patName)!!
        val l4 = scanner.findOrTryCreateLeaf(sentence, l3.nextInputPosition, litSemi)!!
        val actual = listOf(l1, l2, l3, l4)

        val expected = listOf(
            CompleteTreeDataNode(litClass, 0, 5, 5, 0),
            CompleteTreeDataNode(patWS, 5, 6, 6, 0),
            CompleteTreeDataNode(patName, 6, 11, 11, 0),
            CompleteTreeDataNode(litSemi, 11, 12, 12, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun Classic_class_class() {
        val sentence = SentenceDefault("class class;")
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-A]+")
        }
        val patWS = rrs.findRuntimeRule("WS")
        val litClass = rrs.findRuntimeRule("'class'")
        val patName = rrs.findRuntimeRule("NAME")
        val litSemi = rrs.findRuntimeRule("';'")

        val scanner = ScannerClassic(RegexEnginePlatform, rrs.terminals)
        val leaves = mutableListOf<CompleteTreeDataNode>()
        val l1 = scanner.findOrTryCreateLeaf(sentence, 0, litClass)!!
        val l2 = scanner.findOrTryCreateLeaf(sentence, l1.nextInputPosition, patWS)!!
        val l3 = scanner.findOrTryCreateLeaf(sentence, l2.nextInputPosition, patName)!!
        val l4 = scanner.findOrTryCreateLeaf(sentence, l3.nextInputPosition, litSemi)!!
        val actual = listOf(l1, l2, l3, l4)

        val expected = listOf(
            CompleteTreeDataNode(litClass, 0, 5, 5, 0),
            CompleteTreeDataNode(patWS, 5, 6, 6, 0),
            CompleteTreeDataNode(litClass, 6, 11, 11, 0),
            CompleteTreeDataNode(litSemi, 11, 12, 12, 0)
        )
        assertEquals(expected, actual)
    }

}