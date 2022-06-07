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
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_da_sList_root_choicePriority : test_AutomatonAbstract() {

    // S =  E ;
    // E = root < div < add ;
    // root = var < bool ;
    // add = [ E / '+' ]2+ ;
    // div = [ E / '/' ]2+ ;
    // var = 'v' ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("root")
            ref("div")
            ref("add")
        }
        choice("root", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("var")
            //ref("bool")
        }
        sList("div", 2, -1, "E", "'/'")
        sList("add", 2, -1, "E", "'+'")
        concatenation("var") { literal("v") }
        literal("'/'", "/")
        literal("'+'", "+")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val root = rrs.findRuntimeRule("root")
    private val div = rrs.findRuntimeRule("div")
    private val add = rrs.findRuntimeRule("add")
    private val d = rrs.findRuntimeRule("'/'")
    private val a = rrs.findRuntimeRule("'+'")
    private val v = rrs.findRuntimeRule("'v'")
    private val vr = rrs.findRuntimeRule("var")

    private val lhs_da = LHS(d, a).lhs(SM)

    @Test
    fun automaton_parse_v() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "v", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S */
            val s1 = state(RP(v, 0, EOR))     /* v .     */
            val s2 = state(RP(vr, 0, EOR))    /* var = v .     */
            val s3 = state(RP(root, 0, EOR))  /* root = var .     */
            val s4 = state(RP(E, 0, EOR))     /* E = v . */
            val s5 = state(RP(S, 0, EOR))     /* S = E . */
            val s6 = state(
                RP(div, 0, 1),
                RP(add, 0, 1)
            ) /* div = E . '/' ... | div = E . '+' ...  */
            val s7 = state(RP(G, 0, EOR))     /* G = . S   */


            transition(s0, s0, s1, WIDTH, setOf(UP, d, a), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(UP, d, a), setOf(setOf(UP), setOf(d), setOf(a)), listOf(RP(vr,0,SOR)))
            transition(s0, s2, s3, HEIGHT, setOf(UP, d, a), setOf(setOf(UP), setOf(d), setOf(a)), listOf(RP(root,0,SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(UP, d, a), setOf(setOf(UP), setOf(d), setOf(a)), listOf(RP(E,0,SOR)))
            transition(s0, s4, s5, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S,0,SOR)))
            transition(s0, s4, s6, HEIGHT, setOf( d, a), setOf(setOf(UP), setOf(d), setOf(a)), listOf(RP(div,0,SOR),RP(add,0,SOR)))
            transition(s0, s5, s7, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G,0,SOR)))


        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vdv() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "v/v", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))      /* G = . S   */
            val s2 = state(RP(v, 0, EOR))      /* 'v' .   */
            val s3 = state(RP(vr, 0, EOR))     /* var = "[a-z]+" .   */
            val s4 = state(RP(root, 0, EOR))   /* root = vr .   */
            val s5 = state(RP(E, 0, EOR))      /* E = root .   */
            val s6 = state(RP(S, 0, EOR))      /* S = E .   */
            val s7 = state(
                RP(div, 0, PLS),
                RP(add, 0, PLS)
            )               /* div = [E . '/' ...]2+ | div = [E . '+' ...]+2  */
            val s8 = state(RP(d, 0, EOR))        /* '/' . */
            val s9 = state(RP(a, 0, EOR))        /* '/' . */
            val s10 = state(RP(div, 0, PLI))    /* div = [E ... '/' . E ...]2+ */
            val s11 = state(RP(div, 0, EOR))     /* div = [E / '/']2+ . . */
            val s12 = state(RP(E, 1, EOR))       /* E = div . */
            val s1 = state(RP(G, 0, EOR))        /* G = S .   */

            transition(s0, s0, s1, WIDTH, setOf(UP, d, a), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s2, s3, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s3, s4, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s4, s5, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s4, s6, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s5, s7, GRAFT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s7, s7, GOAL, setOf(UP, d, a), emptySet(), null)

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "v/v", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))      /* G = . S   */
            val s1 = state(RP(v, 0, EOR))      /* 'v' .   */
            val s2 = state(RP(vr, 0, EOR))     /* var = "[a-z]+" .   */
            val s3 = state(RP(root, 0, EOR))   /* root = vr .   */
            val s4 = state(RP(E, 0, EOR))      /* E = root .   */
            val s5 = state(RP(S, 0, EOR))      /* S = E .   */
            val s6 = state(
                RP(div, 0, PLS),
                RP(add, 0, PLS)
            )               /* div = [E . '/' ...]2+ | div = [E . '+' ...]+2  */
            val s7 = state(RP(G, 0, EOR))        /* G = S .   */
            val s8 = state(RP(d, 0, EOR))        /* '/' . */
            val s9 = state(RP(a, 0, EOR))        /* '/' . */
            val s10 = state(RP(div, 0, PLI))    /* div = [E ... '/' . E ...]2+ */
            val s11 = state(RP(add, 0, PLI))     /* add = [E ... '+' . E ...]2+ */
            val s12 = state(RP(div, 0, EOR))     /* div = [E / '/']2+ . . */
            val s13 = state(RP(add, 0, EOR))     /* add = [E / '+']2+ . . */
            val s14 = state(RP(E, 1, EOR))       /* E = div . */
            val s15 = state(RP(E, 2, EOR))       /* E = add . */

            transition(s0, s0, s1, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(UP, d, a), emptySet(), null)
            transition(s0, s0, s1, HEIGHT, setOf(UP, d, a), emptySet(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("v","v+v","v+v+v","v/v","v/v/v","v+v/v","v/v+v")
        for(sen in sentences) {
            val (sppt, issues) = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (issues.isNotEmpty())  issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}