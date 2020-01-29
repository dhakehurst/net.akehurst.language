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

package net.akehurst.language.parser.scannerless.listSeparated

import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.ScannerlessParser
import net.akehurst.language.parser.sppt.SPPTParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_sList {

    val rrb = RuntimeRuleSetBuilder()

    private fun test_parse(sp: ScannerlessParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText)
    }

    // r = [a / ',']+
    // a = 'a'
    private fun literal_a1n(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").separatedList(1, -1, rrb.literal(","), r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun literal_a1n__r__empty_fails() {
        val sp = literal_a1n()
        val goalRuleName = "r"
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun literal_a1n__r__a() {
        val sp = literal_a1n()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb.ruleSet())
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a1n__r__aa() {
        val sp = literal_a1n()
        val goalRuleName = "r"
        val inputText = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun literal_a1n__r__aca() {
        val sp = literal_a1n()
        val goalRuleName = "r"
        val inputText = "a,a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' ',' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a1n__r__500() {
        val sp = literal_a1n()
        val goalRuleName = "r"
        val inputText = "a"+",a".repeat(499)

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a'"+" ',' 'a'".repeat(499)+"}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    // r = [a / 'b'][2..5]
    // a = 'a'
    private fun literal_ab25(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").separatedList(2, 5, rrb.literal("b"), r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun literal_ab25__r__empty_fails() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(0, e.location.column)
    }

    @Test
    fun literal_ab25__r__a_fails() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun literal_ab25__r__ab_fails() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "ab"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(2, e.location.column)
    }

    @Test
    fun literal_ab25__r__aba() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "aba"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'b' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_ab25__r__ababa() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "ababa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'b' 'a' 'b' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_ab25__r__abababa() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "abababa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'b' 'a' 'b' 'a' 'b' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_ab25__r__ababababa() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "ababababa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'b' 'a' 'b' 'a' 'b' 'a' 'b' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_ab25__r__a6_fails() {
        val sp = literal_ab25()
        val goalRuleName = "r"
        val inputText = "abababababa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location.line)
        assertEquals(9, e.location.column)
    }
}