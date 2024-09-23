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

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test

internal class test_multi_0_n_literal : test_AutomatonAbstract() {

    // S =  'a'* ;

    private companion object {
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "'a'")
            literal("'a'", "a")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val eS = EMPTY
        val a = rrs.findRuntimeRule("'a'")

        val s0 = SM.startState
        val s1 = SM.createState(listOf(RulePositionRuntime(a, 0, RulePositionRuntime.END_OF_RULE)))
        val s2 = SM.createState(listOf(RulePositionRuntime(eS, 0, RulePositionRuntime.END_OF_RULE)))
        val s3 = SM.createState(listOf(RulePositionRuntime(S, 0, RulePositionRuntime.POSITION_MULIT_ITEM)))
        val s4 = SM.createState(listOf(RulePositionRuntime(S, 0, RulePositionRuntime.END_OF_RULE)))

        val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))
        val lhs_aU = SM.createLookaheadSet(true, false, false, setOf(a))
        val lhs_aT = SM.createLookaheadSet(false, true, false, setOf(a))
    }

    @Test
    fun parse_aba() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "aba")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("", "a", "aa", "aaa", "aaaa")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}