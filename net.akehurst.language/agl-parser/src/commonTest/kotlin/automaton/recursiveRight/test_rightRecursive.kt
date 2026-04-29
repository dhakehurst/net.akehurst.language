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

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
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

class test_rightRecursive : test_AutomatonAbstract() {

    // S =  'a' | S1 ;
    // S1 = 'a' S ;

    val rrs = ruleSet("Test") {
        choiceLongest("S") {
            literal("a")
            ref("S1")
        }
        concatenation("S1") { literal("a"); ref("S") }
    } as RuntimeRuleSet

    private val _t0 = rrs.rule[0]  // 'a'
    private val S = rrs.rule[1]  // S
    private val S1 = rrs.rule[2]  // S1
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_a_aa_aaa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "a")
        parser.parseForGoal("S", "aa")
        parser.parseForGoal("S", "aaa")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'a'
            state(S, o0, ER)   // S = 'a' .
            state(S1, oN, 1)   // S1 = 'a' . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S1, oN, ER)   // S1 = 'a' S .
            state(S, o1, ER)   // S = S1 .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(RT), setOf(RT)); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)); prevPair(RP(S1, oN, 1), RP(S1, oN, 1)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S1, oN, 1); lhg(setOf(_t0), setOf(RT,EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)); prevPair(RP(S1, oN, 1), RP(S1, oN, 1)) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o0, ER); tgt(S1, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(S1, oN, 1)); prevPair(RP(S1, oN, 1), RP(S1, oN, 1)) }
            trans(WIDTH) { src(S1, oN, 1); tgt(_t0, oN, ER); lhg(setOf(RT,_t0)); ctx(RP(rG, oN, SR),RP(S1, oN, 1)) }
            trans(HEIGHT) { src(S1, oN, ER); tgt(S, o1, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o1, ER); tgt(S1, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o0, ER)   // S = 'a' .
            state(S, o1, ER)   // S = S1 .
            state(S1, oN, 1)   // S1 = 'a' . S
            state(S1, oN, ER)   // S1 = 'a' S .
            state(_t0, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(S, o0, ER); tgt(S1, oN, ER); lhg(EOT);  prevPair(RP(S1, oN, 1), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o1, ER); tgt(S1, oN, ER); lhg(EOT);  prevPair(RP(S1, oN, 1), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S1, oN, 1); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0)); ctx(RP(S1, oN, 1),RP(rG, oN, SR)) }
            trans(HEIGHT) { src(S1, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(S1, oN, 1), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(S1, oN, 1), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S1, oN, 1); lhg(setOf(_t0), setOf(EOT));  prevPair(RP(S1, oN, 1), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(S1, oN, 1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("a", "aa", "aaa", "aaaa")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S") as ParserStateSet
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(
            automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(
                no_lookahead_compare = true
            )
        )
    }
}