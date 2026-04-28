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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_bodmas_expreOpRules_root_choiceEqual : test_AutomatonAbstract() {

    // S =  E ;
    // E = R | M | A ;
    // R = 'v'           root = var
    // M = E 'm' E ;     mul = expr * expr
    // A = E 'a' E ;     add = expr + expr

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choiceLongest("E") {
            ref("R")
            ref("M")
            ref("A")
        }
        concatenation("R") { literal("v") }
        concatenation("M") { ref("E"); literal("m"); ref("E") }
        concatenation("A") { ref("E"); literal("a"); ref("E") }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val E = rrs.rule[1]  // E
    private val _t2 = rrs.rule[2]  // 'v'
    private val R = rrs.rule[3]  // R
    private val _t4 = rrs.rule[4]  // 'm'
    private val M = rrs.rule[5]  // M
    private val _t6 = rrs.rule[6]  // 'a'
    private val A = rrs.rule[7]  // A
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun automaton_parse_v() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "v")
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(R, oN, ER)   // R = 'v' .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, oN, 1)   // A = E . 'a' E
            state(M, oN, 1)   // M = E . 'm' E
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(EOT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(EOT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vmv() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vmv")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(R, oN, ER)   // R = 'v' .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, oN, 1)   // A = E . 'a' E
            state(M, oN, 1)   // M = E . 'm' E
            state(_t4, oN, ER)   // 'm'
            state(M, oN, 2)   // M = E 'm' . E
            state(M, oN, ER)   // M = E 'm' E .
            state(E, o1, ER)   // E = M .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(RT,EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(RT,EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(GRAFT) { src(E, o0, ER); tgt(M, oN, ER); lhg(RT); ctx(M, oN, 2); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(M, oN, 1); tgt(_t4, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(M, oN, 2); lhg(_t2); ctx(M, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(M, oN, 2); tgt(_t2, oN, ER); lhg(setOf(RT,_t4,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(M, oN, ER); tgt(E, o1, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(RT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(RT,_t4,_t6)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_sentences() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val sentences = listOf("v", "vav", "vavav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        //val sentences = listOf( "vav")
        sentences.forEach {
            println(it)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(R, oN, ER)   // R = 'v' .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, oN, 1)   // A = E . 'a' E
            state(M, oN, 1)   // M = E . 'm' E
            state(rG, oN, ER)   // <GOAL> = S .
            state(_t6, oN, ER)   // 'a'
            state(A, oN, 2)   // A = E 'a' . E
            state(A, oN, ER)   // A = E 'a' E .
            state(E, o2, ER)   // E = A .
            state(_t4, oN, ER)   // 'm'
            state(M, oN, 2)   // M = E 'm' . E
            state(M, oN, ER)   // M = E 'm' E .
            state(E, o1, ER)   // E = M .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(RT,EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(E, o0, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(RT,EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(GRAFT) { src(E, o0, ER); tgt(A, oN, ER); lhg(RT); ctx(A, oN, 2); pctx(RP(rG, oN, SR),RP(M, oN, 2)) }
            trans(GRAFT) { src(E, o0, ER); tgt(M, oN, ER); lhg(RT); ctx(M, oN, 2); pctx(RP(rG, oN, SR),RP(A, oN, 2)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            // because GRAFT is done before HEIGHT, RP(A,0,2) never becomes context for this trans, even though prebuild allows for it
            trans(WIDTH) { src(A, oN, 1); tgt(_t6, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR),RP(M, oN, 2)) }
            trans(WIDTH) { src(M, oN, 1); tgt(_t4, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR),RP(A, oN, 2)) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(A, oN, 2); lhg(_t2); ctx(A, oN, 1); pctx(RP(rG, oN, SR),RP(M, oN, 2)) }
            trans(WIDTH) { src(A, oN, 2); tgt(_t2, oN, ER); lhg(setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)) }
            trans(HEIGHT) { src(A, oN, ER); tgt(E, o2, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o2, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o2, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2)); pctx(rG, oN, SR) }
            trans(GRAFT) { src(E, o2, ER); tgt(M, oN, ER); lhg(RT); ctx(M, oN, 2); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(M, oN, 2); lhg(_t2); ctx(M, oN, 1); pctx(RP(rG, oN, SR),RP(A, oN, 2)) }
            trans(WIDTH) { src(M, oN, 2); tgt(_t2, oN, ER); lhg(setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2)) }
            trans(HEIGHT) { src(M, oN, ER); tgt(E, o1, ER); lhg(setOf(RT,_t4,_t6), setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(RT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2)); pctx(rG, oN, SR) }
            trans(GRAFT) { src(E, o1, ER); tgt(A, oN, ER); lhg(RT); ctx(A, oN, 2); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = E .
            state(E, o0, ER)   // E = R .
            state(E, o1, ER)   // E = M .
            state(E, o2, ER)   // E = A .
            state(R, oN, ER)   // R = 'v' .
            state(M, oN, 1)   // M = E . 'm' E
            state(M, oN, 2)   // M = E 'm' . E
            state(M, oN, ER)   // M = E 'm' E .
            state(A, oN, 1)   // A = E . 'a' E
            state(A, oN, 2)   // A = E 'a' . E
            state(A, oN, ER)   // A = E 'a' E .
            state(_t2, oN, ER)   // 'v'
            state(_t4, oN, ER)   // 'm'
            state(_t6, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o0, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(E, o0, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(GRAFT) { src(E, o0, ER); tgt(M, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(M, oN, 2); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(GRAFT) { src(E, o0, ER); tgt(A, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(A, oN, 2); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o1, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(E, o1, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(GRAFT) { src(E, o1, ER); tgt(M, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(M, oN, 2); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(GRAFT) { src(E, o1, ER); tgt(A, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(A, oN, 2); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(E, o2, ER); tgt(A, oN, 1); lhg(setOf(_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
            trans(HEIGHT) { src(E, o2, ER); tgt(M, oN, 1); lhg(setOf(_t4), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
            trans(GRAFT) { src(E, o2, ER); tgt(A, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(A, oN, 2); pctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
            trans(GRAFT) { src(E, o2, ER); tgt(M, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(M, oN, 2); pctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
            trans(HEIGHT) { src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(WIDTH) { src(M, oN, 1); tgt(_t4, oN, ER); lhg(_t2); ctx(RP(M, oN, 2),RP(A, oN, 2),RP(rG, oN, SR)) }
            trans(WIDTH) { src(M, oN, 2); tgt(_t2, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(M, oN, ER); tgt(E, o1, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)); pctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
            trans(WIDTH) { src(A, oN, 1); tgt(_t6, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
            trans(WIDTH) { src(A, oN, 2); tgt(_t2, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(RP(A, oN, 2),RP(rG, oN, SR),RP(M, oN, 2)) }
            trans(HEIGHT) { src(A, oN, ER); tgt(E, o2, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(M, oN, 2),RP(A, oN, 2),RP(rG, oN, SR)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6)); ctx(RP(M, oN, 2),RP(A, oN, 2),RP(rG, oN, SR)); pctx(RP(rG, oN, SR),RP(M, oN, 2),RP(A, oN, 2)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(M, oN, 2); lhg(_t2); ctx(M, oN, 1); pctx(RP(M, oN, 2),RP(A, oN, 2),RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(A, oN, 2); lhg(_t2); ctx(A, oN, 1); pctx(RP(rG, oN, SR),RP(A, oN, 2),RP(M, oN, 2)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, config = AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}