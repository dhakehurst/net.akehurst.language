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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_sList_compositeMulti : test_AutomatonAbstract() {

    // S = [nl / ';']*
    // nl = N cnm
    // cnm = cn*
    // cn = ',' N
    // N = "[0-9]+"

    private companion object {

        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "nl", "SMI")
            concatenation("nl") { ref("N"); ref("cnm") }
            multi("cnm", 0, -1, "cn")
            concatenation("cn") { ref("CMR"); ref("N"); }
            literal("CMR", ",")
            literal("SMI", ";")
            pattern("N", "[0-9]+")
        }

        val S = rrs.findRuntimeRule("S")
        val Se = S.rhs.items[RuntimeRuleItem.SLIST__EMPTY_RULE]
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val nl = rrs.findRuntimeRule("nl")
        val cnm = rrs.findRuntimeRule("cnm")
        val cn = rrs.findRuntimeRule("cn")
        val c = rrs.findRuntimeRule("CMR")
        val i = rrs.findRuntimeRule("SMI")
        val n = rrs.findRuntimeRule("N")

        val lhs_n = SM.createLookaheadSet(false, false, false,setOf(n))
        val lhs_c = SM.createLookaheadSet(false,false, false,setOf(c))
        val lhs_i = SM.createLookaheadSet(false,false, false,setOf(i))
        val lhs_nU = SM.createLookaheadSet(true,false, false, setOf(n))
        val lhs_ciU = SM.createLookaheadSet(true, false, false,setOf(c, i))

        val s0 = SM.startState
        val s1 = SM.createState(listOf(RP(n, 0, EOR)))
        val s2 = SM.createState(listOf(RP(Se, 0, EOR)))
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