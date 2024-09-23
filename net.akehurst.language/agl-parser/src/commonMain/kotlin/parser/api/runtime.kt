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

package net.akehurst.language.parser.api

import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonKind

interface RuleSet {
    val nonSkipTerminals: List<Rule>
    val terminals: List<Rule>

    fun automatonFor(goalRuleName: String, automatonKind: AutomatonKind): Automaton
    fun usedAutomatonFor(goalRuleName: String):Automaton
    fun addPreBuiltFor(userGoalRuleName: String, automaton: Automaton)
}

interface Rule {
    val tag: String

    val isSkip: Boolean

    /**
     * pseudo rules are created where there is not a 1:1 mapping from (user-defined) grammar-rule to runtime-rule
     */
    val isPseudo: Boolean

    /**
     * Empty, Literal, Pattern, Embedded
     */
    val isTerminal: Boolean
    val isEndOfText:Boolean
    val isEmptyTerminal: Boolean
    val isEmptyListTerminal: Boolean
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

    /**
     * will throw an error if this rule is not a Terminal
     */
    val unescapedTerminalValue: String
}

interface PrefRule {
    val contextRule: Rule
}

interface RulePosition {
    val rule: Rule
    val option: Int
    val position: Int

    companion object {
        const val START_OF_RULE = 0
        const val END_OF_RULE = -1

        const val OPTION_OPTIONAL_ITEM = 0
        const val OPTION_OPTIONAL_EMPTY = 1

        const val OPTION_MULTI_ITEM = 0
        const val OPTION_MULTI_EMPTY = 1

        const val OPTION_SLIST_ITEM_OR_SEPERATOR = 0
        const val OPTION_SLIST_EMPTY = 1

        //for use in multi and separated list
        const val POSITION_MULIT_ITEM = 1 //TODO: make -ve
        const val POSITION_SLIST_SEPARATOR = 1 //TODO: make -ve
        const val POSITION_SLIST_ITEM = 2 //TODO: make -ve
    }
}

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