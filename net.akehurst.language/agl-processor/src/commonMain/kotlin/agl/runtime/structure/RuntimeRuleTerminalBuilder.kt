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

class RuntimeRuleTerminalBuilder(val rrsb: RuntimeRuleSetBuilder) {


    private var kind: RuntimeRuleKind = RuntimeRuleKind.TERMINAL
    private var isPattern: Boolean = false
    private var isSkip: Boolean = false

    fun skip(value:Boolean): RuntimeRuleTerminalBuilder {
        this.isSkip = value
        return this
    }

    fun literal(value: String) : RuntimeRule {
        val name = "'$value'"
        val rr = RuntimeRule(this.rrsb.rules.size, name, value, kind, isPattern, isSkip)
        this.rrsb.rules.add(rr)
        return rr
    }

    fun literal(name:String, value: String) : RuntimeRule {
        val rr = RuntimeRule(this.rrsb.rules.size, name, value, kind, isPattern, isSkip)
        this.rrsb.rules.add(rr)
        return rr
    }

    fun pattern(pattern: String) : RuntimeRule {
        val name = "\"$pattern\""
        this.isPattern = true
        val rr = RuntimeRule(this.rrsb.rules.size, name, pattern, kind, isPattern, isSkip)
        this.rrsb.rules.add(rr)
        return rr
    }
    fun pattern(name:String, pattern: String) : RuntimeRule {
        this.isPattern = true
        val rr = RuntimeRule(this.rrsb.rules.size, name, pattern, kind, isPattern, isSkip)
        this.rrsb.rules.add(rr)
        return rr
    }
    fun empty(ruleThatIsEmpty: RuntimeRule): RuntimeRule {
        val name = "§empty."+ruleThatIsEmpty.tag
        val rr = RuntimeRule(this.rrsb.rules.size, name, name, kind, isPattern, isSkip)
        this.rrsb.rules.add(rr)
        rr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.EMPTY,RuntimeRuleChoiceKind.NONE,0,0, arrayOf(ruleThatIsEmpty))
        return rr
    }

}