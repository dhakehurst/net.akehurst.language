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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import kotlin.test.*

class test_RuntimeParser_parse {

    private fun test_parse(sp: ScannerlessParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText)
    }

    // literal
    private fun literal_a(): ScannerlessParser {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("a").concatenation(r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    //  a = 'a' | 'a'
    @Test
    fun literal_a_a_a() {
        val sp = literal_a()
        val goalRuleName = "a"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    // a = 'a' | 'b'
    @Test
    fun literal_a_a_b_fails() {
        val sp = literal_a()
        val goalRuleName = "a"
        val inputText = "b"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }

    // pattern
    private fun pattern_a(): ScannerlessParser {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.pattern("a")
        val r1 = rrb.rule("a").concatenation(r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    // a = "a" | 'a'
    @Test
    fun pattern_a_a_a() {
        val sp = pattern_a()
        val goalRuleName = "a"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    // a = "a" | 'b'
    @Test
    fun pattern_a_a_b_fails() {
        val sp = pattern_a()
        val goalRuleName = "a"
        val inputText = "b"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }

    private fun pattern_a2c(): ScannerlessParser {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.pattern("[a-c]")
        val r1 = rrb.rule("a").concatenation(r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    // a = "[a-c]" | 'a'
    @Test
    fun pattern_a2c_a_a() {
        val sp = pattern_a2c()
        val goalRuleName = "a"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    // a = "[a-c]" | 'b'
    @Test
    fun pattern_a2c_a_b() {
        val sp = pattern_a2c()
        val goalRuleName = "a"
        val inputText = "b"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    // a = "[a-c]" | 'c'
    @Test
    fun pattern_a2c_a_c() {
        val sp = pattern_a2c()
        val goalRuleName = "a"
        val inputText = "c"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

}