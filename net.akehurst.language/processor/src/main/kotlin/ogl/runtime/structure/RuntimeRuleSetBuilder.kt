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
import net.akehurst.language.api.processor.LanguageProcessorException
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.parser.sppt.SPPTBranchDefault
import net.akehurst.language.parser.sppt.SPPTLeafDefault

class RuntimeRuleSetBuilder() {

    val rules : MutableList<RuntimeRule> = mutableListOf()

    private var runtimeRuleSet : RuntimeRuleSet? = null

    fun literal(value : String) : RuntimeRule {
        if (null!=this.runtimeRuleSet) {
            throw ParseException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this, rules.size).literal(value)
        }
    }

    fun pattern(pattern : String) : RuntimeRule {
        if (null!=this.runtimeRuleSet) {
            throw ParseException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this, rules.size).pattern(pattern)
        }
    }

    fun empty(ruleThatIsEmpty: RuntimeRule) : RuntimeRule {
        if (null!=this.runtimeRuleSet) {
            throw ParseException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleTerminalBuilder(this, rules.size).empty(ruleThatIsEmpty)
        }
    }

    fun rule(name: String) : RuntimeRuleNonTerminalBuilder {
        if (null!=this.runtimeRuleSet) {
            throw ParseException("Must not add rules after creating the ruleSet")
        } else {
            return RuntimeRuleNonTerminalBuilder(this, rules.size, name)
        }
    }

    fun ruleSet() : RuntimeRuleSet {
        if (null==this.runtimeRuleSet) {
            this.runtimeRuleSet = RuntimeRuleSet(this.rules)
        }
        return this.runtimeRuleSet ?: throw ParseException("Should never happen")
    }
}