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

package net.akehurst.language.parser.leftcorner.leftAndRightRecursive

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

/**
 * Left recursion through a chain of unit rules between B and E.
 * With 2 or more unit rules (A0 = A1 ; A1 = E) 'a+b+c' used to fail at the second '+',
 * because the closure info for items on the left-recursive cycle was incomplete.
 */
class test_leftRecursion_unitChain : test_LeftCornerParserAbstract() {

    // S = E ;
    // E = B | P ;
    // B = A0 O A0 ;
    // A0 = A1 ; ... ; An = E     (n = chainLength, A0 = E when n = 0)
    // O = '+' ;
    // P = "[a-z]" ;
    private companion object {
        fun rrs(chainLength: Int) = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choiceLongest("E") { ref("B"); ref("P") }
            concatenation("B") { ref("A0"); ref("O"); ref("A0") }
            for (i in 0 until chainLength) concatenation("A$i") { ref("A${i + 1}") }
            concatenation("A$chainLength") { ref("E") }
            concatenation("O") { literal("+") }
            concatenation("P") { pattern("[a-z]") }
        }

        val goal = "S"

        // wrap 'inner' in the unit rule chain A0 { A1 { ... An { inner } } }
        fun a(chainLength: Int, inner: String): String =
            (chainLength downTo 0).fold(inner) { acc, i -> "A$i { $acc }" }

        fun p(chainLength: Int, c: Char) = a(chainLength, "E { P { \"[a-z]\" : '$c' } }")
    }

    private fun check(chainLength: Int, sentence: String, expected: String) {
        super.test_pass(
            rrs = rrs(chainLength),
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    private fun a_add_b(n: Int) = check(n, "a+b", "S { E { B { ${p(n, 'a')} O { '+' } ${p(n, 'b')} } } }")

    // left associative, as when there are no unit rules in the chain
    private fun a_add_b_add_c(n: Int) = check(
        n, "a+b+c",
        "S { E { B { ${a(n, "E { B { ${p(n, 'a')} O { '+' } ${p(n, 'b')} } }")} O { '+' } ${p(n, 'c')} } } }"
    )

    private fun a_add_b_add_c_add_d(n: Int) = check(
        n, "a+b+c+d",
        "S { E { B { ${a(n, "E { B { ${a(n, "E { B { ${p(n, 'a')} O { '+' } ${p(n, 'b')} } }")} O { '+' } ${p(n, 'c')} } }")} O { '+' } ${p(n, 'd')} } } }"
    )

    @Test
    fun chain0_a_add_b() = a_add_b(0)

    @Test
    fun chain0_a_add_b_add_c() = a_add_b_add_c(0)

    @Test
    fun chain0_a_add_b_add_c_add_d() = a_add_b_add_c_add_d(0)

    @Test
    fun chain1_a_add_b() = a_add_b(1)

    @Test
    fun chain1_a_add_b_add_c() = a_add_b_add_c(1)

    @Test
    fun chain1_a_add_b_add_c_add_d() = a_add_b_add_c_add_d(1)

    @Test
    fun chain2_a_add_b() = a_add_b(2)

    @Test
    fun chain2_a_add_b_add_c() = a_add_b_add_c(2)

    @Test
    fun chain2_a_add_b_add_c_add_d() = a_add_b_add_c_add_d(2)

}
