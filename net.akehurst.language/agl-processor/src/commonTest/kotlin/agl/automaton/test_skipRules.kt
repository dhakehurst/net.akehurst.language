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

class test_skipRules : test_Abstract() {

    // skip WS = "\s+" ;
    // skip COMMENT = "//[^\n]*$"
    // S = 'a' ;

    private companion object {

        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            pattern("COMMENT", "//[^\\n]*$", true)
            concatenation("S") { literal("a") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val a = rrs.findRuntimeRule("'a'")
        val G = SM.startState.runtimeRules.first()

        val s0 = rrs.startingState(S)

        val skipSS = rrs.skipParserStateSet!!
        val sk0 = skipSS.startState
        val skG = sk0.runtimeRules.first()                    // G = skS ;
        val skM = skG.rhs.items[0]                            // skS = skC+
        val skC = skM.rhs.items[RuntimeRuleItem.MULTI__ITEM]  // skC = WS | CM
        val skWS = rrs.findRuntimeRule("WS")
        val skCM = rrs.findRuntimeRule("COMMENT")

        val sk1 = skipSS.states[listOf(RulePosition(skWS, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_skWCU = SM.runtimeRuleSet.createLookaheadSet(setOf(skWS, skCM, UP))
        val lhs_aT = SM.runtimeRuleSet.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))
        val lhs_WS_CM_UP = SM.runtimeRuleSet.createLookaheadSet(setOf(skWS, skCM, UP))
    }

    override val SM: ParserStateSet get() = Companion.SM

    @Test
    fun firstTerminals() {
        //TODO
        var actual = skipSS.firstTerminals[RulePosition(skG, 0, 0)]
        var expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skM, 0, 0)]
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skM, 0, RulePosition.POSITION_MULIT_ITEM)]
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skC, 0, 0)]
        expected = setOf(skWS)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skC, 1, 0)]
        expected = setOf(skCM)
        assertEquals(expected, actual)


    }

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RP(G, 0, SOR), lhs_U, setOf(a)),    // G = . S
                Triple(RP(G, 0, EOR), lhs_U, setOf(UP)),   // G = S .
                Triple(RP(S, 0, SOR), lhs_U, setOf(a)),    // S = . a
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),   // S = a .

                Triple(RP(skG, OMI, SOR), lhs_U, setOf(a)),      // skG = . skM+
                Triple(RP(skG, OMI, EOR), lhs_U, setOf(a)),      // skG = skM+ .
                Triple(RP(skG, 0, EOR), lhs_U, setOf(UP)),   // skG = skM . skM+
                Triple(RP(skG, 0, SOR), lhs_U, setOf(a)),    // skG = skM+ .
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),     // skM = . WS
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),     // skM = WS .
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),     // skM = . CM
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP))      // skM = CM .
        )


    override val s0_widthInto_expected: List<Pair<RulePosition, LookaheadSet>>
        get() = listOf(
                Pair(RP(a, 0, EOR), lhs_U)
        )

    @Test
    fun sk0_widthInto() {
        val actual = sk0.widthInto(null).toList()

        val expected = listOf(
                Pair(RulePosition(skWS, 0, RulePosition.END_OF_RULE), lhs_skWCU),
                Pair(RulePosition(skCM, 0, RulePosition.END_OF_RULE), lhs_skWCU)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = sk1.heightOrGraftInto(sk0).toList()

        val expected = listOf(
                HeightGraft(
                    listOf(RulePosition(skC, 0, RulePosition.START_OF_RULE)),
                    listOf(RulePosition(skC, 0, RulePosition.END_OF_RULE)),
                    lhs_WS_CM_UP,
                    lhs_WS_CM_UP
                )
        )
        assertEquals(expected, actual)

    }
}