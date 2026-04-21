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

class test_Grune_Jacobs_Fig_10_2_Expressions_LC : test_AutomatonAbstract() {

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
    private val E = rrs.findRuntimeRule("E")
    private val T = rrs.findRuntimeRule("T")
    private val F = rrs.findRuntimeRule("F")
    private val F2 = rrs.findRuntimeRule("F2")
    private val E1 = rrs.findRuntimeRule("E1")
    private val T1 = rrs.findRuntimeRule("T1")

    private val a = rrs.findRuntimeRule("'a'")
    private val m = rrs.findRuntimeRule("'m'")
    private val v = rrs.findRuntimeRule("'v'")
    private val o = rrs.findRuntimeRule("'('")
    private val c = rrs.findRuntimeRule("')'")

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("v", "vav", "vmv", "vavmv", "vmvav")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            //todo
        }
        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("v", "vav", "vmv", "vavmv", "vmvav", "(v)", "(vav)", "(v)mv", "vm(v)", "(v)m(v)")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S", true))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S", true))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}