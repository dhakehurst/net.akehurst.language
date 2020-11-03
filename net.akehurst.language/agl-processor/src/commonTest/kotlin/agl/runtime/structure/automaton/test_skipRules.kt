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

package net.akehurst.language.agl.runtime.structure

import kotlin.test.Test
import kotlin.test.assertEquals

class test_skipRules {

    // skip WS = "\s+" ;
    // skip COMMENT = "//[^\n]*$"
    // S = 'a' ;

    companion object {

        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            pattern("COMMENT", "//[^\\n]*$", true)
            concatenation("S") { literal("a") }
        }


        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val a = rrs.findRuntimeRule("'a'")
        val G = SM.startState.runtimeRule
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val s0 = rrs.startingState(S)

        val skipSS = rrs.skipParserStateSet!!
        val sk0 = skipSS.startState
        val skG = sk0.runtimeRule                             // G = skS ;
        val skM = skG.rhs.items[0]                            // skS = skC+
        val skC = skM.rhs.items[RuntimeRuleItem.MULTI__ITEM]  // skC = WS | CM
        val skWS = rrs.findRuntimeRule("WS")
        val skCM = rrs.findRuntimeRule("COMMENT")

        val sk1 = skipSS.states[RulePosition(skWS,0,RulePosition.END_OF_RULE)]


        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_skWCU = SM.runtimeRuleSet.createLookaheadSet(setOf(skWS, skCM, UP))
        val lhs_aT = SM.runtimeRuleSet.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))

    }

    @Test
    fun parentPosition() {
        var actual = skipSS.parentPosition[skG]
        var expected = emptySet<RulePosition>()
        assertEquals(expected, actual)

        actual = skipSS.parentPosition[skM]
        expected = setOf(
                RulePosition(G, 0, 0)
        )
        assertEquals(expected, actual)


        actual = skipSS.parentPosition[skC]
        expected = setOf(
                RulePosition(skM, 0, 0),
                RulePosition(skM, 0, RulePosition.MULIT_ITEM_POSITION)
        )
        assertEquals(expected, actual)


        actual = skipSS.parentPosition[skWS]
        expected = setOf(
                RulePosition(skC, 0, 0)
        )
        assertEquals(expected, actual)

        actual = skipSS.parentPosition[skCM]
        expected = setOf(
                RulePosition(skC, 1, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun firstTerminals() {
        var actual = skipSS.firstTerminals[RulePosition(skG, 0, 0)]
        var expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skM, 0, 0)]
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skM, 0, RulePosition.MULIT_ITEM_POSITION)]
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skC, 0, 0)]
        expected = setOf(skWS)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RulePosition(skC, 1, 0)]
        expected = setOf(skCM)
        assertEquals(expected, actual)


    }

    @Test
    fun firstOf() {

        var actual = s0.stateSet.firstOf(RulePosition(G, 0, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        var expected = setOf(a)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(G, 0, RulePosition.END_OF_RULE), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf()
        assertEquals(expected, actual)


        actual = s0.stateSet.firstOf(RulePosition(S, 1, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected, actual)

        actual = s0.stateSet.firstOf(RulePosition(S, 1, 0), setOf(RuntimeRuleSet.USE_PARENT_LOOKAHEAD))
        expected = setOf(a)
        assertEquals(expected, actual)


    }

    @Test
    fun expectedAfter() {

        var actual = skipSS.expectedAfter(RulePosition(skG, 0, 0))
        var expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skG, 0, RulePosition.END_OF_RULE))
        expected = setOf(UP)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skM, 0, 0))
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skM, 0, RulePosition.MULIT_ITEM_POSITION))
        expected = setOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skM, 0, RulePosition.END_OF_RULE))
        expected = setOf(UP)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skC, 0, 0))
        expected = setOf(skWS)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skC, 0, RulePosition.END_OF_RULE))
        expected = setOf(skWS, skCM, UP)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skC, 0, 0))
        expected = setOf(skWS)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skC, 0, RulePosition.END_OF_RULE))
        expected = setOf(skWS, skCM, UP)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skWS, 0, RulePosition.END_OF_RULE))
        expected = setOf(skWS, skCM, UP)
        assertEquals(expected, actual)

        actual = skipSS.expectedAfter(RulePosition(skCM, 0, RulePosition.END_OF_RULE))
        expected = setOf(skWS, skCM, UP)
        assertEquals(expected, actual)


    }

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

        val actual = sk1.heightOrGraftInto(sk0.rulePosition).toList()

        val expected = listOf(
                HeightGraft(RulePosition(test_leftRecursive.G, 0, 0),RulePosition(skC, 0, RulePosition.START_OF_RULE),RulePosition(skC, 0, RulePosition.END_OF_RULE), lhs_a,lhs_U)
        )
        assertEquals(expected, actual)

    }
}