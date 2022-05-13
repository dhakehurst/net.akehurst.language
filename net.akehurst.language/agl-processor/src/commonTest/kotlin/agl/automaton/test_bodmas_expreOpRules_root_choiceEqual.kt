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

import agl.automaton.AutomatonTest
import agl.automaton.automaton
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_bodmas_expreOpRules_root_choiceEqual : test_AutomatonAbstract() {

    // S =  E ;
    // E = R | M | A ;
    // R = 'v'           root = var
    // M = E 'm' E ;     mul = expr * expr
    // A = E 'a' E ;     add = expr + expr

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("R")
            ref("M")
            ref("A")
        }
        concatenation("R") { literal("v") }
        concatenation("M") { ref("E"); literal("m"); ref("E") }
        concatenation("A") { ref("E"); literal("a"); ref("E") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val root = rrs.findRuntimeRule("R")
    private val mul = rrs.findRuntimeRule("M")
    private val add = rrs.findRuntimeRule("A")
    private val m = rrs.findRuntimeRule("'m'")
    private val a = rrs.findRuntimeRule("'a'")
    private val v = rrs.findRuntimeRule("'v'")


    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), LHS(UP), LHS(v)),       // G = . S
            Triple(RP(G, 0, EOR), LHS(UP), LHS(UP)),      // G = S .
            Triple(RP(S, 0, EOR), LHS(UP), LHS(UP)),      // E = . v
            Triple(RP(E, 0, EOR), LHS(UP), LHS(UP)),      // E = root .
//TODO
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {

    }


    @Test
    fun automaton_parse_v() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "v", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_sentences() {
        val parser = ScanOnDemandParser(rrs)
        val sentences = listOf("v","vav", "vmv","vmvav","vavmv")
        sentences.forEach {
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() } )
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        //TODO val sentences = listOf("v","vav", "vmv","vmvav","vavmv")
        val sentences = listOf("vmv")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() } )
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))      /* G = . S   */
            val s1 = state(RP(v, 0, EOR))      /* 'v' .   */

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}