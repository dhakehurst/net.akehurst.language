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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_ab_cOa_bc : test_AutomatonAbstract() {

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
    private val G = rrs.goalRuleFor[S]
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
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size, result.issues.joinToString("\n") { it.toString() })
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))      /* G = . S   */
            state(RP(G, oN, EOR))      /* G = S .   */
            state(RP(S, oN, EOR))      /* S = ab_c . */
            state(RP(S, o1, EOR))      /* S = a_bc . */
            state(RP(ab_c, oN, p1))    /* ab_c = a . bc */
            state(RP(ab_c, oN, EOR))   /* ab_c = a bc . */
            state(RP(a_bc, oN, p1))    /* a_bc = ab . c */
            state(RP(a_bc, oN, EOR))   /* a_bc = ab c . */
            state(RP(ab, oN, p1))      /* ab = 'a' . 'b' */
            state(RP(ab, oN, EOR))     /* ab = 'a' 'b' . */
            state(RP(bc, oN, p1))      /* bc = 'b' . 'c' */
            state(RP(bc, oN, EOR))     /* bc = 'b' 'c' . */
            state(RP(b, oN, EOR))      /* b . */
            state(RP(a, oN, EOR))      /* a . */
            state(RP(c, oN, EOR))      /* c . */

            trans(WIDTH) { ctx(G, oN, SOR); src(G, oN, SOR); tgt(a); lhg(b) }
            trans(WIDTH) { ctx(G, oN, SOR); src(a_bc, oN, p1); tgt(b); lhg(c) }
            trans(WIDTH) { ctx(G, oN, SOR); src(ab, oN, p1); tgt(b); lhg(c) }
            trans(WIDTH) { ctx(G, oN, SOR); src(ab_c, oN, p1); tgt(c); lhg(EOT) }
            trans(WIDTH) { ctx(a_bc, oN, p1); src(bc, oN, p1); tgt(c); lhg(EOT) }
            trans(GOAL) { ctx(G, oN, SOR); src(S); tgt(G); lhg(EOT) }
            trans(GOAL) { ctx(G, oN, SOR); src(S, o1, EOR); tgt(G); lhg(EOT) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(ab_c); tgt(S); lhg(EOT) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(a_bc); tgt(S, o1, EOR) }
            trans(GRAFT) { ctx(a_bc, oN, p1); src(bc); tgt(a_bc) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(a); tgt(a_bc, oN, p1) }
            trans(GRAFT) { ctx(ab, oN, p1); src(b); tgt(ab) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(a); tgt(ab, oN, p1) }
            trans(GRAFT) { ctx(ab_c, oN, p1); src(c); tgt(ab_c) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(ab); tgt(ab_c, oN, p1) }
            trans(GRAFT) { ctx(bc, oN, p1); src(c); tgt(bc) }
            trans(HEIGHT) { ctx(a_bc, oN, p1); src(b); tgt(bc, oN, p1) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

}