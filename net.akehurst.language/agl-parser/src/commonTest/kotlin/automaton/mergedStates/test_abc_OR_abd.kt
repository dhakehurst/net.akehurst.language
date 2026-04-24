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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_abc_OR_abd : test_AutomatonAbstract() {

    // S =  ABC | ABD
    // ABC = a b c
    // ABD = a b d

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        choiceLongest("S") {
            ref("ABC")
            ref("ABD")
        }
        concatenation("ABC") { literal("a"); literal("b"); literal("c") }
        concatenation("ABD") { literal("a"); literal("b"); literal("d") }
    }  as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val _t1 = rrs.rule[1]  // 'a'
    private val _t2 = rrs.rule[2]  // 'b'
    private val _t3 = rrs.rule[3]  // 'c'
    private val ABC = rrs.rule[4]  // ABC
    private val _t5 = rrs.rule[5]  // 'd'
    private val ABD = rrs.rule[6]  // ABD
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun automaton_parse_abc() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)      /* G = . S   */
            state(_t1, oN, ER)     /* a .       */
            state(ABC, oN, p1)     /* ABC = a . bc */
            state( ABD, oN, p1)    /* ABD = a . bd */
            state(_t2, oN, EOR)    /* b . */
            //state(ABD, oN, p2)     /* ABD = ab . d  */
            state(ABC, oN, p2)     /* ABC = ab . c  */
            state(_t3, oN, EOR)    /* c . */
            state(ABC, oN, EOR)    /* ABC = abc . */
            state(S, oN, EOR)      /* S = ABC . */
            state(rG, oN, EOR)     /* G = S .   */

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1); lhg(_t2); ctx(rG, oN, SR) }
//            trans(WIDTH) { src(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))); tgt(_t2); lhg(setOf(_t5, _t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p2); tgt(_t3); lhg(RT); ctx(rG, oN, SR) }

            trans(GOAL) { src(S); tgt(rG); lhg(EOT); ctx(rG, oN, SR) }

//            trans(GRAFT) { src(_t2); tgt(ABC, oN, p2); lhg(_t3); ctx(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))) }
            trans(GRAFT) { src(_t3); tgt(ABC); lhg(RT); ctx(ABC, oN, p2) }
//            trans(HEIGHT) { src(_t1); tgt(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR) }
//            trans(GRAFT) { src(_t2); tgt(ABD, oN, p2); lhg(_t5); ctx(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))) }
            trans(HEIGHT) { src(ABC); tgt(S); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR) }

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abd() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abd")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)     /* G = . S   */
            state(_t1, oN, ER)     /* a .       */
            state(ABD, oN, p1)   /* ABD = a . bd */
            state(ABC, oN, p1)   /* ABC = a . bc */
            state(_t2, oN, ER)     /* b . */
            state(ABD, oN, p2)   /* ABD = ab . d  */
            state(_t5, oN, ER)     /* d . */
            state(ABD, oN, ER)   /* ABD = abd . */
            state(S, o1, ER)     /* S = ABD . */
            state(rG, oN, ER)     /* G = S .   */

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1); lhg(_t2); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p1); tgt(_t2); lhg(_t3); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABD, oN, p1); tgt(_t2); lhg(_t5); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABD, oN, p2); tgt(_t5); lhg(RT); ctx(rG, oN, SR) }

            trans(GOAL) { src(S, o1, ER); tgt(rG); lhg(EOT); ctx(rG, oN, SR) }

            trans(HEIGHT) { src(_t1); tgt(ABC, oN, p1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t5); tgt(ABD); lhg(RT); ctx(ABD, oN, p2) }
            trans(HEIGHT) { src(_t1); tgt(ABD, oN, p1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2); tgt(ABD, oN, p2); lhg(_t5); ctx(ABD, oN, p1) }
            trans(HEIGHT) { src(ABD); tgt(S, o1, ER); lhg(setOf(RT), setOf(RT)); ctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("abc", "abd")
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
            state(S, o0, ER)   // S = ABC .
            state(S, o1, ER)   // S = ABD .
            state(ABC, oN, 1)   // ABC = 'a' . 'b' 'c'
            state(ABC, oN, 2)   // ABC = 'a' 'b' . 'c'
            state(ABC, oN, ER)   // ABC = 'a' 'b' 'c' .
            state(ABD, oN, 1)   // ABD = 'a' . 'b' 'd'
            state(ABD, oN, 2)   // ABD = 'a' 'b' . 'd'
            state(ABD, oN, ER)   // ABD = 'a' 'b' 'd' .
            state(_t1, oN, ER)   // 'a'
            state(_t2, oN, ER)   // 'b'
            state(_t3, oN, ER)   // 'c'
            state(_t5, oN, ER)   // 'd'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABC, oN, 1); tgt(_t2, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABC, oN, 2); tgt(_t3, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ABC, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ABD, oN, 1); tgt(_t2, oN, ER); lhg(_t5); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ABD, oN, 2); tgt(_t5, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ABD, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABC, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(ABD, oN, 1); lhg(setOf(_t2), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABC, oN, 2); lhg(_t3); ctx(ABC, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(ABD, oN, 2); lhg(_t5); ctx(ABD, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(ABC, oN, ER); lhg(EOT); ctx(ABC, oN, 2); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t5, oN, ER); tgt(ABD, oN, ER); lhg(EOT); ctx(ABD, oN, 2); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = (rrs).clone()  as RuntimeRuleSet
        val rrs_preBuild = (rrs).clone() as RuntimeRuleSet

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abc", "abd")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")as ParserStateSet
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(
            automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(
                in_actual_substitue_lookahead_RT_with = setOf(EOT)
            )
        )
    }
}