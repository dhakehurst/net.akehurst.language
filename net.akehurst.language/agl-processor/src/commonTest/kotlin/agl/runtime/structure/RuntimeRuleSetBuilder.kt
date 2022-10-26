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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.api.parser.ParserException

@Deprecated("Use runtimeRuleSet { ... }")
internal class RuntimeRuleSetBuilder() {

    val runtimeRuleSet = RuntimeRuleSet(RuntimeRuleSet.nextRuntimeRuleSetNumber++)
    private var nextGroupNumber: Int = 0
    private var nextChoiceNumber: Int = 0
    private var nextMultiNumber: Int = 0
    private var nextListNumber: Int = 0

    val rules: MutableList<RuntimeRule> = mutableListOf()

    fun createGroupRuleName(parentRuleName: String): String {
        return "§${parentRuleName}§group" + this.nextGroupNumber++ //TODO: include original rule name fo easier debug
    }

    fun createChoiceRuleName(parentRuleName: String): String { //TODO: split into priority or simple choice type
        return "§${parentRuleName}§choice" + this.nextChoiceNumber++ //TODO: include original rule name fo easier debug
    }

    fun createMultiRuleName(parentRuleName: String): String {
        return "§${parentRuleName}§multi" + this.nextMultiNumber++ //TODO: include original rule name fo easier debug
    }

    fun createListRuleName(parentRuleName: String): String {
        return "§${parentRuleName}§sList" + this.nextListNumber++ //TODO: include original rule name fo easier debug
    }

    fun findRuleByName(ruleName: String, terminal: Boolean): RuntimeRule? {
        return this.rules.firstOrNull {
            if (terminal) {
                it.kind == RuntimeRuleKind.TERMINAL && it.tag == ruleName
            } else {
                it.tag == ruleName
            }
        }
    }

    fun literal(value: String): RuntimeRule {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this).literal(value)
        }
    }

    fun literal(name: String, value: String, isSkip: Boolean = false): RuntimeRule {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this).skip(isSkip).literal(name, value)
        }
    }

    fun pattern(pattern: String): RuntimeRule {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this).pattern(pattern)
        }
    }

    fun pattern(name: String, pattern: String, isSkip: Boolean = false): RuntimeRule {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this).skip(isSkip).pattern(name, pattern)
        }
    }

    fun empty(ruleThatIsEmpty: RuntimeRule): RuntimeRule {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this).empty(ruleThatIsEmpty)
        }
    }

    fun embedded(tag: String, name: String, embeddedRuntimeRuleSet: RuntimeRuleSet, embeddedStartRule: RuntimeRule): RuntimeRule {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this).embedded(tag,name,embeddedRuntimeRuleSet,embeddedStartRule)
        }
    }

    fun rule(name: String): RuntimeRuleNonTerminalBuilder {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleNonTerminalBuilder(this, name)
        }
    }

    fun rule(rule: RuntimeRule): RuntimeRuleExtender {
        if (this.runtimeRuleSet.runtimeRules.isNotEmpty()) {
            throw ParserException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleExtender(this, this.rules.first { it == rule })
        }
    }

    fun ruleSet(): RuntimeRuleSet {
        this.runtimeRuleSet.setRules(this.rules)
        return this.runtimeRuleSet
    }
}