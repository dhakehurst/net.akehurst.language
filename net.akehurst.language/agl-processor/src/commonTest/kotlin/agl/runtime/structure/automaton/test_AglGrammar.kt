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

        val s0 = rrs.startingState(R_rule)
        val sm = s0.stateSet

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
    fun rule_firstOf_s0() {

        val rule = rrs.findRuntimeRule("rule")
        val s0 = rrs.startingState(rule)
        val sm = s0.stateSet

        val actual = sm.firstOf(s0.rulePosition, setOf(RuntimeRuleSet.END_OF_TEXT))

        val expected = setOf<RuntimeRule>(R_override, R_skip, R_leaf,R_IDENTIFIER)

        assertEquals(expected, actual)

    }

}