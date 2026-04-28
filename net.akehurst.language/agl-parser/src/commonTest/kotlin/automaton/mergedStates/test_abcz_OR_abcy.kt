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

class test_abcz_OR_abcy : test_AutomatonAbstract() {

    // S =  ABCZ | ABCY
    // ABCZ = a b c z
    // ABCY = a b c y

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        choiceLongest("S") {
            ref("ABCZ")
            ref("ABCY")
        }
        concatenation("ABCZ") { literal("a"); literal("b"); literal("c"); literal("z") }
        concatenation("ABCY") { literal("a"); literal("b"); literal("c"); literal("y") }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val _t1 = rrs.rule[1]  // 'a'
    private val _t2 = rrs.rule[2]  // 'b'
    private val _t3 = rrs.rule[3]  // 'c'
    private val _t4 = rrs.rule[4]  // 'z'
    private val ABCZ = rrs.rule[5]  // ABCZ
    private val _t6 = rrs.rule[6]  // 'y'
    private val ABCY = rrs.rule[7]  // ABCY
    private val rG = rrs.goalRuleFor[S]

    //private val lhs_b = SM.createLookaheadSet(false, false, false, setOf(b))

    @Test
    fun automaton_parse_abcz() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abcz")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(ABCZ, oN, 1)   // ABCZ = 'a' . 'b' 'c' 'z'
            state(ABCY, oN, 1)   // ABCY = 'a' . 'b' 'c' 'y'
            state(_t2, oN, ER)   // 'b'
            state(ABCZ, oN, 2)   // ABCZ = 'a' 'b' . 'c' 'z'
            state(ABCY, oN, 2)   // ABCY = 'a' 'b' . 'c' 'y'
            state(_t3, oN, ER)   // 'c'
            state(ABCZ, oN, 3)   // ABCZ = 'a' 'b' 'c' . 'z'
            state(_t4, oN, ER)   // 'z'
            state(ABCZ, oN, ER)   // ABCZ = 'a' 'b' 'c' 'z' .
            state(S, o0, ER)   // S = ABCZ .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABCZ, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABCY, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABCZ, oN, 2); lhg(_t3); ctx(ABCZ, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABCY, oN, 2); lhg(_t3); ctx(ABCY, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 2); tgt(_t3, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 2); tgt(_t3, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(ABCZ, oN, 3); lhg(_t4); ctx(ABCZ, oN, 2); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 3); tgt(_t4, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ABCZ, oN, ER); lhg(RT); ctx(ABCZ, oN, 3); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(ABCZ, oN, ER); tgt(S, o0, ER); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abcy() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abcy")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(ABCZ, oN, 1)   // ABCZ = 'a' . 'b' 'c' 'z'
            state(ABCY, oN, 1)   // ABCY = 'a' . 'b' 'c' 'y'
            state(_t2, oN, ER)   // 'b'
            state(ABCZ, oN, 2)   // ABCZ = 'a' 'b' . 'c' 'z'
            state(ABCY, oN, 2)   // ABCY = 'a' 'b' . 'c' 'y'
            state(_t3, oN, ER)   // 'c'
            state(ABCY, oN, 3)   // ABCY = 'a' 'b' 'c' . 'y'
            state(_t6, oN, ER)   // 'y'
            state(ABCY, oN, ER)   // ABCY = 'a' 'b' 'c' 'y' .
            state(S, o1, ER)   // S = ABCY .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABCZ, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABCY, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABCZ, oN, 2); lhg(_t3); ctx(ABCZ, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABCY, oN, 2); lhg(_t3); ctx(ABCY, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 2); tgt(_t3, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 2); tgt(_t3, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(ABCY, oN, 3); lhg(_t6); ctx(ABCY, oN, 2); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 3); tgt(_t6, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(ABCY, oN, ER); lhg(RT); ctx(ABCY, oN, 3); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(ABCY, oN, ER); tgt(S, o1, ER); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("abcz", "abcy")
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
            state(S, o0, ER)   // S = ABCZ .
            state(S, o1, ER)   // S = ABCY .
            state(ABCZ, oN, 1)   // ABCZ = 'a' . 'b' 'c' 'z'
            state(ABCZ, oN, 2)   // ABCZ = 'a' 'b' . 'c' 'z'
            state(ABCZ, oN, 3)   // ABCZ = 'a' 'b' 'c' . 'z'
            state(ABCZ, oN, ER)   // ABCZ = 'a' 'b' 'c' 'z' .
            state(ABCY, oN, 1)   // ABCY = 'a' . 'b' 'c' 'y'
            state(ABCY, oN, 2)   // ABCY = 'a' 'b' . 'c' 'y'
            state(ABCY, oN, 3)   // ABCY = 'a' 'b' 'c' . 'y'
            state(ABCY, oN, ER)   // ABCY = 'a' 'b' 'c' 'y' .
            state(_t1, oN, ER)   // 'a'
            state(_t2, oN, ER)   // 'b'
            state(_t3, oN, ER)   // 'c'
            state(_t4, oN, ER)   // 'z'
            state(_t6, oN, ER)   // 'y'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 2); tgt(_t3, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCZ, oN, 3); tgt(_t4, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ABCZ, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 2); tgt(_t3, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABCY, oN, 3); tgt(_t6, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ABCY, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABCZ, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABCY, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABCY, oN, 2); lhg(_t3); ctx(ABCY, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABCZ, oN, 2); lhg(_t3); ctx(ABCZ, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(ABCZ, oN, 3); lhg(_t4); ctx(ABCZ, oN, 2); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(ABCY, oN, 3); lhg(_t6); ctx(ABCY, oN, 2); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ABCZ, oN, ER); lhg(EOT); ctx(ABCZ, oN, 3); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(ABCY, oN, ER); lhg(EOT); ctx(ABCY, oN, 3); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abcy", "abcz")
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild,config = AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}