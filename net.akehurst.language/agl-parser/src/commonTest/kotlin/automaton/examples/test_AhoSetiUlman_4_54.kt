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

class test_AhoSetiUlman_4_54 : test_AutomatonAbstract() {
    // S = C C ;
    // C = c C | d ;
    //
    // S = C C ;
    // C = C1 | d ;
    // C1 = c C ;
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("C"); ref("C") }
        choiceLongest("C") {
            ref("C1")
            literal("d")
        }
        concatenation("C1") { literal("c"); ref("C") }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val _t1 = rrs.rule[1]  // 'd'
    private val C = rrs.rule[2]  // C
    private val _t3 = rrs.rule[3]  // 'c'
    private val C1 = rrs.rule[4]  // C1
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_dd() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "dd")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t3, oN, ER)   // 'c'
            state(_t1, oN, ER)   // 'd'
            state(C, o1, ER)   // C = 'd' .
            state(S, oN, 1)   // S = C . C
            state(S, oN, ER)   // S = C C .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(_t3,_t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t3,_t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(C, o1, ER); lhg(setOf(_t3,_t1), setOf(_t3,_t1)); lhg(setOf(RT), setOf(RT));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
            }
            trans(HEIGHT) { src(C, o1, ER); tgt(S, oN, 1); lhg(setOf(_t3,_t1), setOf(EOT));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GRAFT) { src(C, o1, ER); tgt(S, oN, ER); lhg(RT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
            }
            trans(WIDTH) { src(S, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t3,_t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t1, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
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
            state(S, oN, 1)   // S = C . C
            state(S, oN, ER)   // S = C C .
            state(C, o0, ER)   // C = C1 .
            state(C, o1, ER)   // C = 'd' .
            state(C1, oN, 1)   // C1 = 'c' . C
            state(C1, oN, ER)   // C1 = 'c' C .
            state(_t3, oN, ER)   // 'c'
            state(_t1, oN, ER)   // 'd'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(_t3, _t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(_t3, _t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t3, _t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t1, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(C, o0, ER); tgt(S, oN, 1); lhg(setOf(_t3, _t1), setOf(EOT));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GRAFT) {
                src(C, o0, ER); tgt(S, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
            }
            trans(GRAFT) {
                src(C, o0, ER); tgt(C1, oN, ER); lhg(setOf(EOT, _t3, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(C1, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(S, oN, 1)), setOf(RP(C1, oN, 1)))
            }
            trans(HEIGHT) {
                src(C, o1, ER); tgt(S, oN, 1); lhg(setOf(_t3, _t1), setOf(EOT));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GRAFT) {
                src(C, o1, ER); tgt(S, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
            }
            trans(GRAFT) {
                src(C, o1, ER); tgt(C1, oN, ER); lhg(setOf(EOT, _t3, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(C1, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(S, oN, 1)), setOf(RP(C1, oN, 1)))
            }
            trans(WIDTH) { src(C1, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t3, _t1)); ctx(RP(rG, oN, SR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(WIDTH) { src(C1, oN, 1); tgt(_t1, oN, ER); lhg(setOf(EOT, _t3, _t1)); ctx(RP(rG, oN, SR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(HEIGHT) {
                src(C1, oN, ER); tgt(C, o0, ER); lhg(setOf(EOT, _t3, _t1), setOf(EOT, _t3, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(C1, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(S, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
            }
            trans(HEIGHT) {
                src(_t3, oN, ER); tgt(C1, oN, 1); lhg(setOf(_t3, _t1), setOf(EOT, _t3, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(C1, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(S, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(_t1, oN, ER); tgt(C, o1, ER); lhg(setOf(EOT, _t3, _t1), setOf(EOT, _t3, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(C1, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(S, oN, 1)), setOf(RP(C1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S, oN, 1)))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }
}