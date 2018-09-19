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

    // multi
    // r = 'a'?
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
        val expected = p.addTree("r { ${'$'}empty }")

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

        assertEquals(1, e.location["line"])
        assertEquals(1, e.location["column"])
    }

    // multi
    // r = 'a'*
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
        val expected = p.addTree("r { ${'$'}empty }")

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
        val expected = p.addTree("r {'a'}")

        assertEquals(expected.toStringAll, actual.toStringAll)
    }
}