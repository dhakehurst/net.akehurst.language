/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test

class test_AhoSetiUlman_4_5_5 : test_Abstract() {
    // S = C C ;
    // C = c C | d ;
    //
    // S = C C ;
    // C = C1 | d ;
    // C1 = c C ;
    private companion object {

        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C"); ref("C") }
            choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C1")
                literal("d")
            }
            concatenation("C1") { literal("c"); ref("C") }
        }
        val S = rrs.findRuntimeRule("S")
        val C = rrs.findRuntimeRule("C")
        val C1 = rrs.findRuntimeRule("C1")
        val T_c = rrs.findRuntimeRule("'c'")
        val T_d = rrs.findRuntimeRule("'d'")
        val G = rrs.startingState(S).runtimeRules.first()

        val SM = rrs.fetchStateSetFor(S)
        val s0 = SM.startState

        val lhs_cd = LookaheadSet(0, setOf(T_c, T_d))
    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RP(C1, 0, SOR), lhs_U, setOf(T_c)),  // C1 = . c C
                Triple(RP(C1, 0, 1), lhs_U, setOf(T_c, T_d)),  // C1 = c . C
                Triple(RP(C1, 0, EOR), lhs_U, setOf(UP)),  // C1 = c C .
                Triple(RP(C, 1, SOR), lhs_U, setOf(T_d)),  // C = . d
                Triple(RP(C, 1, EOR), lhs_U, setOf(UP)),  // C = d .
                Triple(RP(C, 0, SOR), lhs_U, setOf(T_c)),  // C = . C1
                Triple(RP(C, 0, EOR), lhs_U, setOf(UP)),  // C = C1 .
                Triple(RP(S, 0, SOR), lhs_U, setOf(T_c, T_d)),  // S = . C C
                Triple(RP(S, 0, 1), lhs_U, setOf(T_c, T_d)),  // S = C . C
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),  // S = C C .
                Triple(RP(G, 0, SOR), lhs_U, setOf(T_c, T_d)),  // G = . S
                Triple(RP(G, 0, EOR), lhs_U, setOf(UP))   // G = S .
        )

    override val s0_widthInto_expected: List<Pair<RulePosition, LookaheadSet>>
        get() = listOf(
                Pair(RP(T_c, 0, EOR), lhs_cd),
                Pair(RP(T_d, 0, EOR), lhs_cd)
        )

    @Test
    fun f() {
        TODO()
    }
}