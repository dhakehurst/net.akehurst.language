/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner.ambiguity

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

/**
 * Two derivations give the same complete node (same rule, start, end and option) that the merge decision can not decide,
 * here different splits of the input between the children of a concatenation.
 * The choice must not depend on the order of the parse: at the first child that differs, the longer child wins,
 * so lists are greedy (no dynamic priority for concatenation children).
 * (test_LeftCornerParserAbstract also checks that the pre-built automaton gives the same tree.)
 */
class test_Undecidable_concatenationSplit : test_LeftCornerParserAbstract() {

    private companion object {
        // S = A B ; A = 'x' Y1 ; Y1 = 'y'? ; B = Y2 'z' ; Y2 = 'y'?
        val rrsOptional = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B") }
            concatenation("A") { literal("x"); ref("Y1") }
            optional("Y1", "'y'")
            concatenation("B") { ref("Y2"); literal("z") }
            optional("Y2", "'y'")
            literal("'y'", "y")
        }

        // S = A B ; A = 'a'* ; B = 'a'*
        val rrsList = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B") }
            multi("A", 0, -1, "'a'")
            multi("B", 0, -1, "'a'")
            literal("'a'", "a")
        }
    }

    @Test
    fun optional_xyz() {
        val expected = """
            S {
              A { 'x' Y1 { 'y' } }
              B { Y2 { <EMPTY> } 'z' }
            }
        """.trimIndent()
        super.test_pass(rrsOptional, "S", "xyz", 1, expected)
    }

    @Test
    fun list_aaa() {
        val expected = """
            S {
              A { 'a' 'a' 'a' }
              B { <EMPTY_LIST> }
            }
        """.trimIndent()
        super.test_pass(rrsList, "S", "aaa", 1, expected)
    }
}
