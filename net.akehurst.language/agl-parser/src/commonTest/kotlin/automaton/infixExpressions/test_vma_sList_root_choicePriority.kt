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
package net.akehurst.language.automaton.leftcorner.infixExpressions

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.AutomatonTest
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_vma_sList_root_choicePriority : test_AutomatonAbstract() {

    // S =  E ;
    // E = R < M < A ;
    // R = V ;
    // A = [ E / 'a' ]2+ ;
    // M = [ E / 'm' ]2+ ;
    // V = 'v'
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("R")
            ref("M")
            ref("A")
        }
        concatenation("R") { ref("V") }
        concatenation("V") { literal("v") }
        sList("M", 2, -1, "E", "'m'")
        sList("A", 2, -1, "E", "'a'")
        literal("'m'", "m")
        literal("'a'", "a")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val R = rrs.findRuntimeRule("R")
    private val rM = rrs.findRuntimeRule("M")
    private val rA = rrs.findRuntimeRule("A")
    private val m = rrs.findRuntimeRule("'m'")
    private val a = rrs.findRuntimeRule("'a'")
    private val v = rrs.findRuntimeRule("'v'")


    @Test
    fun automaton_parse_v() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "v")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SR))     /* G = . S */
            val s1 = state(RP(v, oN, EOR))     /* v .     */
            val s2 = state(RP(R, oN, EOR))     /* R = v .     */
            val s3 = state(RP(E, oN, EOR))     /* E = R . */
            val s4 = state(RP(S, oN, EOR))     /* S = E . */
            val s5 = state(RP(rA, oSI, PLS))   /* A = [ E . 'a'... ]2+ */
            val s6 = state(RP(rM, oSI, PLS))   /* M = [ E . 'm'... ]2+ */
            val s7 = state(RP(G, oN, EOR))     /* G = . S   */

            trans(WIDTH) { ctx(G, oN, SOR); src(G, oN, SOR); tgt(v); lhg(setOf(EOT, m, a)) }
            trans(GOAL) { ctx(G, oN, SOR); src(S); tgt(G); lhg(setOf(EOT)) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E); tgt(rA, oSI, PLS); lhg(setOf(a), setOf(EOT, m, a)) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(R); tgt(E); lhg(m, m); lhg(EOT, EOT); lhg(a, a) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E); tgt(rM, oSI, PLS); lhg(setOf(m), setOf(EOT, m, a)) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(v); tgt(R); lhg(m, m); lhg(EOT, EOT); lhg(a, a) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E); tgt(S); lhg(EOT, EOT); }

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vavav() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vavav")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))      /* G = . S   */
            val s2 = state(RP(v, oN, ER))      /* 'v' .   */
            val s4 = state(RP(R, oN, ER))   /* root = vr .   */
            val s5 = state(RP(E, oN, ER))      /* E = root .   */
            val s6 = state(RP(S, oN, ER))      /* S = E .   */

            val s8 = state(RP(m, oN, ER))        /* '/' . */
            val s9 = state(RP(a, oN, ER))        /* '/' . */
            val s10 = state(RP(rM, oN, PLI))    /* div = [E ... '/' . E ...]2+ */
            val s11 = state(RP(rM, oN, ER))     /* div = [E / '/']2+ . . */
            val s12 = state(RP(E, o1, ER))       /* E = div . */
            val s1 = state(RP(G, oN, ER))        /* G = S .   */

            transition(s0, s0, s1, WIDTH, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s2, s3, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s3, s4, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s4, s5, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s4, s6, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s5, s7, GRAFT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s7, s7, GOAL, setOf(EOT, m, a), emptySet(), null)

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vmvav() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vmvav")
        println(result.sppt!!.toStringAllWithIndent("  "))
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))      /* G = . S   */
            val s2 = state(RP(v, oN, ER))      /* 'v' .   */
            val s4 = state(RP(R, oN, ER))   /* root = vr .   */
            val s5 = state(RP(E, oN, ER))      /* E = root .   */
            val s6 = state(RP(S, oN, ER))      /* S = E .   */

            val s8 = state(RP(m, oN, ER))        /* '/' . */
            val s9 = state(RP(a, oN, ER))        /* '/' . */
            val s10 = state(RP(rM, oN, PLI))    /* div = [E ... '/' . E ...]2+ */
            val s11 = state(RP(rM, oN, ER))     /* div = [E / '/']2+ . . */
            val s12 = state(RP(E, o1, ER))       /* E = div . */
            val s1 = state(RP(G, oN, ER))        /* G = S .   */

            transition(s0, s0, s1, WIDTH, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s2, s3, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s3, s4, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s4, s5, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s4, s6, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s5, s7, GRAFT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s7, s7, GOAL, setOf(EOT, m, a), emptySet(), null)

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vavmvav() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vavmvav")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))      /* G = . S   */
            val s2 = state(RP(v, oN, ER))      /* 'v' .   */
            val s4 = state(RP(R, oN, ER))   /* root = vr .   */
            val s5 = state(RP(E, oN, ER))      /* E = root .   */
            val s6 = state(RP(S, oN, ER))      /* S = E .   */

            val s8 = state(RP(m, oN, ER))        /* '/' . */
            val s9 = state(RP(a, oN, ER))        /* '/' . */
            val s10 = state(RP(rM, oN, PLI))    /* div = [E ... '/' . E ...]2+ */
            val s11 = state(RP(rM, oN, ER))     /* div = [E / '/']2+ . . */
            val s12 = state(RP(E, o1, ER))       /* E = div . */
            val s1 = state(RP(G, oN, ER))        /* G = S .   */

            transition(s0, s0, s1, WIDTH, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s2, s3, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s3, s4, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s4, s5, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            transition(s0, s4, s6, HEIGHT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s5, s7, GRAFT, setOf(EOT, m, a), emptySet(), null)
            //transition(s0, s7, s7, GOAL, setOf(EOT, m, a), emptySet(), null)

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "v/v")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))      /* G = . S */
            val s1 = state(RP(G, oN, ER))      /* G = S . */
            val s2 = state(RP(S, oN, ER))      /* S = E . */
            val s3 = state(RP(E, oN, ER))      /* E = R . */
            val s4 = state(RP(E, o1, ER))      /* E = M . */
            val s5 = state(RP(E, o2, ER))      /* E = A . */
            val s6 = state(RP(rA, oSI, ER))    /* A = [E / 'a' ]2+ . */
            val s7 = state(RP(rM, oSI, ER))    /* M = [E / 'm' ]2+ . */
            val s8 = state(RP(a, oN, ER))      /* 'a' . */
            val s9 = state(RP(R, oN, ER))      /* R = 'v' . */
            val s10 = state(RP(v, oN, ER))     /* 'v' . */
            val s11 = state(RP(m, oN, ER))     /* 'm' . */
            val s12 = state(RP(rA, oSI, PLS))   /* A = [E . 'a'...]2+  */
            val s13 = state(RP(rA, oSI, PLI))   /* A = [E 'a' . E...]2+  */
            val s14 = state(RP(rM, oSI, PLS))   /* M = [E . 'm'...]2+  */
            val s15 = state(RP(rM, oSI, PLI))   /* M = [E 'm' . E...]2+  */

            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(rA, oSI, PLS); tgt(a); lhg(v) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(rM, oSI, PLS); tgt(m); lhg(v) }
            trans(WIDTH) { ctx(G, oN, SOR); src(G, oN, SOR); tgt(v); lhg(setOf(EOT, m, a)) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(rA, oSI, PLI); tgt(v); lhg(setOf(EOT, a, m)) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(rM, oSI, PLI); tgt(v); lhg(setOf(EOT, a, m)) }
            trans(GOAL) { ctx(G, oN, SOR); src(S); tgt(G); lhg(EOT) }
            trans(GRAFT) { ctx(rA, oSI, PLI); src(E, oN, ER); tgt(rA, oSI, ER); lhg(setOf(EOT, a, m)); gpg(rA, oSI, PLI) }
            trans(GRAFT) { ctx(rA, oSI, PLI); src(E, o1, ER); tgt(rA, oSI, ER); lhg(setOf(EOT, a, m)); gpg(rA, oSI, PLI) }
            trans(GRAFT) { ctx(rA, oSI, PLI); src(E, o2, ER); tgt(rA, oSI, ER); lhg(setOf(EOT, a, m)); gpg(rA, oSI, PLI) }
            trans(GRAFT) { ctx(rA, oSI, PLS); src(a); tgt(rA, oSI, PLI); lhg(v); gpg(rA, oSI, PLS) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(E, oN, ER); tgt(rA, oSI, PLS); lhg(setOf(a), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rA, oSI, PLI); src(E, oN, ER); tgt(rA, oSI, PLS); lhg(a); gpg(rA, oSI, PLI) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(E, o1, ER); tgt(rA, oSI, PLS); lhg(setOf(a), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rA, oSI, PLI); src(E, o1, ER); tgt(rA, oSI, PLS); lhg(a); gpg(rA, oSI, PLI) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(E, o2, ER); tgt(rA, oSI, PLS); lhg(setOf(a), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rA, oSI, PLI); src(E, o2, ER); tgt(rA, oSI, PLS); lhg(a); gpg(rA, oSI, PLI) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(R); tgt(E, oN, ER); lhg(setOf(EOT, a, m), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(rM, oSI, ER); tgt(E, o1, ER); lhg(setOf(EOT, a, m), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(rA, oSI, ER); tgt(E, o2, ER); lhg(setOf(EOT, a, m), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rM, oSI, PLI); src(E, oN, ER); tgt(rM, oSI, ER); lhg(setOf(EOT, a, m)); gpg(rM, oSI, PLI) }
            trans(GRAFT) { ctx(rM, oSI, PLI); src(E, o1, ER); tgt(rM, oSI, ER); lhg(setOf(EOT, a, m)); gpg(rM, oSI, PLI) }
            trans(GRAFT) { ctx(rM, oSI, PLI); src(E, o2, ER); tgt(rM, oSI, ER); lhg(setOf(EOT, a, m)); gpg(rM, oSI, PLI) }
            trans(GRAFT) { ctx(rM, oSI, PLS); src(m); tgt(rM, oSI, PLI); lhg(v); gpg(rM, oSI, PLS) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(E, oN, ER); tgt(rM, oSI, PLS); lhg(setOf(m), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rM, oSI, PLI); src(E, oN, ER); tgt(rM, oSI, PLS); lhg(m); gpg(rM, oSI, PLI) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(E, o1, ER); tgt(rM, oSI, PLS); lhg(setOf(m), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rM, oSI, PLI); src(E, o1, ER); tgt(rM, oSI, PLS); lhg(m); gpg(rM, oSI, PLI) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(E, o2, ER); tgt(rM, oSI, PLS); lhg(setOf(m), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rM, oSI, PLI); src(E, o2, ER); tgt(rM, oSI, PLS); lhg(m); gpg(rM, oSI, PLI) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oSI, PLI), RP(rM, oSI, PLI)); src(v); tgt(R); lhg(setOf(EOT, a, m), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, oN, ER); tgt(S); lhg(setOf(EOT), setOf(EOT)) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, o1, ER); tgt(S); lhg(setOf(EOT), setOf(EOT)) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, o2, ER); tgt(S); lhg(setOf(EOT), setOf(EOT)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vavmv", "vmvav")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println("$sen: $it") }
        }
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")

        println("--No Build Run--")
        val result_noBuild = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild).parseForGoal("S", "vmvav")
        println(result_noBuild.sppt!!.toStringAllWithIndent("  "))

        println("--Build Run--")
        val result_build = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_preBuild.nonSkipTerminals), rrs_preBuild).parseForGoal("S", "vmvav")
        println(result_build.sppt!!.toStringAllWithIndent("  "))

        println("--Build SM--")
        println(rrs_preBuild.usedAutomatonToString("S"))

        println("--No Build SM--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}