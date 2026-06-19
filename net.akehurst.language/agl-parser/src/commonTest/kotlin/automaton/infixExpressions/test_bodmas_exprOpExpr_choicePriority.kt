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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_bodmas_exprOpExpr_choicePriority : test_AutomatonAbstract() {

    // S = E
    // E = v < EA < EB
    // EA = E a E
    // EB = E b E

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choicePriority("E") {
            ref("'v'")
            ref("EA")
            ref("EB")
        }
        literal("'v'", "v")
        concatenation("EA") { ref("E"); literal("a"); ref("E") }
        concatenation("EB") { ref("E"); literal("b"); ref("E") }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val E = rrs.rule[1]  // E
    private val _t2 = rrs.rule[2]  // 'v'
    private val _t3 = rrs.rule[3]  // 'a'
    private val EA = rrs.rule[4]  // EA
    private val _t5 = rrs.rule[5]  // 'b'
    private val EB = rrs.rule[6]  // EB
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
            state(E, o0, ER)   // E = 'v' .
            state(S, oN, ER)   // S = E .
            state(EB, oN, 1)   // EB = E . 'b' E
            state(EA, oN, 1)   // EA = E . 'a' E
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT,_t3,_t5)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t3,_t5), setOf(EOT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(EB, oN, 1); lhg(setOf(_t5), setOf(EOT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(EA, oN, 1); lhg(setOf(_t3), setOf(EOT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vav() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vav")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(E, o0, ER)   // E = 'v' .
            state(S, oN, ER)   // S = E .
            state(EB, oN, 1)   // EB = E . 'b' E
            state(EA, oN, 1)   // EA = E . 'a' E
            state(_t3, oN, ER)   // 'a'
            state(EA, oN, 2)   // EA = E 'a' . E
            state(EA, oN, ER)   // EA = E 'a' E .
            state(E, o1, ER)   // E = EA .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT,_t3,_t5)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(E, o0, ER); lhg(setOf(RT,_t3,_t5), setOf(RT,_t3,_t5)); lhg(setOf(EOT,_t3,_t5), setOf(EOT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(EA, oN, 1); lhg(setOf(_t3), setOf(RT,EOT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(EB, oN, 1); lhg(setOf(_t5), setOf(RT,EOT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
            }
            trans(GRAFT) { src(E, o0, ER); tgt(EA, oN, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
            }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(EA, oN, 1); tgt(_t3, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(EA, oN, 2); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(EA, oN, 1))
            }
            trans(WIDTH) { src(EA, oN, 2); tgt(_t2, oN, ER); lhg(setOf(RT,_t3,_t5)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(EA, oN, ER); tgt(E, o1, ER); lhg(setOf(RT,_t3,_t5), setOf(RT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(EB, oN, 1); lhg(setOf(_t5), setOf(RT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(EA, oN, 1); lhg(setOf(_t3), setOf(RT,_t3,_t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        // then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = E .
            state(E, o0, ER)   // E = 'v' .
            state(E, o1, ER)   // E = EA .
            state(E, o2, ER)   // E = EB .
            state(EA, oN, 1)   // EA = E . 'a' E
            state(EA, oN, 2)   // EA = E 'a' . E
            state(EA, oN, ER)   // EA = E 'a' E .
            state(EB, oN, 1)   // EB = E . 'b' E
            state(EB, oN, 2)   // EB = E 'b' . E
            state(EB, oN, ER)   // EB = E 'b' E .
            state(_t2, oN, ER)   // 'v'
            state(_t3, oN, ER)   // 'a'
            state(_t5, oN, ER)   // 'b'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT, _t3, _t5)); ctx(rG, oN, SR) }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(EA, oN, 1); lhg(setOf(_t3), setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(EB, oN, 1); lhg(setOf(_t5), setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(EB, oN, ER); lhg(setOf(EOT, _t3, _t5));
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(EA, oN, ER); lhg(setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(EA, oN, 1); lhg(setOf(_t3), setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(EB, oN, 1); lhg(setOf(_t5), setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(EA, oN, ER); lhg(setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(EB, oN, ER); lhg(setOf(EOT, _t3, _t5));
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(EA, oN, 1); lhg(setOf(_t3), setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(EB, oN, 1); lhg(setOf(_t5), setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(EA, oN, ER); lhg(setOf(EOT, _t3, _t5));
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(EB, oN, ER); lhg(setOf(EOT, _t3, _t5));
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
            }
            trans(WIDTH) { src(EA, oN, 1); tgt(_t3, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR), RP(EA, oN, 2), RP(EB, oN, 2)) }
            trans(WIDTH) { src(EA, oN, 2); tgt(_t2, oN, ER); lhg(setOf(EOT, _t3, _t5)); ctx(RP(rG, oN, SR), RP(EA, oN, 2), RP(EB, oN, 2)) }
            trans(HEIGHT) {
                src(EA, oN, ER); tgt(E, o1, ER); lhg(setOf(EOT, _t3, _t5), setOf(EOT, _t3, _t5));
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
            }
            trans(WIDTH) { src(EB, oN, 1); tgt(_t5, oN, ER); lhg(_t2); ctx(RP(EA, oN, 2), RP(rG, oN, SR), RP(EB, oN, 2)) }
            trans(WIDTH) { src(EB, oN, 2); tgt(_t2, oN, ER); lhg(setOf(EOT, _t3, _t5)); ctx(RP(EA, oN, 2), RP(rG, oN, SR), RP(EB, oN, 2)) }
            trans(HEIGHT) {
                src(EB, oN, ER); tgt(E, o2, ER); lhg(setOf(EOT, _t3, _t5), setOf(EOT, _t3, _t5));
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(_t2, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT, _t3, _t5), setOf(EOT, _t3, _t5));
                prevPair(RP(EA, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 2))
                prevPair(RP(rG, oN, SR), RP(EA, oN, 2))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 2))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) {
                src(_t3, oN, ER); tgt(EA, oN, 2); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(EA, oN, 1))
                prevPair(RP(EA, oN, 2), RP(EA, oN, 1))
                prevPair(RP(EB, oN, 2), RP(EA, oN, 1))
            }
            trans(GRAFT) {
                src(_t5, oN, ER); tgt(EB, oN, 2); lhg(_t2);
                prevPair(RP(EA, oN, 2), RP(EB, oN, 1))
                prevPair(RP(rG, oN, SR), RP(EB, oN, 1))
                prevPair(RP(EB, oN, 2), RP(EB, oN, 1))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }
}