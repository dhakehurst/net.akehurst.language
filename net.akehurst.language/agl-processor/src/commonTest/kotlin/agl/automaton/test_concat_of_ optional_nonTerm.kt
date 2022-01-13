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

import agl.automaton.AutomatonTest
import agl.automaton.automaton
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_concat_of_optional_nonTerm : test_AutomatonAbstract() {

    //    GenericMethodInvocation = TypeArguments? MethodInvocation ;
    //    MethodInvocation = IDENTIFIER ArgumentList ;
    //    ArgumentList = '(' Arguments ')' ;
    //    Arguments = [ Expression / ',' ]* ;

    // S =  optA B ;
    // optA = A? ;
    // A = a
    // B = b L ;
    // L = c Ls d ;
    // Ls = [ e / f ]*

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("optA"); ref("B") }
            multi("optA", 0, -1, "A")
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val eS = S.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
        val optA = rrs.findRuntimeRule("optA")
        val A = rrs.findRuntimeRule("A")
        val B = rrs.findRuntimeRule("B")
        val T_a = rrs.findRuntimeRule("'a'")
        val T_b = rrs.findRuntimeRule("'b'")
        val E_optA = optA.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
    }
    @Test
    override fun firstOf() {
        listOf(
            /* G = S . */ Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, LHS(UP)),
            /* G = . S */ Triple(RulePosition(G, 0, 0), lhs_U, LHS(UP))
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()
        TODO()
        val expected = listOf<WidthInfo>(

        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun parse_abc() {
        //TODO: is there a way to reset the rrs if it needs it?
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))       // G = . S
            val s1 = state(RP(T_a, 0, EOR))       // 'a'
            val s2 = state(RP(E_optA, 0, EOR))
            val s3 = state(RP(optA, 1, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(T_b, 0, EOR))
            val s6 = state(RP(B, 0, EOR))
            val s7 = state(RP(S, 0, EOR))
            val s8 = state(RP(G, 0, EOR))
            // S =  optA B ;
            // optA = A? ;
            // A = 'a'
            // B = b ;
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}