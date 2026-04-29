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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonUtilsAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test

class test_buildFor : test_AutomatonUtilsAbstract() {

    @Test
    fun concatenation() {
        val rrs = ruleSet("Test") {
            concatenation("S") { literal("a"); literal("b"); literal("c") }
        } as RuntimeRuleSet

         val _t0 = rrs.rule[0]  // 'a'
         val _t1 = rrs.rule[1]  // 'b'
         val _t2 = rrs.rule[2]  // 'c'
         val S = rrs.rule[3]  // S
         val rG = rrs.goalRuleFor[S]

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, 1)   // S = 'a' . 'b' 'c'
            state(S, oN, 2)   // S = 'a' 'b' . 'c'
            state(S, oN, ER)   // S = 'a' 'b' 'c' .
            state(_t0, oN, ER)   // 'a'
            state(_t1, oN, ER)   // 'b'
            state(_t2, oN, ER)   // 'c'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t1, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t2, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, oN, 1); lhg(setOf(_t1), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, oN, 2); lhg(_t2);  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
        }
        AutomatonTest.assertMatches(expected, actual)
    }


    @Test
    fun leftRecursion() {
        val rrs = ruleSet("Test") {
            choiceLongest("S") {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        } as RuntimeRuleSet

         val _t0 = rrs.rule[0]  // 'a'
         val S = rrs.rule[1]  // S
         val S1 = rrs.rule[2]  // S1
         val rG = rrs.goalRuleFor[S]

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o0, ER)   // S = 'a' .
            state(S, o1, ER)   // S = S1 .
            state(S1, oN, 1)   // S1 = S . 'a'
            state(S1, oN, ER)   // S1 = S 'a' .
            state(_t0, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(S, o0, ER); tgt(S1, oN, 1); lhg(setOf(_t0), setOf(EOT,_t0));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(S, o1, ER); tgt(S1, oN, 1); lhg(setOf(_t0), setOf(EOT,_t0));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S1, oN, 1); tgt(_t0, oN, ER); lhg(setOf(EOT,_t0)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(S1, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT,_t0), setOf(EOT,_t0));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT,_t0), setOf(EOT,_t0));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t0, oN, ER); tgt(S1, oN, ER); lhg(setOf(EOT,_t0));  prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
        }
        AutomatonTest.assertMatches(expected, actual)
    }

    @Test
    fun nested() {
        val rrs = ruleSet("Test") {
            concatenation("S") { ref("S1") }
            concatenation("S1") { ref("M"); ref("S2") }
            multi("M", 0, -1, "'b'")
            concatenation("S2") { ref("S3") }
            concatenation("S3") { literal("a") }
            literal("'b'", "b")
        } as RuntimeRuleSet

         val S = rrs.rule[0]  // S
         val S1 = rrs.rule[1]  // S1
         val M = rrs.rule[2]  // M
         val S2 = rrs.rule[3]  // S2
         val _t4 = rrs.rule[4]  // 'a'
         val S3 = rrs.rule[5]  // S3
         val _t6 = rrs.rule[6]  // 'b'
         val rG = rrs.goalRuleFor[S]

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        parser.parseForGoal("S", "ba")
        parser.parseForGoal("S", "a")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = S1 .
            state(S1, oN, 1)   // S1 = M . S2
            state(S1, oN, ER)   // S1 = M S2 .
            state(M, LI, 1)   // ['b' . 'b']
            state(M, LI, ER)   // ['b'] .
            state(M, LE, ER)   // [EMPTY 'b'] .
            state(S2, oN, ER)   // S2 = S3 .
            state(S3, oN, ER)   // S3 = 'a' .
            state(_t6, oN, ER)   // 'b'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(_t4, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t6, oN, ER); lhg(setOf(_t6,_t4)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S1, oN, 1); tgt(_t4, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(S1, oN, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(M, LI, 1); tgt(_t6, oN, ER); lhg(setOf(_t6,_t4)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(M, LI, ER); tgt(S1, oN, 1); lhg(setOf(_t4), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(M, LE, ER); tgt(S1, oN, 1); lhg(setOf(_t4), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S2, oN, ER); tgt(S1, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
            trans(HEIGHT) { src(S3, oN, ER); tgt(S2, oN, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(M, LI, ER); lhg(setOf(_t4), setOf(_t4));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(M, LI, 1); lhg(setOf(_t6), setOf(_t4));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(M, LI, ER); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(M, oLI, 1)) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(M, LI, 1); lhg(_t6);  prevPair(RP(rG, oN, SR), RP(M, oLI, 1)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(M, LE, ER); lhg(setOf(_t4), setOf(_t4));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(S3, oN, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(S1, oN, 1)) }
        }
        AutomatonTest.assertMatches(expected, actual)
    }

    @Test
    fun xx() {
        val rrs = ruleSet("Test") {
            choiceLongest("S") {
                ref("ABCX");
                ref("S2")
            }
            concatenation("S2") { ref("S"); literal("y"); ref("S"); }
            concatenation("ABCX") { ref("ABCopt"); literal("x") }
            multi("ABCopt", 0, 1, "ABC")
            concatenation("ABC") { literal("a"); literal("b"); literal("c") }
        } as RuntimeRuleSet

         val S = rrs.rule[0]  // S
         val _t1 = rrs.rule[1]  // 'y'
         val S2 = rrs.rule[2]  // S2
         val _t3 = rrs.rule[3]  // 'x'
         val ABCX = rrs.rule[4]  // ABCX
         val ABCopt = rrs.rule[5]  // ABCopt
         val _t6 = rrs.rule[6]  // 'a'
         val _t7 = rrs.rule[7]  // 'b'
         val _t8 = rrs.rule[8]  // 'c'
         val ABC = rrs.rule[9]  // ABC
         val rG = rrs.goalRuleFor[S]

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        parser.parseForGoal("S", "abcx")
        parser.parseForGoal("S", "x")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o0, ER)   // S = ABCX .
            state(S, o1, ER)   // S = S2 .
            state(ABCX, oN, 1)   // ABCX = ABCopt . 'x'
            state(ABCX, oN, ER)   // ABCX = ABCopt 'x' .
            state(ABCopt, LI, ER)   // [ABC] .
            state(ABCopt, LE, ER)   // [EMPTY ABC] .
            state(ABC, oN, 1)   // ABC = 'a' . 'b' 'c'
            state(ABC, oN, 2)   // ABC = 'a' 'b' . 'c'
            state(ABC, oN, ER)   // ABC = 'a' 'b' 'c' .
            state(S2, oN, 1)   // S2 = S . 'y' S
            state(S2, oN, 2)   // S2 = S 'y' . S
            state(S2, oN, ER)   // S2 = S 'y' S .
            state(_t6, oN, ER)   // 'a'
            state(_t7, oN, ER)   // 'b'
            state(_t8, oN, ER)   // 'c'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(_t3, oN, ER)   // 'x'
            state(_t1, oN, ER)   // 'y'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t6, oN, ER); lhg(_t7); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(S, o0, ER); tgt(S2, oN, 1); lhg(setOf(_t1), setOf(EOT,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o0, ER); tgt(S2, oN, ER); lhg(setOf(EOT,_t1));  prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(HEIGHT) { src(S, o1, ER); tgt(S2, oN, 1); lhg(setOf(_t1), setOf(EOT,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(S, o1, ER); tgt(S2, oN, ER); lhg(setOf(EOT,_t1));  prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(WIDTH) { src(ABCX, oN, 1); tgt(_t3, oN, ER); lhg(setOf(EOT,_t1)); ctx(RP(rG, oN, SR),RP(S2, oN, 2)) }
            trans(HEIGHT) { src(ABCX, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT,_t1), setOf(EOT,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(HEIGHT) { src(ABCopt, LI, ER); tgt(ABCX, oN, 1); lhg(setOf(_t3), setOf(EOT,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(HEIGHT) { src(ABCopt, LE, ER); tgt(ABCX, oN, 1); lhg(setOf(_t3), setOf(EOT,_t1));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(WIDTH) { src(ABC, oN, 1); tgt(_t7, oN, ER); lhg(_t8); ctx(RP(S2, oN, 2),RP(rG, oN, SR)) }
            trans(WIDTH) { src(ABC, oN, 2); tgt(_t8, oN, ER); lhg(_t3); ctx(RP(rG, oN, SR),RP(S2, oN, 2)) }
            trans(HEIGHT) { src(ABC, oN, ER); tgt(ABCopt, LI, ER); lhg(setOf(_t3), setOf(_t3));  prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S2, oN, 1); tgt(_t1, oN, ER); lhg(setOf(_t6,_t3)); ctx(RP(S2, oN, 2),RP(rG, oN, SR)) }
            trans(WIDTH) { src(S2, oN, 2); tgt(_t6, oN, ER); lhg(_t7); ctx(RP(S2, oN, 2),RP(rG, oN, SR)) }
            trans(WIDTH) { src(S2, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(_t3); ctx(RP(S2, oN, 2),RP(rG, oN, SR)) }
            trans(HEIGHT) { src(S2, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT,_t1), setOf(EOT,_t1));  prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(ABC, oN, 1); lhg(setOf(_t7), setOf(_t3));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(GRAFT) { src(_t7, oN, ER); tgt(ABC, oN, 2); lhg(_t8);  prevPair(RP(S2, oN, 2), RP(ABC, oN, 1)); prevPair(RP(rG, oN, SR), RP(ABC, oN, 1)) }
            trans(GRAFT) { src(_t8, oN, ER); tgt(ABC, oN, ER); lhg(_t3);  prevPair(RP(rG, oN, SR), RP(ABC, oN, 2)); prevPair(RP(S2, oN, 2), RP(ABC, oN, 2)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(ABCopt, LE, ER); lhg(setOf(_t3), setOf(_t3));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(S2, oN, 2), RP(S2, oN, 2)); prevPair(RP(rG, oN, SR), RP(S2, oN, 2)) }
            trans(GRAFT) { src(_t3, oN, ER); tgt(ABCX, oN, ER); lhg(setOf(EOT,_t1));  prevPair(RP(rG, oN, SR), RP(ABCX, oN, 1)); prevPair(RP(S2, oN, 2), RP(ABCX, oN, 1)) }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S2, oN, 2); lhg(setOf(_t6,_t3));  prevPair(RP(S2, oN, 2), RP(S2, oN, 1)); prevPair(RP(rG, oN, SR), RP(S2, oN, 1)) }
        }
        AutomatonTest.assertMatches(expected, actual)
    }

}
