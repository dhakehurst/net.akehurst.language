/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.api

@DslMarker
annotation class RuntimeRuleSetDslMarker

@RuntimeRuleSetDslMarker
interface RuleSetBuilder {
    fun concatenation(ruleName: String, isSkip: Boolean = false, isPseudo: Boolean = false, init: ConcatenationBuilder.() -> Unit)
    fun choiceLongest(ruleName: String, isSkip: Boolean = false, isPseudo: Boolean = false, init: ChoiceBuilder.() -> Unit)
    fun choicePriority(ruleName: String, isSkip: Boolean = false, isPseudo: Boolean = false, init: ChoiceBuilder.() -> Unit)
    fun optional(ruleName: String, itemRef: String, isSkip: Boolean = false, isPseudo: Boolean = false)
    fun multi(ruleName: String, min: Int, max: Int, itemRef: String, isSkip: Boolean = false, isPseudo: Boolean = false)
    fun sList(ruleName: String, min: Int, max: Int, itemRef: String, sepRef: String, isSkip: Boolean = false, isPseudo: Boolean = false)
    fun embedded(ruleName: String, embeddedRuleSet: RuleSet, startRuleName: String, isSkip: Boolean = false, isPseudo: Boolean = false)

    fun literal(literalUnescaped: String, isSkip: Boolean = false)
    fun literal(name: String?, literalUnescaped: String, isSkip: Boolean = false)
    fun pattern(value: String, isSkip: Boolean = false)
    fun pattern(name: String?, patternUnescaped: String, isSkip: Boolean = false)

    fun preferenceFor(precedenceContextRuleName: String, init: PrecedenceBuilder.() -> Unit)
}

@RuntimeRuleSetDslMarker
interface ConcatenationBuilder {
    fun empty()
    fun emptyList()
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

interface PrecedenceBuilder {

    /**
     * indicate that @param ruleName is
     */
    //fun none(ruleName: String)

    /**
     * indicate that @param ruleName is left-associative
     */
    fun left(spine: List<String>, operatorRuleNames: Set<String>)

    /**
     * indicate that @param ruleName is right-associative
     */
    fun leftOption(spine: List<String>, option: OptionNum, operatorRuleNames: Set<String>)

    /**
     * indicate that @param ruleName is right-associative
     */
    fun right(spine: List<String>, operatorRuleNames: Set<String>)

    /**
     * indicate that @param ruleName is right-associative
     */
    fun rightOption(spine: List<String>, option: OptionNum, operatorRuleNames: Set<String>)
}