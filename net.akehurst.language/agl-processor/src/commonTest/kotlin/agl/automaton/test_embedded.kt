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

import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_embedded : test_Abstract() {

    /*
    B = b ;

    S = a gB a ;
    gB = grammar B ;
 */
    private companion object {

        val rrsB = runtimeRuleSet {
            concatenation("B") { literal("b") }
        }
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); ref("gB"); literal("a"); }
            embedded("gB", rrsB, rrsB.findRuntimeRule("B"))
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val gB = rrs.findRuntimeRule("gB")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1).startState.runtimeRules.first()

        val B = rrsB.findRuntimeRule("B")
        val b_ = rrsB.findRuntimeRule("'b'")

        val S_SM = rrsB.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val s0 = S_SM.startState

        val B_SM = rrsB.fetchStateSetFor(B, AutomatonKind.LOOKAHEAD_1)
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, setOf(a)),      // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP)),     // G = S .
            Triple(RP(S, 0, SOR), lhs_U, setOf(a)),      // S = . a gB a
            Triple(RP(S, 0, 1), lhs_U, setOf(b_)),  // S = a . gB a
            Triple(RP(S, 0, 2), lhs_U, setOf(a)),   // S = a gB . a
            Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),     // S = a gB a .
            Triple(RP(gB, 0, SOR), lhs_U, setOf(UP)),     // gB = . grammar B
            Triple(RP(gB, 0, EOR), lhs_U, setOf(a)),     // gB = grammar B .
            Triple(RP(B, 0, SOR), lhs_U, setOf(UP)),     // B = . b
            Triple(RP(B, 0, EOR), lhs_U, setOf(a))      // B = b .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        TODO("not implemented")
    }


}