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
import net.akehurst.language.automaton.leftcorner.AutomatonTest
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonUtilsAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test

internal class test_buildFor : test_AutomatonUtilsAbstract() {

    @Test
    fun concatenation() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b"); literal("c") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))   // G = . S
            state(RP(G, oN, EOR))   // G = S .
            state(RP(S, oN, p1))    // S = a . b c
            state(RP(S, oN, p2))    // S = a b . c
            state(RP(S, oN, EOR))    // S = a b c .
            state(RP(a))
            state(RP(b))
            state(RP(c))
        }
        AutomatonTest.assertMatches(expected, actual)
    }


    @Test
    fun leftRecursion() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")


        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))   // G = . S
            state(RP(G, oN, EOR))   // G = S .
            state(RP(S, oN, EOR))    // S = a .
            state(RP(S, o1, EOR))   // S = S1 .
            state(RP(S1, oN, p1))   // S1 = S . a
            state(RP(S1, oN, EOR))   // S1 = S a .
            state(RP(a))
        }
        AutomatonTest.assertMatches(expected, actual)
    }

    @Test
    fun nested() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("S1") }
            concatenation("S1") { ref("M"); ref("S2") }
            multi("M", 0, -1, "'b'")
            concatenation("S2") { ref("S3") }
            concatenation("S3") { literal("a") }
            literal("'b'", "b")
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val S1 = rrs.findRuntimeRule("S1")
        val S2 = rrs.findRuntimeRule("S2")
        val S3 = rrs.findRuntimeRule("S3")
        val M = rrs.findRuntimeRule("M")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val eM = EMPTY

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        parser.parseForGoal("S", "ba")
        parser.parseForGoal("S", "a")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))   // G = . S
            state(RP(G, oN, ER))   // G = S .
            state(RP(S, oN, ER))    // S = S1 .
            state(RP(S1, oN, p1))   // S1 = M . S2
            state(RP(S1, oN, ER))  // S1 = M S2 .
            state(RP(M, oLE, ER))  // S1 = b*   [] .
            state(RP(M, oLI, PMI))  // S1 = b*   [--b . b--]
            state(RP(M, oLI, ER))  // S1 = b*   [--b] .
            state(RP(S2, oN, ER))   // S2 = S3 .
            state(RP(S3, oN, ER))   // S3 = a .
            state(RP(a))            // a .
            state(RP(b))            // b .
            state(RP(eM))            // <empty>.M .
        }
        AutomatonTest.assertMatches(expected, actual)
    }

    @Test
    fun xx() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABCX");
                ref("S2")
            }
            concatenation("S2") { ref("S"); literal("y"); ref("S"); }
            concatenation("ABCX") { ref("ABCopt"); literal("x") }
            multi("ABCopt", 0, 1, "ABC")
            concatenation("ABC") { literal("a"); literal("b"); literal("c") }

        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val S2 = rrs.findRuntimeRule("S2")
        val ABCX = rrs.findRuntimeRule("ABCX")
        val ABCopt = rrs.findRuntimeRule("ABCopt")
        val eABCopt = EMPTY
        val ABC = rrs.findRuntimeRule("ABC")
        val x = rrs.findRuntimeRule("'x'")
        val y = rrs.findRuntimeRule("'y'")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        parser.parseForGoal("S", "abcx")
        parser.parseForGoal("S", "x")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))     // G = . S
            state(RP(G, oN, ER))     // G = S .
            state(RP(S, oN, ER))     // S = ABCX .
            state(RP(S, o1, ER))     // S = S2 .
            state(RP(S2, oN, p1))     // S2 = S . y S
            state(RP(S2, oN, p2))     // S2 = S y . S
            state(RP(S2, oN, ER))    // S2 = S y S .
            state(RP(ABCX, oN, p1))   // ABCX = ABCopt . x
            state(RP(ABCX, oN, ER))  // ABCX = ABCopt x .
            state(RP(ABCopt, oLE, ER))  // ABCopt = <empty> .
            state(RP(ABCopt, oLI, ER))  // ABCopt = ABC .
            state(RP(ABC, oN, p1))      // ABC = a . b c
            state(RP(ABC, oN, p2))      // ABC = a b . c
            state(RP(ABC, oN, ER))      // ABC = a b c .
            state(RP(x))
            state(RP(y))
            state(RP(a))
            state(RP(b))
            state(RP(c))
            state(RP(eABCopt))
        }
        AutomatonTest.assertMatches(expected, actual)
    }

}
