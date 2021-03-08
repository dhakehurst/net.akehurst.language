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

package net.akehurst.language.parser.scanondemand

import net.akehurst.language.agl.automaton.AutomatonKind
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import kotlin.test.*

class test_RuntimeParser_parse_empty {

    private fun test_parse(sp: ScanOnDemandParser, goalRuleName: String, inputText: String): SharedPackedParseTree {
        return sp.parse(goalRuleName, inputText, AutomatonKind.LOOKAHEAD_1)
    }

    //  R = <empty>
    private fun empty(): ScanOnDemandParser {
        val rrb = RuntimeRuleSetBuilder()
        rrb.rule("R").empty()
        return ScanOnDemandParser(rrb.ruleSet())
    }

    @Test
    fun empty_R_empty() {
        val sp = empty()
        val goalRuleName = "R"
        val inputText = ""

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun empty_R_a_fails() {
        val sp = empty()
        val goalRuleName = "R"
        val inputText = "a"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    //  R = 'a' | e
    //  e = <empty>
    private fun choiceRempty(): ScanOnDemandParser {
        val rrb = RuntimeRuleSetBuilder()
        val e = rrb.rule("e").empty()
        val a = rrb.literal("a")
        rrb.rule("R").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,a, e)
        return ScanOnDemandParser(rrb.ruleSet())
    }

    @Test
    fun choiceRempty_R_empty() {
        val sp = choiceRempty()
        val goalRuleName = "R"
        val inputText = ""

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun choiceRempty_R_a() {
        val sp = choiceRempty()
        val goalRuleName = "R"
        val inputText = "a"

        val actual = test_parse(sp, goalRuleName, inputText)

        assertNotNull(actual)
    }

    @Test
    fun choiceRempty_R_b_fails() {
        val sp = empty()
        val goalRuleName = "R"
        val inputText = "b"

        val ex = assertFailsWith(ParseFailedException::class) {
            test_parse(sp, goalRuleName, inputText)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }
}