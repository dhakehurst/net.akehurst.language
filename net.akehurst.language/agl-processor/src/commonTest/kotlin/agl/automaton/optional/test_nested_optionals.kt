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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_nested_optionals : test_AutomatonAbstract() {
    /*
        S = 'i' 'a' Rs 'z' ;
        Rs = R+ ;
        R = Os 'i' 't' ;
        Os = Bo Co Do ;
        Bo = 'b'?
        Co = 'c'?
        Do = 'd'?
     */
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { literal("i"); literal("a"); ref("Rs"); literal("z") }
        multi("Rs",1,-1,"R")
        concatenation("R") { ref("Os"); literal("i"); literal("t") }
        concatenation("Os") { ref("Bo"); ref("Co"); ref("Do") }
        multi("Bo", 0, 1, "'b'")
        multi("Co", 0, 1, "'c'")
        multi("Do", 0, 1, "'d'")
        literal("'b'", "b")
        literal("'c'", "c")
        literal("'d'", "d")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val Rs = rrs.findRuntimeRule("Rs")
    private val R = rrs.findRuntimeRule("R")
    private val Os = rrs.findRuntimeRule("Os")
    private val Bo = rrs.findRuntimeRule("Bo")
    private val Bo_E = EMPTY
    private val Co = rrs.findRuntimeRule("Co")
    private val Co_E = EMPTY
    private val Do = rrs.findRuntimeRule("Do")
    private val Do_E = EMPTY
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")
    private val i = rrs.findRuntimeRule("'i'")
    private val t = rrs.findRuntimeRule("'t'")
    private val z = rrs.findRuntimeRule("'z'")

    @Test
    fun parse_iaitz() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "iaitz", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, o0, SR))
            val s1 = state(RP(i, o0, EOR))
            val s2 = state(RP(S, o0, p1))
            val s3 = state(RP(a, o0, EOR))



            trans(WIDTH) { ctx(G,o0,SR); src(S,o0,p1); tgt(a); lhg(b) }
            trans(WIDTH) { ctx(G,o0,SOR); src(G,o0,p1); tgt(i); lhg(a) }
            trans(HEIGHT) { ctx(G,o0,SOR); src(i); tgt(S,o0,p1); lhg(a) }

        }
        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("iaitz","iabitz", "iacitz","iaditz","iabcditz")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val result = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {


        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_dmdBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_dmdBuild)
        val sentences = listOf("iaitz","iabitz", "iacitz","iaditz","iabcditz")
        for(sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_dmdBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--Build-on-Demand--")
        println(rrs_dmdBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}