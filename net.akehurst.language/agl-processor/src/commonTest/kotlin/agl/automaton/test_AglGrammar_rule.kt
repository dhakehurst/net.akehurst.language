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

import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_AglGrammar_rule : test_AutomatonAbstract() {

    private companion object {
        val grammar = AglGrammarGrammar()
        val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
        val parser = ScanOnDemandParser(converterToRuntimeRules.runtimeRuleSet)
        val rrs = parser.runtimeRuleSet

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

        val R_IDENTIFIER = rrs.findRuntimeRule("IDENTIFIER")
        val T_namespace = rrs.findRuntimeRule("'namespace'")

        val SM = rrs.fetchStateSetFor(R_rule, AutomatonKind.LOOKAHEAD_1)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()


    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, LHS(R_isLeaf)), // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP))        // G = S .
//TODO
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(s0).toList()

        val expected = listOf(
            WidthInfo(RP(T_namespace, 0, EOR), lhs_U.part),
            WidthInfo(RP(T_namespace, 0, EOR), lhs_U.part)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
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

}