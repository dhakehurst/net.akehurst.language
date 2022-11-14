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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_leftRecursive : test_AutomatonAbstract() {

    // S =  'a' | S1
    // S1 = S 'a'

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("a")
            ref("S1")
        }
        concatenation("S1") { ref("S"); literal("a") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val S1 = rrs.findRuntimeRule("S1")
    private val a = rrs.findRuntimeRule("'a'")

    @Test
    fun parse_a() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, EOR))     /* {}     G = S .    */
            state(RP(S, o0, EOR))     /* {0}    S = a .    */
            state(RP(a, o0, EOR))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */

            transition(WIDTH) { ctx(G,o0,SOR); src(G,o0,SOR); tgt(a); lhg(setOf(EOT,a))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S); tgt(G); lhg(setOf(EOT))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(a); tgt(S); lhg(setOf(RT, EOT,a),setOf(RT, EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S); tgt(S1,o0,p1); lhg(setOf(a),setOf(RT, EOT,a))  }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aa() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "aa", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, EOR))     /* {}     G = S .    */
            state(RP(S, o0, EOR))     /* {0}    S = a .    */
            state(RP(S, o1, EOR))     /* {0}    S = S1 .   */
            state(RP(a, o0, EOR))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */
            state(RP(S1, o0, EOR))    /* {0}    S1 = S a . */

            transition(WIDTH) { ctx(G,o0,SOR); src(G,o0,SOR); tgt(a); lhg(setOf(EOT,a))  }
            transition(WIDTH) { ctx(G,o0,SOR); src(S1,o0,p1); tgt(a); lhg(setOf(RT))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S); tgt(G); lhg(setOf(EOT))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S,o1,EOR); tgt(G); lhg(setOf(EOT))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(a); tgt(S); lhg(setOf(RT, EOT,a),setOf(RT, EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S1); tgt(S,o1,EOR); lhg(setOf(RT,a),setOf(RT,a))  }
            transition(GRAFT) { ctx(S1,o0,p1); src(a); tgt(S1); lhg(setOf(RT))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S); tgt(S1,o0,p1); lhg(setOf(a),setOf(RT, EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S,o1,EOR); tgt(S1,o0,p1); lhg(setOf(a),setOf(RT,a))  }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aaa() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "aaa", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, EOR))     /* {}     G = S .    */
            state(RP(S, o0, EOR))     /* {0}    S = a .    */
            state(RP(S, o1, EOR))     /* {0}    S = S1 .   */
            state(RP(a, o0, EOR))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */
            state(RP(S1, o0, EOR))    /* {0}    S1 = S a . */

            transition(WIDTH) { ctx(G,o0,SOR); src(G,o0,SOR); tgt(a); lhg(setOf(EOT,a))  }
            transition(WIDTH) { ctx(G,o0,SOR); src(S1,o0,p1); tgt(a); lhg(setOf(RT))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S); tgt(G); lhg(setOf(EOT))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S,o1,EOR); tgt(G); lhg(setOf(EOT))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(a); tgt(S); lhg(setOf(RT, EOT,a),setOf(RT, EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S1); tgt(S,o1,EOR); lhg(setOf(RT,a),setOf(RT,a))  }
            transition(GRAFT) { ctx(S1,o0,p1); src(a); tgt(S1); lhg(setOf(RT))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S); tgt(S1,o0,p1); lhg(setOf(a),setOf(RT, EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S,o1,EOR); tgt(S1,o0,p1); lhg(setOf(a),setOf(RT,a))  }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, EOR))     /* {}     G = S .    */
            state(RP(S, o0, EOR))     /* {0}    S = a .    */
            state(RP(S, o1, EOR))     /* {0}    S = S1 .   */
            state(RP(a, o0, EOR))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */
            state(RP(S1, o0, EOR))    /* {0}    S1 = S a . */

            transition(WIDTH) { ctx(G,o0,SOR); src(G,o0,SOR); tgt(a); lhg(setOf(EOT,a))  }
            transition(WIDTH) { ctx(G,o0,SOR); src(S1,o0,p1); tgt(a); lhg(setOf(EOT,a))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S); tgt(G); lhg(setOf(EOT))  }
            transition(GOAL) { ctx(G,o0,SOR); src(S,o1,EOR); tgt(G); lhg(setOf(EOT))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(a); tgt(S); lhg(setOf(EOT,a),setOf(EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S1); tgt(S,o1,EOR); lhg(setOf(EOT,a),setOf(EOT,a))  }
            transition(GRAFT) { ctx(S1,o0,p1); src(a); tgt(S1); lhg(setOf(EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S); tgt(S1,o0,p1); lhg(setOf(a),setOf(EOT,a))  }
            transition(HEIGHT) { ctx(G,o0,SOR); src(S,o1,EOR); tgt(S1,o0,p1); lhg(setOf(a),setOf(EOT,a))  }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("a", "aa", "aaa", "aaaa")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(automaton_preBuild, automaton_noBuild,AutomatonTest.MatchConfiguration(
            no_lookahead_compare = true
        ))
    }
}