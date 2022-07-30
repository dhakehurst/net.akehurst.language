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
import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_AglGrammar_grammar : test_AutomatonAbstract() {


    private  val grammar = AglGrammarGrammar()
    private  val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
    private   val parser = ScanOnDemandParser(converterToRuntimeRules.runtimeRuleSet)
    private   val rrs = parser.runtimeRuleSet

    private   val R_grammarDefinition = rrs.findRuntimeRule("grammarDefinition")
    private    val R_namespace = rrs.findRuntimeRule("namespace")
    private   val R_rule = rrs.findRuntimeRule("rule")

    private    val R_isOverride = rrs.findRuntimeRule("isOverride")
    private   val R_override = R_isOverride.rhs.items[RuntimeRuleItem.MULTI__ITEM]
    private   val R_overrideEmpty = R_isOverride.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]

    private    val R_isSkip = rrs.findRuntimeRule("isSkip")
    private    val R_skip = R_isSkip.rhs.items[RuntimeRuleItem.MULTI__ITEM]
    private    val R_skipEmpty = R_isSkip.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]

    private    val R_isLeaf = rrs.findRuntimeRule("isLeaf")
    private    val R_leaf = R_isLeaf.rhs.items[RuntimeRuleItem.MULTI__ITEM]
    private   val R_leafEmpty = R_isLeaf.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]

    private    val T_IDENTIFIER = rrs.findRuntimeRule("IDENTIFIER")
    private    val T_namespace = rrs.findRuntimeRule("'namespace'")
    private    val T_grammar = rrs.findRuntimeRule("'grammar'")

    private    val SM = rrs.fetchStateSetFor(R_grammarDefinition, AutomatonKind.LOOKAHEAD_1)
    private    val s0 = SM.startState
    private    val G = s0.runtimeRules.first()

    private    val lhs_IDENTIFIER = LookaheadSetPart(false,false, false,setOf(T_IDENTIFIER))

    private val goal = "grammarDefinition"

    @Test
    fun rule_firstTerminals() {
        // rule = ruleTypeLabels IDENTIFIER '='  choice';'
        // ruleTypeLabels = isOverride isSkip isLeaf
        // isOverride 'override'?
        // isSkip 'skip'?
        // isLeaf 'leaf'?


        val actual = rrs.firstTerminals[R_rule.number]

        val expected = setOf<RuntimeRule>(R_override, R_overrideEmpty)

        assertEquals(expected, actual)

    }

    @Test
    fun parse_xxx() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "namespace test grammar Test { r = 'a' ; }", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(R_grammarDefinition, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser_preBuild = ScanOnDemandParser(rrs_preBuild)
        val parser_noBuild = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf(
            "namespace test grammar Test { r = 'a' ; }",
            "namespace test.ns1 grammar Test { r = 'a' ; }",
            "namespace test.ns1.ns2 grammar Test { r = 'a' ; }",
            "namespace test grammar Test1 extends Test2 { r = 'a' ; }",
            "namespace test grammar Test { r = 'a' 'b' ; }",
            "namespace test grammar Test { r = 'a' 'b' 'c' ; }",
            "namespace test grammar Test { r = 'a' | 'b' ; }",
            "namespace test grammar Test { r = 'a' | 'b' | 'c' ; }",

        )
        for(sen in sentences) {
            val result = parser_noBuild.parseForGoal(goal, sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) {
                println("--Error: No Build--")
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
            val result2 = parser_preBuild.parseForGoal(goal, sen, AutomatonKind.LOOKAHEAD_1)
            if (result2.issues.isNotEmpty()) {
                println("--Error: Pre Build--")
                println("Sentence: $sen")
                result2.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor(goal)
        val automaton_preBuild = rrs_preBuild.buildFor(goal,AutomatonKind.LOOKAHEAD_1)

        println("--No Build--")
        println(rrs_preBuild.usedAutomatonToString(goal))
        println("--Pre Build--")
        println(rrs_noBuild.usedAutomatonToString(goal))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}