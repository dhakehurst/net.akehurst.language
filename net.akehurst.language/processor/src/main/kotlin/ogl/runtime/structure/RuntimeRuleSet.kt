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

package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.parser.ParserConstructionFailedException

class RuntimeRuleSet(val rules: List<RuntimeRule>) {

    //TODO: are Arrays faster ?
    private val runtimeRules: Array<RuntimeRule?> = arrayOfNulls<RuntimeRule>(rules.size)
    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val emptyRulesFor: Array<RuntimeRule?> =arrayOfNulls<RuntimeRule>(rules.size)

    init {
        for (rrule in rules) {
//            if (null == rrule) {
//                throw ParserConstructionFailedException("RuntimeRuleSet must not contain a null rule!")
//            }
            this.runtimeRules[rrule.number] = rrule
            if (RuntimeRuleKind.NON_TERMINAL == rrule.kind) {
                //                  this.nodeTypes.set(i, rrule.name)
                this.nonTerminalRuleNumber[rrule.name] = rrule.number
            } else {
                terminalRuleNumber[rrule.name] = rrule.number
                if (rrule.isEmptyRule) {
                    this.emptyRulesFor[rrule.ruleThatIsEmpty.number] = rrule
                }
            }
        }
    }

    fun findEmptyRule(ruleThatIsEmpty: RuntimeRule): RuntimeRule {
        return this.emptyRulesFor[ruleThatIsEmpty.number] ?: throw ParseException("Empty rule for RuntimeRule ${ruleThatIsEmpty} not found")
    }

    fun findRuntimeRule(ruleName: String): RuntimeRule {
        val number = this.nonTerminalRuleNumber[ruleName]
                ?: throw ParseException("NonTerminal RuntimeRule ${ruleName} not found")
        return this.runtimeRules[number] ?: throw ParseException("NonTerminal RuntimeRule ${ruleName} not found")
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
                ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number]?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
    }
}