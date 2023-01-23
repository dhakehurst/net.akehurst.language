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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test

internal class test_sList_0_n_literal : test_AutomatonAbstract() {
    // S =  ['a' / 'b']* ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        sList("S", 0, -1, "'a'", "'b'")
        literal("'a'", "a")
        literal("'b'", "b")
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val G = SM.startState.runtimeRules.first()

    private val s0 = SM.startState

    private val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))

    @Test
    fun parse_empty() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G,o0,SR)  // G=.S
            state(G,o0,ER)  // G=S.
            state(a,o0,ER)  // a
            state(EMPTY,o0,ER)  // <empty>
            state(S,OLE,ER) // S=[EMPTY].

            trans(WIDTH) { src(G,o0,SR); tgt(EMPTY); lhg(EOT); ctx(G,o0,SR)  }
            trans(WIDTH) { src(G,o0,SR); tgt(a); lhg(setOf(EOT,b)); ctx(G,o0,SR)  }
            trans(GOAL) { src(S,OLE,ER); tgt(G); lhg(EOT); ctx(G,o0,SR)  }
            trans(HEIGHT) { src(EMPTY); tgt(S,OLE,ER); lhg(setOf(EOT),setOf(EOT)); ctx(G,o0,SR)  }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_a() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

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
    fun parse_empty_and_abababa() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "", AutomatonKind.LOOKAHEAD_1)
        parser.parseForGoal("S", "abababa", AutomatonKind.LOOKAHEAD_1)
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

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("","a","aba","ababa", "abababa")
        for(sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty())  result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(automaton_preBuild, automaton_noBuild,AutomatonTest.MatchConfiguration(
            in_actual_substitue_lookahead_RT_with = setOf(EOT)
        ))
    }
}