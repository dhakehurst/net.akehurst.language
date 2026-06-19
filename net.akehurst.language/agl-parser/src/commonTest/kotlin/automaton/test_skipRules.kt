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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test

class test_skipRules : test_AutomatonAbstract() {

    // skip WS = "\s+" ;
    // skip COMMENT = "//[^\n]*$"
    // S = 'a' ;

    val rrs = ruleSet("Test") {
        pattern("WS", "\\s+", true)
        pattern("COMMENT", "//[^\\n]*[\\n]", true)
        concatenation("S") { literal("a") }
    } as RuntimeRuleSet

    val _t0 = rrs.rule[0]  // WS
    val _t1 = rrs.rule[1]  // COMMENT
    val _t2 = rrs.rule[2]  // 'a'
    val S = rrs.rule[3]  // S
    val rG = rrs.skipParserStateSet!!.goalRule

    val rSKIP_MULTI = rrs.skipParserStateSet!!.userGoalRule
    val rSKIP_CHOICE = rrs.skipParserStateSet!!.userGoalRule.rhsItems[0][0]

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
    fun parse_COMMENT_WS_a_WS__SKIP() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        parser.parseForGoal("S", "//comment\na ")
        val actual = parser.runtimeRuleSet.skipParserStateSet!!
        println(actual.usedAutomatonToString())
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . rSKIP_MULTI
            state(_t0, oN, ER)   // WS("\s+")
            state(_t1, oN, ER)   // COMMENT("//[^\n]*[\n]")
            state(rSKIP_CHOICE, o1, ER)   // rSKIP_CHOICE = COMMENT .
            state(rSKIP_MULTI, LI, ER)   // [rSKIP_CHOICE] .
            state(rSKIP_MULTI, LI, 1)   // [rSKIP_CHOICE . rSKIP_CHOICE]
            state(rG, oN, ER)   // <GOAL> = rSKIP_MULTI .
            state(rSKIP_CHOICE, o0, ER)   // rSKIP_CHOICE = WS .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(setOf(EOT, _t0, _t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT, _t0, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(rSKIP_CHOICE, o0, ER); lhg(setOf(EOT, _t0, _t1), setOf(EOT, _t0, _t1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(rSKIP_CHOICE, o1, ER); lhg(setOf(EOT, _t0, _t1), setOf(EOT, _t0, _t1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o1, ER); tgt(rSKIP_MULTI, LI, ER); lhg(setOf(EOT), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o1, ER); tgt(rSKIP_MULTI, LI, 1); lhg(setOf(_t0, _t1), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GOAL) { src(rSKIP_MULTI, LI, ER); tgt(rG, oN, ER); lhg(EOT); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o0, ER); tgt(rSKIP_MULTI, LI, ER); lhg(setOf(EOT), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o0, ER); tgt(rSKIP_MULTI, LI, 1); lhg(setOf(_t0, _t1), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor__SKIP() {
        val actual = rrs.skipParserStateSet!!
        actual.build()
        println(actual.usedAutomatonToString())

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . rSKIP_MULTI
            state(_t0, oN, ER)   // WS("\s+")
            state(_t1, oN, ER)   // COMMENT("//[^\n]*[\n]")
            state(rSKIP_CHOICE, o1, ER)   // rSKIP_CHOICE = COMMENT . 
            state(rSKIP_MULTI, LI, ER)   // [rSKIP_CHOICE] .
            state(rSKIP_MULTI, LI, 1)   // [rSKIP_CHOICE . rSKIP_CHOICE]
            state(rG, oN, ER)   // <GOAL> = rSKIP_MULTI . 
            state(rSKIP_CHOICE, o0, ER)   // rSKIP_CHOICE = WS . 

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0,_t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t0,_t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(rSKIP_CHOICE, o0, ER); lhg(setOf(EOT,_t0,_t1), setOf(EOT,_t0,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(rSKIP_MULTI, oLI, 1)) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(rSKIP_CHOICE, o1, ER); lhg(setOf(EOT,_t0,_t1), setOf(EOT,_t0,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(rSKIP_MULTI, oLI, 1)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o1, ER); tgt(rSKIP_MULTI, LI, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o1, ER); tgt(rSKIP_MULTI, LI, 1); lhg(setOf(_t0,_t1), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(rSKIP_CHOICE, o1, ER); tgt(rSKIP_MULTI, LI, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rSKIP_MULTI, oLI, 1)) }
            trans(GRAFT) { src(rSKIP_CHOICE, o1, ER); tgt(rSKIP_MULTI, LI, 1); lhg(setOf(_t0,_t1));  prevPair(RP(rG, oN, SR), RP(rSKIP_MULTI, oLI, 1)) }
            trans(GOAL) { src(rSKIP_MULTI, LI, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(rSKIP_MULTI, LI, 1); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0,_t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rSKIP_MULTI, LI, 1); tgt(_t1, oN, ER); lhg(setOf(EOT,_t0,_t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o0, ER); tgt(rSKIP_MULTI, LI, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(rSKIP_CHOICE, o0, ER); tgt(rSKIP_MULTI, LI, 1); lhg(setOf(_t0,_t1), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(rSKIP_CHOICE, o0, ER); tgt(rSKIP_MULTI, LI, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rSKIP_MULTI, oLI, 1)) }
            trans(GRAFT) { src(rSKIP_CHOICE, o0, ER); tgt(rSKIP_MULTI, LI, 1); lhg(setOf(_t0,_t1));  prevPair(RP(rG, oN, SR), RP(rSKIP_MULTI, oLI, 1)) }        }

        AutomatonTest.assertEquals(expected, actual)
    }
}