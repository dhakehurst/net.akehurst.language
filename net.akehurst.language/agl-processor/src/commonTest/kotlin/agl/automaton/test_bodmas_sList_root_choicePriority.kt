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
import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_bodmas_sList_root_choicePriority : test_AutomatonAbstract() {

    // S =  E ;
    // E = root < grp < div < mul < add < sub ;
    // root = var < bool ;
    // sub = [ E / '-' ]2+ ;
    // add = [ E / '+' ]2+ ;
    // mul = [ E / '*' ]2+ ;
    // div = [ E / '/' ]2+ ;
    // grp = '(' E ')' ;
    // bool = 'true' | 'false' ;
    // var = 'v' ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("root")
            //ref("grp")
            ref("div")
            //ref("mul")
            ref("add")
            //ref("sub")
        }
        choice("root", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("var")
            //ref("bool")
        }
        sList("div", 2, -1, "E", "'/'")
        sList("mul", 2, -1, "E", "'*'")
        sList("add", 2, -1, "E", "'+'")
        sList("sub", 2, -1, "E", "'-'")
        concatenation("grp") { literal("("); ref("E"); literal(")") }
        choice("bool", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("true");literal("false") }
        concatenation("var") { literal("v") }
        literal("'/'", "/")
        literal("'*'", "*")
        literal("'+'", "+")
        literal("'-'", "-")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val root = rrs.findRuntimeRule("root")
    private val div = rrs.findRuntimeRule("div")
    private val mul = rrs.findRuntimeRule("mul")
    private val add = rrs.findRuntimeRule("add")
    private val sub = rrs.findRuntimeRule("sub")
    private val grp = rrs.findRuntimeRule("grp")
    private val d = rrs.findRuntimeRule("'/'")
    private val m = rrs.findRuntimeRule("'*'")
    private val a = rrs.findRuntimeRule("'+'")
    private val s = rrs.findRuntimeRule("'-'")
    private val v = rrs.findRuntimeRule("'v'")
    private val t = rrs.findRuntimeRule("'true'")
    private val f = rrs.findRuntimeRule("'false'")
    private val o = rrs.findRuntimeRule("'('")
    private val c = rrs.findRuntimeRule("')'")
    private val vr = rrs.findRuntimeRule("var")

    private val lhs_dmas = LHS(d,m,a,s).lhs(SM)

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
            val s2 = state(RP(E, 0, EOR))     /* E = v . */
            val s3 = state(RP(S, 0, EOR))     /* S = E . */
            val s7 = state(RP(G, 0, EOR))     /* G = . S   */


            //transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)

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
            val s1 = state(RP(v, 0, EOR))      /* 'v' .   */
            val s2 = state(RP(t, 0, EOR))      /* 'true' .   */
            val s3 = state(RP(f, 0, EOR))      /* 'false' .   */
            val s4 = state(RP(o, 0, EOR))      /* '(' .   */
            val s5 = state(RP(vr, 0, EOR))     /* var = "[a-z]+" .   */
            val s6 = state(RP(root, 0, EOR))   /* root = vr .   */
            val s7 = state(RP(E, 0, EOR))      /* E = root .   */
            val s8 = state(RP(S, 0, EOR))      /* S = E .   */
                                                    /* div = E . '/' ... | div = E . '*' ... | div = E . '+' ... | div = E . '-' ...   */
            val s9 = state(RP(div, 0, 1),RP(mul, 0, 1),RP(add, 0, 1),RP(sub, 0, 1))


            //transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)

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