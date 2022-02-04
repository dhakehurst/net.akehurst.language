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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_skipRules : test_AutomatonAbstract() {

    // skip WS = "\s+" ;
    // skip COMMENT = "//[^\n]*$"
    // S = 'a' ;

    private companion object {

        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            pattern("COMMENT", "//[^\\n]*[\\n]", true)
            concatenation("S") { literal("a") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val a = rrs.findRuntimeRule("'a'")
        val G = SM.startState.runtimeRules.first()

        val s0 = SM.startState

        val skipSS = rrs.skipParserStateSet!!
        val sk0 = skipSS.startState
        val skG = sk0.runtimeRules.first()                    // G = skS ;
        val skM = skG.rhs.items[0]                            // skS = skC+
        val skC = skM.rhs.items[RuntimeRuleItem.MULTI__ITEM]  // skC = WS | CM
        val skWS = rrs.findRuntimeRule("WS")
        val skCM = rrs.findRuntimeRule("COMMENT")

        val sk1 = skipSS.states[listOf(RulePosition(skWS, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.createLookaheadSet(false,false, false,setOf(a))
        val lhs_skWCU = SM.createLookaheadSet(true, false, false,setOf(skWS, skCM))
        val lhs_aT = SM.createLookaheadSet(false, true, false,setOf(a))
        val lhs_WS_CM_UP = SM.createLookaheadSet(true, false, false,setOf(skWS, skCM))
    }

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

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, LHS(a)),    // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP)),   // G = S .
            Triple(RP(S, 0, SOR), lhs_U, LHS(a)),    // S = . a
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),   // S = a .

            Triple(RP(skG, OMI, SOR), lhs_U, LHS(a)),      // skG = . skM+
            Triple(RP(skG, OMI, EOR), lhs_U, LHS(a)),      // skG = skM+ .
            Triple(RP(skG, 0, EOR), lhs_U, LHS(UP)),   // skG = skM . skM+
            Triple(RP(skG, 0, SOR), lhs_U, LHS(a)),    // skG = skM+ .
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),     // skM = . WS
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),     // skM = WS .
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),     // skM = . CM
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP))      // skM = CM .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_U.part)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun sk0_widthInto() {
        val actual = sk0.widthInto(null).toList()

        val expected = listOf(
            WidthInfo(RulePosition(skWS, 0, RulePosition.END_OF_RULE), lhs_skWCU.part),
            WidthInfo(RulePosition(skCM, 0, RulePosition.END_OF_RULE), lhs_skWCU.part)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = sk1.heightOrGraftInto(sk0).toList()

        val expected = listOf(
            HeightGraftInfo(emptyList(),
                listOf(RulePosition(skC, 0, RulePosition.START_OF_RULE)),
                listOf(RulePosition(skC, 0, RulePosition.END_OF_RULE)),
                lhs_WS_CM_UP.part,
                setOf(LHS(skWS,skCM,UP))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun parse_aba() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "aba", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {


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