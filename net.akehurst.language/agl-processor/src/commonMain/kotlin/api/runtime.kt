/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.api.runtime

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet

interface RuleSet {
    companion object {
        fun build(name: String, init: RuleSetBuilder.() -> Unit): RuleSet = runtimeRuleSet(name, init)
    }
}

interface Rule {
    val tag: String

    /**
     * Empty, Literal, Pattern, Embedded
     */
    val isTerminal: Boolean
    val isEmptyTerminal: Boolean
    val isLiteral: Boolean
    val isPattern: Boolean
    val isEmbedded: Boolean

    val isChoice: Boolean
    val isList: Boolean
    val isListSimple: Boolean
    val isListSeparated: Boolean

    val isOptional: Boolean
    val isListOptional: Boolean

    val rhsItems: List<List<Rule>>
}

@DslMarker
internal annotation class RuntimeRuleSetDslMarker

@RuntimeRuleSetDslMarker
interface RuleSetBuilder {
    fun concatenation(ruleName: String, isSkip: Boolean = false, init: ConcatenationBuilder.() -> Unit)
    fun choiceLongest(ruleName: String, isSkip: Boolean = false, init: ChoiceBuilder.() -> Unit)
    fun choicePriority(ruleName: String, isSkip: Boolean = false, init: ChoiceBuilder.() -> Unit)
}

@RuntimeRuleSetDslMarker
interface ConcatenationBuilder {
    fun empty()
    fun literal(value: String)
    fun pattern(pattern: String)
    fun ref(name: String)
}

@RuntimeRuleSetDslMarker
interface ChoiceBuilder {
    fun concatenation(init: ConcatenationBuilder.() -> Unit)
    fun ref(ruleName: String)
    fun literal(value: String)
    fun pattern(pattern: String)
}