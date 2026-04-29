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

package net.akehurst.language.automaton.leftcorner.embedded

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test

class test_embedded : test_AutomatonAbstract() {

    /*
    B = b ;

    S = a gB a ;
    gB = B::B ;
 */
    val rrsB = ruleSet("rrsB") {
        concatenation("B") { literal("b") }
    } as RuntimeRuleSet
    val rrs = ruleSet("Test") {
        concatenation("S") { literal("a"); ref("gB"); literal("a"); }
        embedded("gB", rrsB, "B")
    } as RuntimeRuleSet

    private val _t0 = rrs.rule[0]  // 'a'
    private val S = rrs.rule[1]  // S
    private val _t2 = rrs.rule[2]  // gB
    private val rG = rrs.goalRuleFor[S]

    private val B__t0 = rrsB.rule[0]  // 'b'
    private val B_B = rrsB.rule[1]  // B
    private val B_rG = rrsB.goalRuleFor[B_B]

    @Test
    fun parse_aba() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "aba")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        println(rrsB.usedAutomatonToString("B"))
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'a'
            state(S, oN, 1)   // S = 'a' . gB 'a'
            state(_t2, oN, ER)   // gB(EMBED ::B)
            state(S, oN, 2)   // S = 'a' gB . 'a'
            state(S, oN, ER)   // S = 'a' gB 'a' .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(B__t0); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, oN, 1); lhg(setOf(B__t0), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t0, oN, ER); tgt(S, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(EMBED) { src(S, oN, 1); tgt(_t2, oN, ER); lhg(_t0); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, oN, 2); lhg(_t0);  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t0, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actualB = rrsB.buildFor("B", AutomatonKind.LOOKAHEAD_1)
        println(rrsB.usedAutomatonToString("B"))

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expectedB = automaton(rrsB, AutomatonKind.LOOKAHEAD_1, "B", false) {
            state(B_rG, oN, SR)   // <GOAL> =  . B
            state(B_rG, oN, ER)   // <GOAL> = B .
            state(B_B, oN, ER)   // B = 'b' .
            state(B__t0, oN, ER)   // 'b'

            trans(WIDTH) { src(B_rG, oN, SR); tgt(B__t0, oN, ER); lhg(EOT); ctx(B_rG, oN, SR) }
            trans(GOAL) { src(B_B, oN, ER); tgt(B_rG, oN, ER); lhg(EOT);  prevPair(RP(B_rG, oN, SR), RP(B_rG, oN, SR)) }
            trans(HEIGHT) { src(B__t0, oN, ER); tgt(B_B, oN, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(B_rG, oN, SR), RP(B_rG, oN, SR)) }
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, 1)   // S = 'a' . gB 'a'
            state(S, oN, 2)   // S = 'a' gB . 'a'
            state(S, oN, ER)   // S = 'a' gB 'a' .
            state(_t0, oN, ER)   // 'a'
            state(_t2, oN, ER)   // gB(EMBED ::B)

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(B__t0); ctx(rG, oN, SR) }
            trans(EMBED) { src(S, oN, 1); tgt(_t2, oN, ER); lhg(_t0); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t0, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, oN, 1); lhg(setOf(B__t0), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t0, oN, ER); tgt(S, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, oN, 2); lhg(_t0);  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
        }

        AutomatonTest.assertEquals(expectedB, actualB)
        AutomatonTest.assertEquals(expected, actual)
    }
}