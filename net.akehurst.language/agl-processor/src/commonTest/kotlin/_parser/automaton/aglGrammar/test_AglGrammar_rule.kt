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

import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.ConverterToRuntimeRules
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_AglGrammar_rule : test_AutomatonAbstract() {

    private val grammar = AglGrammar.grammar
    private val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
    private val scanner = ScannerOnDemand(RegexEnginePlatform, converterToRuntimeRules.runtimeRuleSet.terminals.toList())
    private val parser = LeftCornerParser(scanner, converterToRuntimeRules.runtimeRuleSet)
    private val rrs = parser.ruleSet as RuntimeRuleSet

    private val userGoalRuleName = "rule"

    private val R_rule = rrs.findRuntimeRule(userGoalRuleName)

//    private val R_isOverride = rrs.findRuntimeRule("isOverride")
    //   private val T_override = R_isOverride.rhs.items[RuntimeRuleRhs.MULTI__ITEM]
    //   private val T_overrideEmpty = R_isOverride.rhs.items[RuntimeRuleRhs.MULTI__EMPTY_RULE]

//    private val R_isSkip = rrs.findRuntimeRule("isSkip")
    //   private val T_skip = R_isSkip.rhs.items[RuntimeRuleRhs.MULTI__ITEM]
    //   private val T_skipEmpty = R_isSkip.rhs.items[RuntimeRuleRhs.MULTI__EMPTY_RULE]

 //   private val R_isLeaf = rrs.findRuntimeRule("isLeaf")
    //   private val T_leaf = R_isLeaf.rhs.items[RuntimeRuleRhs.MULTI__ITEM]
//    private val T_leafEmpty = R_isLeaf.rhs.items[RuntimeRuleRhs.MULTI__EMPTY_RULE]

//    private val T_IDENTIFIER = rrs.findRuntimeRule("IDENTIFIER")
//    private val T_equals = rrs.findRuntimeRule("'='")

    private val SM = rrs.fetchStateSetFor(R_rule, AutomatonKind.LOOKAHEAD_1)
    private val s0 = SM.startState
    private val G = s0.runtimeRules.first()

    // IMPORTANT, notice that userGoal rule is 'rule' (not 'grammar')

    @Test
    fun automaton_parse__r_a() {
        val parser = LeftCornerParser(scanner, rrs)
        val result = parser.parseForGoal(userGoalRuleName, "r=a;")
        println(rrs.usedAutomatonToString(userGoalRuleName))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = (parser.ruleSet as RuntimeRuleSet).fetchStateSetFor(R_rule, AutomatonKind.LOOKAHEAD_1)

        val expected = aut(rrs, AutomatonKind.LOOKAHEAD_1, userGoalRuleName, false) {
            state(RP(G, oN, SR))     /* G = . S   */

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse__r_bac() {
        val parser = LeftCornerParser(scanner, rrs)
        val result = parser.parseForGoal(userGoalRuleName, "r=(a);")
        println(rrs.usedAutomatonToString(userGoalRuleName))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = (parser.ruleSet as RuntimeRuleSet).fetchStateSetFor(R_rule, AutomatonKind.LOOKAHEAD_1)

        val expected = aut(rrs, AutomatonKind.LOOKAHEAD_1, userGoalRuleName, false) {
            val s0 = state(RP(G, oN, SR))     /* G = . S   */

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse__s_l_r_bac() {
        val parser = LeftCornerParser(scanner, rrs)
        val result = parser.parseForGoal(userGoalRuleName, "skip leaf r=(a);")
        println(rrs.usedAutomatonToString(userGoalRuleName))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = (parser.ruleSet as RuntimeRuleSet).fetchStateSetFor(R_rule, AutomatonKind.LOOKAHEAD_1)

        val expected = aut(rrs, AutomatonKind.LOOKAHEAD_1, userGoalRuleName, false) {
            val s0 = state(RP(G, oN, SOR))     /* G = . S   */

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}