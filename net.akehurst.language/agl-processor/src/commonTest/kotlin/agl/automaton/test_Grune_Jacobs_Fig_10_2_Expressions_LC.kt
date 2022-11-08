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

internal class test_Grune_Jacobs_Fig_10_2_Expressions_LC : test_AutomatonAbstract() {

    //    S = E
    //    E = E1 | T
    //    E1 = E a T
    //    T = T1 | F
    //    T1 = T m F
    //    F = v | F2
    //    F2 = ( E )

    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("E1")
            ref("T")
        }
        concatenation("E1") { ref("E"); literal("a"); ref("T") }
        choice("T", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("T1")
            ref("F")
        }
        concatenation("T1") { ref("T"); literal("m"); ref("F") }
        choice("F", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("v")
            ref("F2")
        }
        concatenation("F2") { literal("("); ref("E"); literal(")") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()

    private val a = rrs.findRuntimeRule("'a'")
    private val m = rrs.findRuntimeRule("'m'")
    private val v = rrs.findRuntimeRule("'v'")

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("v", "vav", "vmv", "vavmv", "vmvav")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val result = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            state(RP(G, o0, SOR))      /* G = . S   */
            state(RP(G, o0, EOR))      /* G = S .   */
            //    S = E
            //    E = E1 | T
            //    E1 = E a T
            //    T = T1 | F
            //    T1 = T m F
            //    F = v | F2
            //    F2 = ( E )


        }

        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("v", "vav", "vmv", "vavmv", "vmvav", "(v)")
        for(sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S", true))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S", true))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}