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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_aObOc : test_ScanOnDemandParserAbstract() {

    // S = a | b | c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("a")
            ref("b")
            ref("c")
        }
        concatenation("a") { literal("a") }
        concatenation("b") { literal("b") }
        concatenation("c") { literal("c") }
    }

    @Test
    fun empty_fails() {
        val goalRuleName = "r"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test(rrs, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'", "'b'", "'c'"), ex.expected)
    }

    @Test
    fun a() {
        val goalRuleName = "S"
        val inputText = "a"

        val expected = """
            S { a { 'a' } }
        """
        val actual = test(rrs, goalRuleName, inputText, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun b() {
        val goalRuleName = "S"
        val inputText = "b"

        val expected = """
            S|1 { b { 'b' } }
        """
        val actual = test(rrs, goalRuleName, inputText, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun c() {
        val goalRuleName = "S"
        val inputText = "c"

        val expected = """
            S|2 { c { 'c' } }
        """
        val actual = test(rrs, goalRuleName, inputText, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun d_fails() {
        val goalRuleName = "S"
        val inputText = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            test(rrs, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'", "'b'", "'c'"), ex.expected)
    }

    @Test
    fun ab_fails() {
        val goalRuleName = "S"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test(rrs, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT_TAG), ex.expected)
    }

}