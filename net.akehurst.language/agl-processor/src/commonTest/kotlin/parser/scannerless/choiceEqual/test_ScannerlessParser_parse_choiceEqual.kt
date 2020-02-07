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

package net.akehurst.language.parser.scannerless.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.ScannerlessParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_ScannerlessParser_parse_choiceEqual {

    private fun test_parse(sp: ScannerlessParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText)
    }

    // r = a | b | c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private fun aObOcLiteral(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val r1 = b.rule("a").concatenation(b.literal("a"))
        val r2 = b.rule("b").concatenation(b.literal("b"))
        val r3 = b.rule("c").concatenation(b.literal("c"))
        b.rule("r").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, r1, r2, r3)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun aObOcLiteral_r_empty_fails() {
        val sp = this.aObOcLiteral()
        val goalRuleName = "r"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }

    @Test
    fun aObOcLiteral_r_a() {
        val sp = this.aObOcLiteral()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun aObOcLiteral_r_b() {
        val sp = this.aObOcLiteral()
        val goalRuleName = "r"
        val inputText = "b"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun aObOcLiteral_r_c() {
        val sp = this.aObOcLiteral()
        val goalRuleName = "r"
        val inputText = "c"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun aObOcLiteral_r_d_fails() {
        val sp = this.aObOcLiteral()
        val goalRuleName = "r"
        val inputText = "d"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun aObOcLiteral_r_ab_fails() {
        val sp = this.aObOcLiteral()
        val goalRuleName = "r"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    // S = ab | c;
    // ab = a b ;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private fun abOcLiteral(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val ra = b.rule("a").concatenation(b.literal("a"))
        val rb = b.rule("b").concatenation(b.literal("b"))
        val rc = b.rule("c").concatenation(b.literal("c"))
        val rab = b.rule("ab").concatenation(ra, rb)
        b.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, rab, rc)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun abOcLiteral_r_empty_fails() {
        val sp = this.abOcLiteral()
        val goalRuleName = "S"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }

    @Test
    fun abOcLiteral_r_a_fails() {
        val sp = this.abOcLiteral()
        val goalRuleName = "S"
        val inputText = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun abOcLiteral_r_ab() {
        val sp = this.abOcLiteral()
        val goalRuleName = "S"
        val inputText = "ab"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun abOcLiteral_r_abc_fails() {
        val sp = this.abOcLiteral()
        val goalRuleName = "S"
        val inputText = "abc"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }

    @Test
    fun abOcLiteral_r_c() {
        val sp = this.abOcLiteral()
        val goalRuleName = "S"
        val inputText = "c"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    // r = a | b c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private fun aObcLiteral(): ScannerlessParser {
        val b = RuntimeRuleSetBuilder()
        val ra = b.rule("a").concatenation(b.literal("a"))
        val rb = b.rule("b").concatenation(b.literal("b"))
        val rc = b.rule("c").concatenation(b.literal("c"))
        val rbc = b.rule("bc").concatenation(rb, rc)
        b.rule("r").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, ra, rbc)
        val sp = ScannerlessParser(b.ruleSet())
        return sp
    }

    @Test
    fun aObcLiteral_r_empty_fails() {
        val sp = this.aObcLiteral()
        val goalRuleName = "r"
        val inputText = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun aObcLiteral_r_a() {
        val sp = this.aObcLiteral()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun aObcLiteral_r_ab_fails() {
        val sp = this.aObcLiteral()
        val goalRuleName = "r"
        val inputText = "ab"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }

    @Test
    fun aObcLiteral_r_abc_fails() {
        val sp = this.aObcLiteral()
        val goalRuleName = "r"
        val inputText = "abc"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(2, ex.location.column)
    }

    @Test
    fun aObcLiteral_r_bc() {
        val sp = this.aObcLiteral()
        val goalRuleName = "r"
        val inputText = "bc"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }
}