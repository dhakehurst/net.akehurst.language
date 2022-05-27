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
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_leftRecursive : test_AutomatonAbstract() {

    // S =  'a' | S1
    // S1 = S 'a'

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("a")
            ref("S1")
        }
        concatenation("S1") { ref("S"); literal("a") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val S1 = rrs.findRuntimeRule("S1")
    private val a = rrs.findRuntimeRule("'a'")

    private val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))
    private val lhs_aU = SM.createLookaheadSet(true, false, false, setOf(a))

    @Test
    fun follow() {
        val ffc = FirstFollowCache(SM)
        listOf(
            listOf(RP(G, 0, SOR),RP(G, 0, SOR),RP(G, 0, SOR), a, LHS(UP, a)),     //  (G = . S) <-- (S = . a)  <==  a
                                                                                                //  (G = . S)    <==  S
            listOf(RP(G, 0, SOR),RP(G, 0, SOR),RP(G, 0, SOR), S, LHS(UP, a)),     //  (G = . S) <-- (S = . S1) <-- (S1 = . S a)  <==  S
            listOf(RP(G, 0, SOR),RP(G, 0, SOR),RP(G, 0, SOR), S1, LHS(UP,a)),     //  (G = . S) <-- (S = . S1)   <==  S1
            listOf(RP(G, 0, SOR),RP(G, 0, SOR),RP(S1, 0, 1), a, LHS(UP)),    // (S1 = S . a)  <==  a
            listOf(RP(G, 0, SOR),RP(G, 0, SOR),RP(G, 0, SOR), G, LHS(UP)),        // (G = . S)  <==  G
        ).testAll { list ->
            val procPrev = list[0] as RulePosition
            val procRp = list[1] as RulePosition
            val followPrev = list[2] as RulePosition
            val followRr = list[3] as RuntimeRule
            val expected = list[4] as LookaheadSetPart
            println("($procPrev, $procRp, $followPrev, ${followRr.tag})")
            ffc.processClosureFor(procPrev, procRp, listOf(),true)
            val actual = LHS(ffc.followAtEndInContext(followPrev, followRr).toSet())
            assertEquals(expected, actual, "failed ($procPrev, $procRp, $followPrev, ${followRr.tag})")
        }
    }

    @Test
    fun parse_a() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S    */
            val s1 = state(RP(a, 0, EOR))     /* a .        */
            val s2 = state(RP(S, 0, EOR))     /* S = a .    */
            val s3 = state(RP(G, 0, EOR))     /* G = S .    */
            val s4 = state(RP(S1, 0, 1)) /* S1 = S . a */

            transition(s0, s0, s1, WIDTH, setOf(UP,a), emptySet(),null)
            transition(s0, s1, s2, HEIGHT, setOf(UP,a), setOf(setOf(UP),setOf(a)), listOf(RP(S,0,SOR)))
            transition(s0, s2, s4, HEIGHT, setOf(a), setOf(setOf(UP),setOf(a)),listOf(RP(S1,0,SOR)))
            transition(s0, s2, s3, GRAFT, setOf(UP), setOf(setOf(UP)),listOf(RP(G,0,SOR)))
            transition(s0, s3, s3, GOAL, emptySet(), emptySet(),null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aa() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aa", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S    */
            val s1 = state(RP(a, 0, EOR))     /* a .        */
            val s2 = state(RP(S, 0, EOR))     /* S = a .    */
            val s3 = state(RP(G, 0, EOR))     /* G = S .    */
            val s4 = state(RP(S1, 0, 1)) /* S1 = S . a */
            val s5 = state(RP(S1, 0, EOR))    /* S1 = S a . */
            val s6 = state(RP(S, 1, EOR))     /* S = S1 . */

        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aaa() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aaa", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* {0}    G = . S    */
            val s1 = state(RP(a, 0, EOR))     /* {0,4}  a .        */
            val s2 = state(RP(S, 0, EOR))     /* {0}    S = a .    */
            val s3 = state(RP(G, 0, EOR))     /* {0}    G = S .    */
            val s4 = state(RP(S1, 0, 1)) /* {0}    S1 = S . a */
            val s5 = state(RP(S1, 0, EOR))    /* {0}    S1 = S a . */
            val s6 = state(RP(S, 1, EOR))     /* {0}    S = S1 .   */

            transition(s0, s0, s1, WIDTH, setOf(UP,a), emptySet(),null)
            transition(s0, s1, s2, HEIGHT, setOf(UP,a), setOf(setOf(UP),setOf(a)), listOf(RP(S,0,SOR)))
            transition(s4, s1, s5, GRAFT, setOf(UP,a), setOf(setOf(UP,a)),listOf(RP(S1,0,1)))
            transition(s0, s2, s4, HEIGHT, setOf(a), setOf(setOf(UP),setOf(a)),listOf(RP(S1,0,SOR)))
            transition(s0, s2, s3, GRAFT, setOf(UP), setOf(setOf(UP)),listOf(RP(G,0,SOR)))
            transition(s0, s3, s3, GOAL, emptySet(), emptySet(),null)
            transition(s0, s4, s1, WIDTH, setOf(UP), emptySet(),null)
            transition(s0, s5, s6, HEIGHT, setOf(UP,a), setOf(setOf(UP),setOf(a)), listOf(RP(S,1,SOR)))
            transition(s0, s6, s4, HEIGHT, setOf(a), setOf(setOf(UP),setOf(a)), listOf(RP(S1,0,SOR)))
            transition(s0, s6, s3, GRAFT, setOf(UP), setOf(setOf(UP)),listOf(RP(G,0,SOR)))
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun stateInfo() {
        val bc = BuildCacheLC1(SM)

        val actual = bc.stateInfo2()
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aaa", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))     /* {}     G = . S    */
            val s1 = state(RP(G, 0, EOR))     /* {}     G = S .    */
            val s2 = state(RP(S, 0, EOR))     /* {0}    S = a .    */
            val s3 = state(RP(S, 1, EOR))     /* {0}    S = S1 .   */
            val s4 = state(RP(a, 0, EOR))     /* {0,5}  a .        */
            val s5 = state(RP(S1, 0, 1)) /* {0}    S1 = S . a */
            val s6 = state(RP(S1, 0, EOR))    /* {0}    S1 = S a . */
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }

        val rrs_preBuild = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("a","aa","aaa","aaaa")
        for(sen in sentences) {
            val (sppt, issues) = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (issues.isNotEmpty())  issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--No Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--Pre Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}