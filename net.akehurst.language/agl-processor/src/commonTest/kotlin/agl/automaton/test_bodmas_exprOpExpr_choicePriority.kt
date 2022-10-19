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

internal class test_bodmas_exprOpExpr_choicePriority : test_AutomatonAbstract() {

    // S = E
    // E = v < EA < EB
    // EA = E a E
    // EB = E b E

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("'v'")
            ref("EA")
            ref("EB")
        }
        literal("'v'", "v")
        concatenation("EA") { ref("E"); literal("a"); ref("E") }
        concatenation("EB") { ref("E"); literal("b"); ref("E") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val EA = rrs.findRuntimeRule("EA")
    private val EB = rrs.findRuntimeRule("EB")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val v = rrs.findRuntimeRule("'v'")

    @Test
    fun automaton_parse_v() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "v", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S */
            val s1 = state(RP(v, 0, EOR))     /* v .     */
            val s2 = state(RP(E, 0, EOR))     /* E = v . */
            val s3 = state(RP(S, 0, EOR))     /* S = E . */
            val s4 = state(RP(EA, 0, 1), RP(EB, 0, 1))     /* G = . S   */
            val s5 = state(RP(EA, 0, 1))     /* G = . S   */
            val s6 = state(RP(EB, 0, 1))     /* G = . S   */
            val s7 = state(RP(G, 0, EOR))     /* G = . S   */


            //transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vav() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "vav", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */


            //transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)

        }

        // then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}