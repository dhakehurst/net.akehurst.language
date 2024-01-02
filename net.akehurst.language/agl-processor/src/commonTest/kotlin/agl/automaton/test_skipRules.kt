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

import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test

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

        //val skM = skG.rhs.items[0]                            // skS = skC+
        //val skC = skM.rhs.items[RuntimeRuleRhs.MULTI__ITEM]  // skC = WS | CM
        val skWS = rrs.findRuntimeRule("WS")
        val skCM = rrs.findRuntimeRule("COMMENT")

        val sk1 = skipSS.createState(listOf(RulePosition(skWS, 0, RulePosition.END_OF_RULE)))

        val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))
        val lhs_skWCU = SM.createLookaheadSet(true, false, false, setOf(skWS, skCM))
        val lhs_aT = SM.createLookaheadSet(false, true, false, setOf(a))
        val lhs_WS_CM_UP = SM.createLookaheadSet(true, false, false, setOf(skWS, skCM))
    }

    /* TODO
        @Test
        fun sk0_widthInto() {
            val actual = sk0.widthInto(RuntimeState(sk0, setOf(LookaheadSet.EMPTY))).toList()

            val expected = listOf(
                WidthInfo(RulePosition(skWS, 0, RulePosition.END_OF_RULE), lhs_skWCU.part),
                WidthInfo(RulePosition(skCM, 0, RulePosition.END_OF_RULE), lhs_skWCU.part)
            )
            assertEquals(expected, actual)
        }

        @Test
        fun s1_heightOrGraftInto_s0() {

            val actual = sk1.heightOrGraftInto(RuntimeState(sk0, setOf(LookaheadSet.EMPTY))).toList()

            val expected = listOf(
                HeightGraftInfo(
                    Transition.ParseAction.HEIGHT,
                    listOf(RulePosition(skC, 0, RulePosition.START_OF_RULE)),
                    listOf(RulePosition(skC, 0, RulePosition.END_OF_RULE)),
                    setOf(LookaheadInfoPart(LHS(skWS,skCM,UP),LHS(skWS,skCM,UP)))
                )
            )
            assertEquals(expected, actual)

        }
    */
    @Test
    fun parse_aba() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        parser.parseForGoal("S", "aba")
        val actual = parser.runtimeRuleSet.skipParserStateSet!!
        println(actual.usedAutomatonToString())
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