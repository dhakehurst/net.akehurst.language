/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.runtime

import net.akehurst.language.api.parser.ParserConstructionFailedException
import parser.runtime.RuntimeRuleKind

class RuntimeRuleSet(val totalRuleNumber: Int) {

    private val runtimeRules: MutableList<RuntimeRule> = mutableListOf()
    private val ruleNumbers: MutableMap<String, Int> = mutableMapOf()
    private val emptyRulesFor: MutableList<RuntimeRule> = mutableListOf()

    val rules: List<RuntimeRule> = mutableListOf<RuntimeRule>()

    fun setRules(rules: List<RuntimeRule>) {
        if (this.totalRuleNumber == rules.size) {

            for (rrule in rules) {
                if (null == rrule) {
                    throw ParserConstructionFailedException("RuntimeRuleSet must not contain a null rule!")
                }
                val i = rrule.number
                this.runtimeRules[i] = rrule
                if (RuntimeRuleKind.NON_TERMINAL == rrule.kind) {
                    this.nodeTypes.set(i, rrule.nodeTypeName)
                    this.ruleNumbers[rrule.nodeTypeName] = i
                } else {
                    this.terminalMap.put(rrule.terminalPatternText, rrule)
                    if (rrule.isEmptyRule) {
                        this.emptyRulesFor[rrule.ruleThatIsEmpty.number] = rrule
                    }
                }
            }

        } else {
            throw ParserConstructionFailedException("Number of RuntimeRules is incorrect")
        }
    }

    fun findEmptyRule(ruleThatIsEmpty: RuntimeRule): RuntimeRule {
        throw UnsupportedOperationException()
    }

    fun findRuntimeRule(ruleName: String): RuntimeRule {
        throw UnsupportedOperationException()
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        throw UnsupportedOperationException()
    }
}