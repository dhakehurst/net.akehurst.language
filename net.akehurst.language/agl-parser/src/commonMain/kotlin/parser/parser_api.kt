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
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.sppt.api.SharedPackedParseTree

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

interface RuntimeSpine {
    val expectedNextTerminals: Set<Rule>
    val elements: List<Rule>
    val nextChildNumber: Int
}

/**
 * Options to configure the parsing of a sentence
 * there is no separate scanner, so scanner options are passed to the parser
 */
interface ParseOptions {
    var goalRuleName: String?
    var reportErrors: Boolean
    var reportGrammarAmbiguities: Boolean
    var cacheSkip: Boolean
}

interface ParseResult {
    val sppt: SharedPackedParseTree?
    val issues: IssueCollection<LanguageIssue>
}

class ParserTerminatedException : RuntimeException()

interface Parser {

    val ruleSet: RuleSet

    fun reset()

    fun interrupt(message: String)

    /**
     * It is not necessary to call this method, but doing so will speed up future calls to parse as it will build the internal caches for the parser,
     */
    fun buildFor(goalRuleName: String)

    /**
     * parse the inputText starting with the given grammar rule and return the shared packed parse Tree.
     *
     * @param goalRuleName
     * @param inputText
     * @return the result of parsing
     */
    fun parseForGoal(goalRuleName: String, sentenceText: String): ParseResult

    fun parse(sentenceText: String, options: ParseOptions): ParseResult

    /**
     * list of non-terminal or terminal runtime rules expected at the position
     *
     **/
    fun expectedAt(sentenceText: String, position: Int, options: ParseOptions): Set<RuntimeSpine>

    /*
     * List of terminal rules expected at the position
     */
    fun expectedTerminalsAt(sentenceText: String, position: Int, options: ParseOptions): Set<Rule>

}