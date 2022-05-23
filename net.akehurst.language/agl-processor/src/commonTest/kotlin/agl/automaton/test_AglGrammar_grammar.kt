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

    private companion object {
        val grammar = AglGrammarGrammar()
        val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
        val parser = ScanOnDemandParser(converterToRuntimeRules.runtimeRuleSet)
        val rrs = parser.runtimeRuleSet

        val R_grammarDefinition = rrs.findRuntimeRule("grammarDefinition")
        val R_namespace = rrs.findRuntimeRule("namespace")
        val R_rule = rrs.findRuntimeRule("rule")

        val R_isOverride = rrs.findRuntimeRule("isOverride")
        val R_override = R_isOverride.rhs.items[0].rhs.items[RuntimeRuleItem.MULTI__ITEM]
        val R_overrideEmpty = R_isOverride.rhs.items[0].rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]

        val R_isSkip = rrs.findRuntimeRule("isSkip")
        val R_skip = R_isSkip.rhs.items[0].rhs.items[RuntimeRuleItem.MULTI__ITEM]
        val R_skipEmpty = R_isSkip.rhs.items[0].rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]

        val R_isLeaf = rrs.findRuntimeRule("isLeaf")
        val R_leaf = R_isLeaf.rhs.items[0].rhs.items[RuntimeRuleItem.MULTI__ITEM]
        val R_leafEmpty = R_isLeaf.rhs.items[0].rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]

        val T_IDENTIFIER = rrs.findRuntimeRule("IDENTIFIER")
        val T_namespace = rrs.findRuntimeRule("'namespace'")
        val T_grammar = rrs.findRuntimeRule("'grammar'")

        val SM = rrs.fetchStateSetFor(R_grammarDefinition, AutomatonKind.LOOKAHEAD_1)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val lhs_IDENTIFIER = LookaheadSetPart(false,false, false,setOf(T_IDENTIFIER))
    }

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
}