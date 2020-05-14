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
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_literal_a2t5 : test_ScanOnDemandParserAbstract() {

    companion object {

        val rrb = RuntimeRuleSetBuilder()

        // S = [a / 'b'][2..5]
        // a = 'a'
        private fun literal_ab25(): RuntimeRuleSet {
            val r0 = rrb.literal("a")
            val r1 = rrb.rule("S").separatedList(2, 5, rrb.literal("b"), r0)
            return rrb.ruleSet()
        }

        val sp = literal_ab25()
        val goalRuleName = "S"
    }

    @Test
    fun literal_ab25__r__empty_fails() {
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun literal_ab25__r__a_fails() {
        val inputText = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            test(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
        assertEquals(setOf("','"), e.expected)
    }

    @Test
    fun literal_ab25__r__ab_fails() {
        val inputText = "ab"

        val e = assertFailsWith(ParseFailedException::class) {
            test(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(3, e.location.column)
        assertEquals(setOf("'a'"), e.expected)
    }

    @Test
    fun literal_ab25__r__aba() {
        val inputText = "aba"

        val expected = "S {'a' 'b' 'a'}"

        super.test(sp, goalRuleName, inputText, expected)
    }

    @Test
    fun literal_ab25__r__ababa() {
        val inputText = "ababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a'}"

        super.test(sp, goalRuleName, inputText, expected)
    }

    @Test
    fun literal_ab25__r__abababa() {
        val inputText = "abababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a' 'b' 'a'}"

        super.test(sp, goalRuleName, inputText, expected)
    }

    @Test
    fun literal_ab25__r__ababababa() {
        val inputText = "ababababa"

        val expected = "S {'a' 'b' 'a' 'b' 'a' 'b' 'a' 'b' 'a'}"

        super.test(sp, goalRuleName, inputText, expected)

    }

    @Test
    fun literal_ab25__r__a6_fails() {
        val inputText = "abababababa"

        val e = assertFailsWith(ParseFailedException::class) {
            test(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(10, e.location.column)
        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT.tag), e.expected)
    }
}