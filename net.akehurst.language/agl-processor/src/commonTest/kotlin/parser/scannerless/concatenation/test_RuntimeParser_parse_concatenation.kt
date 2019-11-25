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

package net.akehurst.language.parser.scannerless.concatenation

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.ScannerlessParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_RuntimeParser_parse_concatenation {

    private fun test_parse(sp: ScannerlessParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText)
    }


    // S = 'a' 'b' 'c' ;
    private fun abc1(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        b.rule("S").concatenation(b.literal("a"), b.literal("b"), b.literal("c"))
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun abc1_S_abc() {
        val sp = this.abc1()
        val goalRuleName = "S"
        val inputText = "abc"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
        assertEquals(11,actual.seasons)
    }

    @Test
    fun abc1_r_empty_fails() {
        val sp = this.abc1()
        val goalRuleName = "S"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(1, ex.location.column, "column is wrong")
    }

    @Test
    fun abc1_r_a_fails() {
        val sp = this.abc1()
        val goalRuleName = "S"
        val inputText = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(1, ex.location.column, "column is wrong")
    }

    @Test
    fun abc1_r_ab_fails() {
        val sp = this.abc1()
        val goalRuleName = "S"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(2, ex.location.column, "column is wrong")
    }

    @Test
    fun abc1_r_abcd_fails() {
        val sp = this.abc1()
        val goalRuleName = "S"
        val inputText = "abcd"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(3, ex.location.column, "column is wrong")
    }

    // S = a b c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private fun abc2(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val r1 = b.rule("a").concatenation(b.literal("a"))
        val r2 = b.rule("b").concatenation(b.literal("b"))
        val r3 = b.rule("c").concatenation(b.literal("c"))
        b.rule("S").concatenation(r1, r2, r3)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun abc2_S_empty_fails() {
        val sp = this.abc2()
        val goalRuleName = "S"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(1, ex.location.column, "column is wrong")
    }
    @Test
    fun abc2_S_a_fails() {
        val sp = this.abc2()
        val goalRuleName = "S"
        val inputText = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(1, ex.location.column, "column is wrong")
    }
    @Test
    fun abc2_S_ab_fails() {
        val sp = this.abc2()
        val goalRuleName = "S"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(2, ex.location.column, "column is wrong")
    }
    @Test
    fun abc2_S_abc() {
        val sp = this.abc2()
        val goalRuleName = "S"
        val inputText = "abc"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }
    @Test
    fun abc2_S_abcd_fails() {
        val sp = this.abc2()
        val goalRuleName = "S"
        val inputText = "abcd"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line, "line is wrong")
        assertEquals(3, ex.location.column, "column is wrong")
    }

    // S = ab c;
    // ab = 'a' 'b' ;
    // c = 'c' ;
    private fun ab_c(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val r_ab = b.rule("ab").concatenation(b.literal("a"),b.literal("b"))
        val r_c = b.rule("c").concatenation(b.literal("c"))
        b.rule("S").concatenation(r_ab, r_c)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun ab_c__S__abc() {
        val sp = this.ab_c()
        val goalRuleName = "S"
        val inputText = "abc"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }
}