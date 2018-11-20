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
import net.akehurst.language.parser.sppt.SPPTParser
import kotlin.test.*

class test_RuntimeParser_parse_multi {

    val rrb = RuntimeRuleSetBuilder()

    private fun test_parse(sp: ScannerlessParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText)
    }

    // r = a?
    // a = 'a'
    private fun multi_0_1_a(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").multi(0, 1, r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun multi_0_1_a__r__empty() {
        val sp = multi_0_1_a()
        val goalRuleName = "r"
        val inputText = ""

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r { §empty }")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_0_1_a__r__a() {
        val sp = multi_0_1_a()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }


    @Test
    fun multi_0_1_a__r__aa_fails() {
        val sp = multi_0_1_a()
        val goalRuleName = "r"
        val inputText = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    // r = a*
    // a = 'a'
    private fun multi_0_n_a(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").multi(0, -1, r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun multi_0_n_a__r__empty() {
        val sp = multi_0_n_a()
        val goalRuleName = "r"
        val inputText = ""

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r { §empty }")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_0_n_a__r__a() {
        val sp = multi_0_n_a()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_0_n_a__r__aa() {
        val sp = multi_0_n_a()
        val goalRuleName = "r"
        val inputText = "aa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_0_n_a__r__aaa() {
        val sp = multi_0_n_a()
        val goalRuleName = "r"
        val inputText = "aaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    // r = a+
    // a = 'a'
    private fun multi_1_n_a(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").multi(1, -1, r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun multi_1_n_a__r__empty_fails() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun multi_1_n_a__r__a() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_1_n_a__r__aa() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "aa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_1_n_a__r__aaa() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "aaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_1_n_a__r__5000a() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "a".repeat(5000)

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val aleaves = "'a' ".repeat(5000)
        val expected = p.addTree("r {${aleaves}}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    // r = a[2..5]
    // a = 'a'
    private fun multi_2_5_a(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").multi(2, 5, r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun multi_2_5_a__r__empty_fails() {
        val sp = multi_2_5_a()
        val goalRuleName = "r"
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun multi_2_5_a__r__a_fails() {
        val sp = multi_2_5_a()
        val goalRuleName = "r"
        val inputText = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun multi_2_5_a__r__aa() {
        val sp = multi_2_5_a()
        val goalRuleName = "r"
        val inputText = "aa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_2_5_a__r__aaa() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "aaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_2_5_a__r__aaaa() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "aaaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_2_5_a__r__aaaaa() {
        val sp = multi_1_n_a()
        val goalRuleName = "r"
        val inputText = "aaaaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multi_2_5_a__r__a6_fails() {
        val sp = multi_2_5_a()
        val goalRuleName = "r"
        val inputText = "aaaaaa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(5, e.location.column)
    }

    // r = m
    // m = a b? a
    // a = 'a'
    // b = 'b'
    private fun rmab01a(): ScannerlessParser {
        val ra = rrb.literal("a")
        val rb = rrb.literal("b")
        val rbm = rrb.rule("bm").multi(0, 1, rb)
        val rm = rrb.rule("m").concatenation(ra, rbm, ra)
        val rr = rrb.rule("r").concatenation(rm)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun rma01__r__empty_fails() {
        val sp = rmab01a()
        val goalRuleName = "r"
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun rma01__r__a_fails() {
        val sp = rmab01a()
        val goalRuleName = "r"
        val inputText = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }


    @Test
    fun rma01__r__aa() {
        val sp = rmab01a()
        val goalRuleName = "r"
        val inputText = "aa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r { m { 'a' bm { §empty } 'a' } }")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

}