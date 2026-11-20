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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

/**
 * The on-demand scanner finds '-' and '->' at the same position.
 * Ending the list (graft into S, only valid for '->') and growing the list (graft into I, only valid for '-')
 * consume different tokens, so both choices are kept; the following input decides.
 * (Before, only the left-most graft was kept, so the parse failed at '->'.)
 */
class test_DifferentTokensSamePosition : test_LeftCornerParserAbstract() {

    private companion object {
        // S = 'w' E '->' E ;
        // E = ID | I ;
        // I = [ E / '-' ]2+ ;
        // ID = "[a-z]" ;
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("w"); ref("E"); ref("'->'"); ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ID")
                ref("I")
            }
            sList("I", 2, -1, "E", "'-'")
            pattern("ID", "[a-z]")
            literal("'-'", "-")
            literal("'->'", "->")
        }
    }

    @Test
    fun list_then_arrow() {
        val expected = """
            S {
              'w'
              E { I { E { ID : 'a' } '-' E { ID : 'c' } } }
              '->'
              E { ID : 'b' }
            }
        """.trimIndent()
        super.test_pass(rrs, "S", "wa-c->b", 1, expected)
    }

    @Test
    fun arrow_only() {
        val expected = """
            S {
              'w'
              E { ID : 'a' }
              '->'
              E { ID : 'b' }
            }
        """.trimIndent()
        super.test_pass(rrs, "S", "wa->b", 1, expected)
    }
}
