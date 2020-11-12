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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.ScanOnDemandParser
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglGrammar {

    companion object {
        val grammar = AglGrammarGrammar()
        val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
        val parser = ScanOnDemandParser(converterToRuntimeRules.transform())
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
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val SM = rrs.fetchStateSetFor(R_rule)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
    }

    @Test
    fun firstOf() {
        val rulePositions = listOf(
                Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, setOf(R_isLeaf)), // G = . S
                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)) // G = S .
//TODO
        )


        for (t in rulePositions) {
            val rp = t.first
            val lhs = t.second
            val expected = t.third

            val actual = SM.firstOf(rp, lhs.content)

            assertEquals(expected, actual, "failed $rp")
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