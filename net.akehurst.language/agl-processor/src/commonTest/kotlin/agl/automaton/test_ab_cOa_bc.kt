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

internal class test_ab_cOa_bc : test_AutomatonAbstract() {

    // S = ab_c | a_bc;
    // ab_c = ab 'c'
    // a_bc = 'a' bc
    // ab = 'a' 'b'
    // bc = 'b' 'c'

    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("ab_c")
            ref("a_bc")
        }
        concatenation("ab_c") { ref("ab"); literal("c") }
        concatenation("a_bc") { literal("a"); ref("bc") }
        concatenation("ab") { literal("a"); literal("b") }
        concatenation("bc") { literal("b"); literal("c") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor("S", AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val ab_c = rrs.findRuntimeRule("ab_c")
    private val a_bc = rrs.findRuntimeRule("a_bc")
    private val ab = rrs.findRuntimeRule("ab")
    private val bc = rrs.findRuntimeRule("bc")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("abc")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val result = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))      /* G = . S   */
            state(RP(G, o0, EOR))      /* G = S .   */
            state(RP(S, o0, EOR))      /* S = ab_c . */
            state(RP(S, o1, EOR))      /* S = a_bc . */
            state(RP(ab_c, o0, p1))    /* ab_c = a . bc */
            state(RP(ab_c, o0, EOR))   /* ab_c = a bc . */
            state(RP(a_bc, o0, p1))    /* a_bc = ab . c */
            state(RP(a_bc, o0, EOR))   /* a_bc = ab c . */
            state(RP(ab, o0, p1))      /* ab = 'a' . 'b' */
            state(RP(ab, o0, EOR))     /* ab = 'a' 'b' . */
            state(RP(ab, o0, p1))      /* bc = 'b' . 'c' */
            state(RP(ab, o0, EOR))     /* bc = 'b' 'c' . */
            state(RP(b, o0, EOR))      /* b . */
            state(RP(a, o0, EOR))      /* a . */
            state(RP(c, o0, EOR))      /* c . */

            transition(WIDTH) { ctx(G, o0, SOR); src(G, o0, SOR); tgt(a); lhg(b) }
            transition(WIDTH) { ctx(G, o0, SOR); src(a_bc, o0, p1); tgt(b); lhg(c) }
            transition(WIDTH) { ctx(G, o0, SOR); src(ab, o0, p1); tgt(b); lhg(c) }
            transition(WIDTH) { ctx(G, o0, SOR); src(ab_c, o0, p1); tgt(c); lhg(EOT) }
            transition(WIDTH) { ctx(a_bc, o0, p1); src(bc, o0, p1); tgt(c); lhg(EOT) }
            transition(GOAL) { ctx(G, o0, SOR); src(S); tgt(G); lhg(EOT) }
            transition(GOAL) { ctx(G, o0, SOR); src(S,o1,EOR); tgt(G); lhg(EOT) }
            transition(HEIGHT) { ctx(G, o0, SOR); src(ab_c); tgt(S); tgt(EOT) }
            transition(HEIGHT) { ctx(G, o0, SOR); src(a_bc); tgt(S,o1,EOR) }
            transition(GRAFT) { ctx(a_bc, o0, p1); src(bc); tgt(a_bc) }
            transition(HEIGHT) { ctx(G, o0, SOR); src(a); tgt(a_bc,o0,p1) }
            transition(GRAFT) { ctx(ab, o0, p1); src(b); tgt(ab) }
            transition(HEIGHT) { ctx(G, o0, SOR); src(a); tgt(ab,o1,p1) }
            transition(GRAFT) { ctx(ab_c, o0, p1); src(c); tgt(ab_c) }
            transition(HEIGHT) { ctx(G, o0, SOR); src(ab); tgt(ab_c,o0,p1) }
            transition(GRAFT) { ctx(bc, o0, p1); src(c); tgt(bc) }
            transition(HEIGHT) { ctx(a_bc, o0, p1); src(b); tgt(bc,o0,p1) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

}