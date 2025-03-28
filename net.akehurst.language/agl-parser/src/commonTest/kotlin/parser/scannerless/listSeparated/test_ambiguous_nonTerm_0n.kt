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

package net.akehurst.language.parser.leftcorner.listSeparated

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_ambiguous_nonTerm_0n : test_LeftCornerParserAbstract() {

    // S = [ambig / sep ]*
    // ambig = a1 | a2
    // a1 = 'a' 'b'?
    // a2 = 'a'
    // sep = ','?
    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "ambig", "sep")
            choice("ambig", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("a1")
                ref("a2")
            }
            concatenation("a1") { literal("a"); ref("optB") }
            concatenation("a2") { literal("a") }
            optional("optB", "'b'")
            literal("'b'", "b")
            optional("sep", "','")
            literal("','", ",")
            preferenceFor("'a'") {
                left(listOf("a1"), setOf("<EOT>", "','", "'a'", "<EOT>", "','", "'a'"))
                left(listOf("a2"), setOf("<EOT>", "','", "'a'", "<EOT>", "','", "'a'"))
            }
        }
    }

    @Test
    fun empty() {
        val goal = "S"
        val sentence = ""

        val expected = "S { <EMPTY_LIST> }"

        super.test_pass(
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

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun aa() {
        val goal = "S"
        val sentence = "aa"

        val expected = """
        S {
          ambig { a2 { 'a' } }
          sep { §empty }
          ambig { a2 { 'a' } }
        }
        """.trimMargin()

        super.test_pass(
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

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acaa() {
        val goal = "S"
        val sentence = "a,aa"

        val expected = """
            S {
              ambig { a2 { 'a' } }
              sep { ',' }
              ambig { a2 { 'a' } }
              sep { §empty }
              ambig { a2 { 'a' } }
            }
        """

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acaca() {
        val goal = "S"
        val sentence = "a,a,a"

        val expected = """
            S {
              ambig { a2 { 'a' } }
              sep { ',' }
              ambig { a2 { 'a' } }
              sep { ',' }
              ambig { a2 { 'a' } }
            }
        """

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun acax100() {
        val goal = "S"
        val sentence = "a" + ",a".repeat(99)

        val expected = "S { ambig { a2 { 'a' } }" + " sep {','} ambig { a2 { 'a' } }".repeat(99) + "}"

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

}