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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_b_aSc : test_AutomatonAbstract() {

    /*
        S = b | a S c ;
     */

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        choiceLongest("S") {
            concatenation { literal("b") }
            concatenation { literal("a"); ref("S"); literal("c") }
        }
    } as RuntimeRuleSet

    private val _t0 = rrs.rule[0]  // 'b'
    private val _t1 = rrs.rule[1]  // 'a'
    private val _t2 = rrs.rule[2]  // 'c'
    private val S = rrs.rule[3]  // S
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun automaton_parse_b() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "b")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'a'
            state(S, o0, ER)   // S = 'b' .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'a'
            state(S, o1, 1)   // S = 'a' . S 'c'
            state(S, o0, ER)   // S = 'b' .
            state(S, o1, 2)   // S = 'a' S . 'c'
            state(_t2, oN, ER)   // 'c'
            state(S, o1, ER)   // S = 'a' S 'c' .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(_t2), setOf(_t2)); ctx(S, o1, 1); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, o1, 1); lhg(setOf(_t0, _t1), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t0, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(S, o0, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 2); tgt(_t2, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, o1, ER); lhg(RT); ctx(S, o1, 2); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "b")
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'a'
            state(S, o0, ER)   // S = 'b' .
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o1, 1)   // S = 'a' . S 'c'
            state(S, o1, 2)   // S = 'a' S . 'c'
            state(_t2, oN, ER)   // 'c'
            state(S, o1, ER)   // S = 'a' S 'c' .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(_t2), setOf(_t2)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(rG, oN, SR), RP(S, o1, 1)); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, o1, 1); lhg(setOf(_t0, _t1), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GRAFT) { src(S, o0, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t0, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 2); tgt(_t2, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, o1, ER); lhg(RT); ctx(S, o1, 2); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_aabcc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aabcc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'a'
            state(S, o1, 1)   // S = 'a' . S 'c'
            state(S, o0, ER)   // S = 'b' .
            state(S, o1, 2)   // S = 'a' S . 'c'
            state(_t2, oN, ER)   // 'c'
            state(S, o1, ER)   // S = 'a' S 'c' .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(_t2), setOf(_t2)); ctx(S, o1, 1); pctx(S, o1, 1) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, o1, 1); lhg(setOf(_t0, _t1), setOf(EOT, _t2)); ctx(RP(rG, oN, SR), RP(S, o1, 1)); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t0, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(GRAFT) { src(S, o0, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(S, o1, 1) }
            trans(WIDTH) { src(S, o1, 2); tgt(_t2, oN, ER); lhg(RT); ctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, o1, ER); lhg(RT); ctx(S, o1, 2); pctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc_aabcc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "b")
        parser.parseForGoal("S", "abc")
        val result = parser.parseForGoal("S", "aabcc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'a'
            state(S, o0, ER)   // S = 'b' .
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o1, 1)   // S = 'a' . S 'c'
            state(S, o1, 2)   // S = 'a' S . 'c'
            state(_t2, oN, ER)   // 'c'
            state(S, o1, ER)   // S = 'a' S 'c' .


            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(_t2), setOf(_t2)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(rG, oN, SR), RP(S, o1, 1)); pctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, o1, 1); lhg(setOf(_t0, _t1), setOf(EOT, _t2)); ctx(RP(rG, oN, SR), RP(S, o1, 1)); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GRAFT) { src(S, o0, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t0, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(WIDTH) { src(S, o1, 2); tgt(_t2, oN, ER); lhg(RT); ctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, o1, ER); lhg(RT); ctx(S, o1, 2); pctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(rG, oN, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("b", "abc", "aabcc")
        sentences.forEach {
            println(it)
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.toString())
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o0, ER)   // S = 'b' .
            state(S, o1, 1)   // S = 'a' . S 'c'
            state(S, o1, 2)   // S = 'a' S . 'c'
            state(S, o1, ER)   // S = 'a' S 'c' .
            state(_t0, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'a'
            state(_t2, oN, ER)   // 'c'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(S, o0, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t0, oN, ER); lhg(_t2); ctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S, o1, 1); tgt(_t1, oN, ER); lhg(setOf(_t0, _t1)); ctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S, o1, 2); tgt(_t2, oN, ER); lhg(setOf(EOT, _t2)); ctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, 2); lhg(_t2); ctx(S, o1, 1); pctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT, _t2), setOf(EOT, _t2)); ctx(RP(rG, oN, SR), RP(S, o1, 1)); pctx(RP(rG, oN, SR), RP(S, o1, 1)) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, o1, 1); lhg(setOf(_t0, _t1), setOf(EOT, _t2)); ctx(RP(S, o1, 1), RP(rG, oN, SR)); pctx(RP(S, o1, 1), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT, _t2)); ctx(S, o1, 2); pctx(RP(S, o1, 1), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("b", "abc", "aabcc", "aaabccc")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
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
