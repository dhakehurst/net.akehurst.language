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

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuleOptionPosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhs
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
        val skC = skM.rhs.items[RuntimeRuleRhs.MULTI__ITEM]  // skC = WS | CM
        val skWS = rrs.findRuntimeRule("WS")
        val skCM = rrs.findRuntimeRule("COMMENT")

        val sk1 = skipSS.createState(listOf(RuleOptionPosition(skWS, 0, RuleOptionPosition.END_OF_RULE)))

        val lhs_a = SM.createLookaheadSet(false,false, false,setOf(a))
        val lhs_skWCU = SM.createLookaheadSet(true, false, false,setOf(skWS, skCM))
        val lhs_aT = SM.createLookaheadSet(false, true, false,setOf(a))
        val lhs_WS_CM_UP = SM.createLookaheadSet(true, false, false,setOf(skWS, skCM))
    }

    @Test
    fun firstTerminals() {
        //TODO
        var actual = skipSS.firstTerminals[RuleOptionPosition(skG, 0, 0)]
        var expected = listOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RuleOptionPosition(skM, 0, 0)]
        expected = listOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RuleOptionPosition(skM, 0, RuleOptionPosition.POSITION_MULIT_ITEM)]
        expected = listOf(skWS, skCM)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RuleOptionPosition(skC, 0, 0)]
        expected = listOf(skWS)
        assertEquals(expected, actual)

        actual = skipSS.firstTerminals[RuleOptionPosition(skC, 1, 0)]
        expected = listOf(skCM)
        assertEquals(expected, actual)


    }

/* TODO
    @Test
    fun sk0_widthInto() {
        val actual = sk0.widthInto(RuntimeState(sk0, setOf(LookaheadSet.EMPTY))).toList()

        val expected = listOf(
            WidthInfo(RuleOptionPosition(skWS, 0, RuleOptionPosition.END_OF_RULE), lhs_skWCU.part),
            WidthInfo(RuleOptionPosition(skCM, 0, RuleOptionPosition.END_OF_RULE), lhs_skWCU.part)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = sk1.heightOrGraftInto(RuntimeState(sk0, setOf(LookaheadSet.EMPTY))).toList()

        val expected = listOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RuleOptionPosition(skC, 0, RuleOptionPosition.START_OF_RULE)),
                listOf(RuleOptionPosition(skC, 0, RuleOptionPosition.END_OF_RULE)),
                setOf(LookaheadInfoPart(LHS(skWS,skCM,UP),LHS(skWS,skCM,UP)))
            )
        )
        assertEquals(expected, actual)

    }
*/
    @Test
    fun parse_aba() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "aba", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}