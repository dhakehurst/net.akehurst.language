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

class test_RuntimeParser_parse_sList {

    val rrb = RuntimeRuleSetBuilder()

    private fun test_parse(sp: ScannerlessParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText)
    }

    // r = [a / ',']?
    // a = 'a'
    private fun literal_a01(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").separatedList(0, 1, rrb.literal(","), r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun literal_a01__r__empty() {
        val sp = literal_a01()
        val goalRuleName = "r"
        val inputText = ""

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r { ${'$'}empty }")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a01__r__a() {
        val sp = literal_a01()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a01__r__aa_fails() {
        val sp = literal_a01()
        val goalRuleName = "r"
        val inputText = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(1, e.location["column"])
    }

    @Test
    fun literal_a01__r__ac_fails() {
        val sp = literal_a01()
        val goalRuleName = "r"
        val inputText = "a,"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(2, e.location["column"])
    }

    @Test
    fun literal_a01__r__aca_fails() {
        val sp = literal_a01()
        val goalRuleName = "r"
        val inputText = "a,a"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(2, e.location["column"])
    }

    // r = [a / ',']*
    // a = 'a'
    private fun literal_a0n(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").separatedList(0, -1, rrb.literal(","), r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun literal_a0n__r__empty() {
        val sp = literal_a0n()
        val goalRuleName = "r"
        val inputText = ""

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r { ${'$'}empty }")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a0n_r__a() {
        val sp = literal_a0n()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a0n__r__aa_fails() {
        val sp = literal_a0n()
        val goalRuleName = "r"
        val inputText = "aa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(1, e.location["column"])
    }

    @Test
    fun literal_a0n__r__aca() {
        val sp = literal_a0n()
        val goalRuleName = "r"
        val inputText = "a,a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' ',' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }
    @Test
    fun literal_a0n__r__acaa_fails() {
        val sp = literal_a0n()
        val goalRuleName = "r"
        val inputText = "a,aa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(3, e.location["column"])
    }
    @Test
    fun literal_a0n__r__acaca() {
        val sp = literal_a0n()
        val goalRuleName = "r"
        val inputText = "a,a,a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' ',' 'a' ',' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
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

        assertEquals(1, e.location["line"])
        assertEquals(0, e.location["column"])
    }

    @Test
    fun literal_a1n__r__a() {
        val sp = literal_a1n()
        val goalRuleName = "r"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
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

        assertEquals(1, e.location["line"])
        assertEquals(1, e.location["column"])
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


    // r = [a / ','][2..5]
    // a = 'a'
    private fun literal_a25(): ScannerlessParser {
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("r").separatedList(2, 5, rrb.literal(","), r0)
        return ScannerlessParser(rrb.ruleSet())
    }

    @Test
    fun literal_a25__r__empty_fails() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = ""

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(0, e.location["column"])
    }

    @Test
    fun literal_a25__r__a_fails() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = "a"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(1, e.location["column"])
    }

    @Test
    fun literal_a25_a__r__aa() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = "aa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a25__r__aaa() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = "aaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a25__r__aaaa() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = "aaaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a25__r__aaaaa() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = "aaaaa"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)

        val p = SPPTParser(rrb)
        val expected = p.addTree("r {'a' 'a' 'a' 'a' 'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun literal_a25__r__a6_fails() {
        val sp = literal_a25()
        val goalRuleName = "r"
        val inputText = "aaaaaa"

        val e = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }

        assertEquals(1, e.location["line"])
        assertEquals(5, e.location["column"])
    }
}