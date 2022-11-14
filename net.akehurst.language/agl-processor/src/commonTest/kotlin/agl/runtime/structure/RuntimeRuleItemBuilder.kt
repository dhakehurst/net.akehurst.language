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

internal class RuntimeRuleItemBuilder(
    private val rrb: RuntimeRuleNonTerminalBuilder,
    private val kind: RuntimeRuleRhsItemsKind,
    private val choiceKind: RuntimeRuleChoiceKind,
    private val listKind:RuntimeRuleListKind,
    private val items: Array<out RuntimeRule>
) {

    private var min: Int = 0
    private var max: Int = 0

    fun min(value: Int) : RuntimeRuleItemBuilder{
        this.min = value
        return this
    }

    fun max(value: Int) : RuntimeRuleItemBuilder{
        this.max = value
        return this
    }

    fun ruleItem() : RuntimeRuleRhs{
        return RuntimeRuleRhs(this.kind, this.choiceKind, this.listKind, this.min, this.max, this.items)
    }

    fun build() : RuntimeRule {
        val rhs = this.ruleItem()
        val rr = rrb.build()
        rr.rhsOpt = rhs
        return rr
    }
}