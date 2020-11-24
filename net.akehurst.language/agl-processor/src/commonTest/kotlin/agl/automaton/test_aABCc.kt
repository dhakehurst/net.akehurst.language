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
import kotlin.test.assertEquals

class test_aABCc : test_Abstract() {

    /*
        S = b | a S c ;

        S = b | S1
        S1 = a S c
     */

    private companion object {

        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S"); literal("c") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val lhs_ab=SM.createLookaheadSet(setOf(a,b))
    }

    override val SM: ParserStateSet = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>> = listOf(
            Triple(RP(G, 0, SOR), lhs_U, setOf(a, b)),     // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP)),      // G = S .
            Triple(RP(S, 0, SOR), lhs_U, setOf(b)),       // S = . b
            Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),      // S = b .
            Triple(RP(S, 1, SOR), lhs_U, setOf(a)),       // S = . S1
            Triple(RP(S, 1, EOR), lhs_U, setOf(UP)),      // S = S1 .
            Triple(RP(S1, 0, SOR), lhs_U, setOf(a)),      // S1 = . a S c
            Triple(RP(S1, 0, 1), lhs_U, setOf(a, b)), // S1 = a . S c
            Triple(RP(S1, 0, 2), lhs_U, setOf(c)),   // S1 = a S . c
            Triple(RP(S1, 0, EOR), lhs_U, setOf(UP))      // S1 = a S c .

    )

    @Test
    fun calcLookaheadDown() {

        var actual = s0.stateSet.calcLookaheadDown(RulePosition(G, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        var expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(G, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = emptySet()
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 1, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S, 1, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S1, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a, b)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S1, 0, 1), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(c)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S1, 0, 2), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)

        actual = s0.stateSet.calcLookaheadDown(RulePosition(S1, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
        assertEquals(expected, actual)
    }

    override val s0_widthInto_expected: List<Pair<RulePosition, LookaheadSet>>
        get() = listOf(
                Pair(RP(b,0,EOR),lhs_U),
                Pair(RP(a,0,EOR),lhs_ab)
        )

}