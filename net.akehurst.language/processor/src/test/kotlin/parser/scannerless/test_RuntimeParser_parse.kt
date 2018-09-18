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
        assertEquals(1, ex.location["line"])
        assertEquals(0, ex.location["column"])
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
        assertEquals(1, ex.location["line"])
        assertEquals(0, ex.location["column"])
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

    // choice
    private fun abcChoice(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val r1 = b.rule("a").concatenation(b.literal("a"))
        val r2 = b.rule("b").concatenation(b.literal("b"))
        val r3 = b.rule("c").concatenation(b.literal("c"))
        b.rule("r").choiceEqual(r1, r2, r3)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun abcChoice_r_a() {
        val sp = this.abcChoice()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun abcChoice_r_b() {
        val sp = this.abcChoice()
        val goalRuleName = "r"
        val inputText = "b"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun abcChoice_r_c() {
        val sp = this.abcChoice()
        val goalRuleName = "r"
        val inputText = "c"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun abcChoice_r_d_fails() {
        val sp = this.abcChoice()
        val goalRuleName = "r"
        val inputText = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"])
        assertEquals(0, ex.location["column"])
    }

    // concatenation
    private fun abcConcatenation1(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        b.rule("r").concatenation(b.literal("a"), b.literal("b"), b.literal("c"))
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun abcConcatenation1_r_abc() {
        val sp = this.abcConcatenation1()
        val goalRuleName = "r"
        val inputText = "abc"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun abcConcatenation1_r_empty() {
        val sp = this.abcConcatenation1()
        val goalRuleName = "r"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(0, ex.location["column"], "column is wrong")
    }

    @Test
    fun abcConcatenation1_r_a() {
        val sp = this.abcConcatenation1()
        val goalRuleName = "r"
        val inputText = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(1, ex.location["column"], "column is wrong")
    }

    @Test
    fun abcConcatenation1_r_ab() {
        val sp = this.abcConcatenation1()
        val goalRuleName = "r"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(2, ex.location["column"], "column is wrong")
    }

    @Test
    fun abcConcatenation1_r_abcd() {
        val sp = this.abcConcatenation1()
        val goalRuleName = "r"
        val inputText = "abcd"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(3, ex.location["column"], "column is wrong")
    }

    // concatenation
    private fun abcConcatenation2(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val r1 = b.rule("a").concatenation(b.literal("a"))
        val r2 = b.rule("b").concatenation(b.literal("b"))
        val r3 = b.rule("c").concatenation(b.literal("c"))
        b.rule("r").concatenation(r1, r2, r3)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun abcConcatenation2_r_empty() {
        val sp = this.abcConcatenation2()
        val goalRuleName = "r"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(0, ex.location["column"], "column is wrong")
    }
    @Test
    fun abcConcatenation2_r_a() {
        val sp = this.abcConcatenation2()
        val goalRuleName = "r"
        val inputText = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(1, ex.location["column"], "column is wrong")
    }
    @Test
    fun abcConcatenation2_r_ab() {
        val sp = this.abcConcatenation2()
        val goalRuleName = "r"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(2, ex.location["column"], "column is wrong")
    }
    @Test
    fun abcConcatenation2_r_abc() {
        val sp = this.abcConcatenation2()
        val goalRuleName = "r"
        val inputText = "abc"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }
    @Test
    fun abcConcatenation2_r_abcd() {
        val sp = this.abcConcatenation2()
        val goalRuleName = "r"
        val inputText = "abcd"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location["line"], "line is wrong")
        assertEquals(3, ex.location["column"], "column is wrong")
    }
}