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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_ambiguous_nonTerm_0n : test_ScanOnDemandParserAbstract() {

    // S = [ambig / sep ]*
    // ambig = a1 | a2
    // a1 = 'a'
    // a2 = 'a' 'b'?
    // sep = ','?
    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "ambig", "sep")
            choice("ambig", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("a1")
                ref("a2")
            }
            concatenation("a1") { literal("a") }
            concatenation("a2") { literal("a"); ref("optB") }
            multi("optB", 0, 1, "'b'")
            literal("'b'", "b")
            multi("sep", 0, 1, "','")
            literal("','", ",")
        }
    }

    @Test
    fun empty() {
        val goal = "S"
        val sentence = ""

        val expected = "S { §empty }"

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = "S { ambig { a2 {'a'} } }"

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aa_fails() {
        val goal = "S"
        val sentence = "aa"

        val expected = """
        S {
          ambig { a2 { 'a' } }
          sep { §empty }
          ambig { a2 { 'a' } }
        }
        """.trimMargin()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aca() {
        val goal = "S"
        val sentence = "a,a"

        val expected = """
        S {
          ambig { a2 { 'a' } }
          sep { ',' }
          ambig { a2 { 'a' } }
        }
        """.trimMargin()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acaa_fails() {
        val goal = "S"
        val sentence = "a,aa"

        val expected = "S {'a' sep {','} 'a' sep|1 {§empty} 'a'}"

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun acaca() {
        val goal = "S"
        val sentence = "a,a,a"

        val expected = "S {'a' sep {','} 'a' sep {','} 'a'}"

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun acax100() {
        val goal = "S"
        val sentence = "a" + ",a".repeat(99)

        val expected = "S {'a'" + " sep {','} 'a'".repeat(99) + "}"

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = *arrayOf(expected)
        )
    }

}