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

import net.akehurst.language.agl.automaton.AutomatonTest
import net.akehurst.language.agl.automaton.automaton
import net.akehurst.language.agl.automaton.test_AutomatonUtilsAbstract
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhs
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
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
            state(RP(G, o0, SOR))   // G = . S
            state(RP(G, o0, EOR))   // G = S .
            state(RP(S, o0, p1))    // S = a . b c
            state(RP(S, o0, p2))    // S = a b . c
            state(RP(S, o0, EOR))    // S = a b c .
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
            state(RP(G, o0, SOR))   // G = . S
            state(RP(G, o0, EOR))   // G = S .
            state(RP(S, o0, EOR))    // S = a .
            state(RP(S, o1, EOR))   // S = S1 .
            state(RP(S1, o0, p1))   // S1 = S . a
            state(RP(S1, o0, EOR))   // S1 = S a .
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

        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "ba", AutomatonKind.LOOKAHEAD_1)
        parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))   // G = . S
            state(RP(G, o0, EOR))   // G = S .
            state(RP(S, o0, EOR))    // S = S1 .
            state(RP(S1, o0, p1))   // S1 = M . S2
            state(RP(S1, o0, EOR))  // S1 = M S2 .
            state(RP(M, OME, EOR))  // S1 = b*   [] .
            state(RP(M, OMI, PMI))  // S1 = b*   [--b . b--]
            state(RP(M, OMI, EOR))  // S1 = b*   [--b] .
            state(RP(S2, o0, EOR))   // S2 = S3 .
            state(RP(S3, o0, EOR))   // S3 = a .
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

        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abcx", AutomatonKind.LOOKAHEAD_1)
        parser.parseForGoal("S", "x", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))     // G = . S
            state(RP(G, o0, EOR))     // G = S .
            state(RP(S, o0, EOR))     // S = ABCX .
            state(RP(S, o1, EOR))     // S = S2 .
            state(RP(S2, o0, p1))     // S2 = S . y S
            state(RP(S2, o0, p2))     // S2 = S y . S
            state(RP(S2, o0, EOR))    // S2 = S y S .
            state(RP(ABCX, o0, p1))   // ABCX = ABCopt . x
            state(RP(ABCX, o0, EOR))  // ABCX = ABCopt x .
            state(RP(ABCopt, OME, EOR))  // ABCopt = <empty> .
            state(RP(ABCopt, OMI, EOR))  // ABCopt = ABC .
            state(RP(ABC, o0, p1))      // ABC = a . b c
            state(RP(ABC, o0, p2))      // ABC = a b . c
            state(RP(ABC, o0, EOR))      // ABC = a b c .
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
