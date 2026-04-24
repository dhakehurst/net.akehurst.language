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

    private val S = rrs.rule[0]  // S
    private val _t1 = rrs.rule[1]  // 'c'
    private val ab_c = rrs.rule[2]  // ab_c
    private val _t3 = rrs.rule[3]  // 'a'
    private val a_bc = rrs.rule[4]  // a_bc
    private val _t5 = rrs.rule[5]  // 'b'
    private val ab = rrs.rule[6]  // ab
    private val bc = rrs.rule[7]  // bc
    private val rG = rrs.goalRuleFor[S]

//    private val S = rrs.findRuntimeRule("S")
//    private val rG = rrs.goalRuleFor[S]
//    private val ab_c = rrs.findRuntimeRule("ab_c")
//    private val a_bc = rrs.findRuntimeRule("a_bc")
//    private val ab = rrs.findRuntimeRule("ab")
//    private val bc = rrs.findRuntimeRule("bc")
//    private val a = rrs.findRuntimeRule("'a'")
//    private val b = rrs.findRuntimeRule("'b'")
//    private val c = rrs.findRuntimeRule("'c'")

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
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o0, ER)   // S = ab_c .
            state(S, o1, ER)   // S = a_bc .
            state(ab_c, oN, 1)   // ab_c = ab . 'c'
            state(ab_c, oN, ER)   // ab_c = ab 'c' .
            state(ab, oN, 1)   // ab = 'a' . 'b'
            state(ab, oN, ER)   // ab = 'a' 'b' .
            state(a_bc, oN, 1)   // a_bc = 'a' . bc
            state(a_bc, oN, ER)   // a_bc = 'a' bc .
            state(bc, oN, 1)   // bc = 'b' . 'c'
            state(bc, oN, ER)   // bc = 'b' 'c' .
            state(_t3, oN, ER)   // 'a'
            state(_t5, oN, ER)   // 'b'
            state(_t1, oN, ER)   // 'c'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(_t5); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ab_c, oN, 1); tgt(_t1, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ab_c, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(ab, oN, 1); tgt(_t5, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ab, oN, ER); tgt(ab_c, oN, 1); lhg(setOf(_t1), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(a_bc, oN, 1); tgt(_t5, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(a_bc, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(bc, oN, 1); tgt(_t1, oN, ER); lhg(EOT); ctx(a_bc, oN, 1) }
            trans(GRAFT) { src(bc, oN, ER); tgt(a_bc, oN, ER); lhg(EOT); ctx(a_bc, oN, 1); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(a_bc, oN, 1); lhg(setOf(_t5), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ab, oN, 1); lhg(setOf(_t5), setOf(_t1)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t5, oN, ER); tgt(bc, oN, 1); lhg(setOf(_t1), setOf(EOT)); ctx(a_bc, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t5, oN, ER); tgt(ab, oN, ER); lhg(_t1); ctx(ab, oN, 1); pctx(rG, oN, SR) }
            trans(GRAFT) { src(_t1, oN, ER); tgt(bc, oN, ER); lhg(EOT); ctx(bc, oN, 1); pctx(a_bc, oN, 1) }
            trans(GRAFT) { src(_t1, oN, ER); tgt(ab_c, oN, ER); lhg(EOT); ctx(ab_c, oN, 1); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

}