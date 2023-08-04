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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.collections.toSeparatedList
import net.akehurst.language.agl.grammar.grammar.asm.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler2
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserFromTreeDataAbstract
import net.akehurst.language.agl.syntaxAnalyser.locationIn
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.SpptDataNodeInfo

internal class AglGrammarSyntaxAnalyser2(
    //val languageRegistry: LanguageRegistryDefault
) : SyntaxAnalyserFromTreeDataAbstract<List<Grammar>>() {

    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    private val _localStore = mutableMapOf<String, Any>()

    init {
        this.registerFor("grammarDefinition", this::grammarDefinition as BranchHandler2<List<Grammar>>)
        this.registerFor("namespace", this::namespace as BranchHandler2<Namespace>)
        this.registerFor("definitions", this::definitions as BranchHandler2<List<Grammar>>)
        this.registerFor("grammar", this::grammar as BranchHandler2<Grammar>)
        this.registerFor("extendsOpt", this::extendsOpt as BranchHandler2<List<GrammarReference>>)
        this.registerFor("extends", this::extends as BranchHandler2<List<GrammarReference>>)
        this.registerFor("extendsList", this::extendsList as BranchHandler2<List<GrammarReference>>)
        this.registerFor("rules", this::rules as BranchHandler2<List<GrammarRule>>)
        this.registerFor("rule", this::rule as BranchHandler2<GrammarItem>)
        this.registerFor("grammarRule", this::grammarRule as BranchHandler2<GrammarRule>)
        this.registerFor("preferenceRule", this::preferenceRule as BranchHandler2<PreferenceRule>)
        this.registerFor("ruleTypeLabels", this::ruleTypeLabels as BranchHandler2<List<String>>)
        // this.register("ruleType", this::ruleType as BranchHandler2<GrammarRule>)
        this.registerFor("rhs", this::rhs as BranchHandler2<RuleItem>)
        this.registerFor("empty", this::empty as BranchHandler2<RuleItem>)
        this.registerFor("choice", this::choice as BranchHandler2<RuleItem>)
        this.registerFor("simpleChoice", this::simpleChoice as BranchHandler2<RuleItem>)
        this.registerFor("priorityChoice", this::priorityChoice as BranchHandler2<RuleItem>)
        this.registerFor("ambiguousChoice", this::ambiguousChoice as BranchHandler2<RuleItem>)
        this.registerFor("concatenation", this::concatenation as BranchHandler2<Concatenation>)
        this.registerFor("concatenationItem", this::concatenationItem as BranchHandler2<ConcatenationItem>)
        this.registerFor("simpleItemOrGroup", this::simpleItemOrGroup as BranchHandler2<SimpleItem>)
        this.registerFor("simpleItem", this::simpleItem as BranchHandler2<SimpleItem>)
        this.registerFor("listOfItems", this::listOfItems as BranchHandler2<ListOfItems>)
        this.registerFor("multiplicity", this::multiplicity as BranchHandler2<Pair<Int, Int>>)
        this.registerFor("range", this::range as BranchHandler2<Pair<Int, Int>>)
        this.registerFor("rangeUnBraced", this::rangeUnBraced as BranchHandler2<Pair<Int, Int>>)
        this.registerFor("rangeBraced", this::rangeBraced as BranchHandler2<Pair<Int, Int>>)
        this.registerFor("rangeMaxOpt", this::rangeMaxOpt as BranchHandler2<Int>)
        this.registerFor("rangeMax", this::rangeMax as BranchHandler2<Int>)
        this.registerFor("rangeMaxBounded", this::rangeMaxBounded as BranchHandler2<Int>)
        this.registerFor("rangeMaxUnbounded", this::rangeMaxUnbounded as BranchHandler2<Int>)
        this.registerFor("simpleList", this::simpleList as BranchHandler2<SimpleList>)
        this.registerFor("group", this::group as BranchHandler2<Group>)
        this.registerFor("groupedContent", this::groupedContent as BranchHandler2<RuleItem>)
        this.registerFor("separatedList", this::separatedList as BranchHandler2<SeparatedList>)
        this.registerFor("nonTerminal", this::nonTerminal as BranchHandler2<NonTerminal>)
        this.registerFor("embedded", this::embedded as BranchHandler2<Embedded>)
        this.registerFor("terminal", this::terminal as BranchHandler2<Terminal>)
        this.registerFor("qualifiedName", this::qualifiedName as BranchHandler2<String>)
        this.registerFor("preferenceOption", this::preferenceOption as BranchHandler2<PreferenceOption>)
        this.registerFor("choiceNumber", this::choiceNumber as BranchHandler2<Int?>)
        this.registerFor("associativity", this::associativity as BranchHandler2<Int?>)
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        //TODO
        return emptyList()
    }

    // grammarDefinition : namespace definitions ;
    private fun grammarDefinition(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<Grammar> {
        val namespace = children[0]
        val definitions = children[1] as List<Grammar>
        return definitions
    }

    // definitions = grammar+ ;
    private fun definitions(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<Grammar> {
        val definitions = children as List<Grammar>
        return definitions
    }

    // namespace : 'namespace' qualifiedName ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Namespace {
        val qualifiedName = children[1] as String
        val ns = NamespaceDefault(qualifiedName).also { this.locationMap[it] = target.node.locationIn(sentence) }
        _localStore["namespace"] = ns
        return ns
    }

    // grammar : 'grammar' IDENTIFIER extendsOpt '{' rules '}' ;
    private fun grammar(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Grammar {
        val namespace = _localStore["namespace"] as Namespace
        val name = children[1] as String
        val extends = children[2] as List<GrammarReference>
        val grmr = GrammarDefault(namespace, name).also { this.locationMap[it] = target.node.locationIn(sentence) }
        grmr.extends.addAll(extends)
        _localStore["grammar"] = grmr
        val rules = children[4] as List<GrammarItem>
        rules.forEach {
            (it as GrammarItemAbstract).grammar = grmr
            when (it) {
                is GrammarRule -> grmr.grammarRule.add(it)
                is PreferenceRule -> grmr.preferenceRule.add(it)
                else -> error("Not handled")
            }
        }
        return grmr
    }

    // extendsOpt = extends?
    private fun extendsOpt(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<GrammarReference> {
        return when {
            null == children[0] -> emptyList()
            else -> children[0] as List<GrammarReference>
        }
    }

    // extends = 'extends' extendsList ;
    private fun extends(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<GrammarReference> {
        return children[1] as List<GrammarReference>
    }

    // extendsList = [qualifiedName / ',']+ ;
    private fun extendsList(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<GrammarReference> {
        val localNamespace = _localStore["namespace"] as Namespace
        val extendNameList = children as List<String>
        val sl = extendNameList.toSeparatedList<String, String>()
        val extendedGrammars = sl.items.map {
            GrammarReferenceDefault(localNamespace, it).also { this.locationMap[it] = target.node.locationIn(sentence) }
        }
        return extendedGrammars
    }

    // rules : rule+ ;
    private fun rules(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<GrammarItem> =
        children as List<GrammarItem>

    // rule = grammarRule | preferenceRule
    private fun rule(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): GrammarItem =
        children[0] as GrammarItem


    // grammarRule : ruleTypeLabels IDENTIFIER '=' rhs ';' ;
    private fun grammarRule(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): GrammarRule {
        val type = children[0] as List<String>
        val isOverride = type.contains("override")
        val isSkip = type.contains("skip")
        val isLeaf = type.contains("leaf")
        val name = children[1] as String
        val result = GrammarRuleDefault(name, isOverride, isSkip, isLeaf).also { this.locationMap[it] = target.node.locationIn(sentence) }
        val rhs = children[3] as RuleItem
        result.rhs = rhs
        return result
    }

    // ruleTypeLabels : isOverride isSkip isLeaf ;
    // isOverride = 'override' ? ;
    // isSkip = 'leaf' ? ;
    // isLeaf = 'skip' ? ;
    private fun ruleTypeLabels(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): List<String> {
        return (children as List<List<String>>).flatten()
    }

    // rhs = empty | concatenation | choice ;
    private fun rhs(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        val rhs = children[0] as RuleItem
        val rhs2 = when (rhs) {
            is Concatenation -> reduceConcatenation(rhs)
            else -> rhs
        }
        return rhs2
    }

    // empty = ;
    private fun empty(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        return EmptyRuleDefault().also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // choice = ambiguousChoice | priorityChoice | simpleChoice ;
    private fun choice(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        return children[0] as RuleItem
    }

    // simpleChoice : [concatenation, '|']* ;
    private fun simpleChoice(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        val simpleChoice = children.toSeparatedList<Concatenation, String>()
        val alternative = simpleChoice.items.map { reduceConcatenation(it) }
        return ChoiceLongestDefault(alternative).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // priorityChoice : [concatenation, '<']* ;
    private fun priorityChoice(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        val priorityChoice = children.toSeparatedList<Concatenation, String>()
        val alternative = priorityChoice.items.map { reduceConcatenation(it) }
        return ChoicePriorityDefault(alternative).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // ambiguousChoice : [concatenation, '||']* ;
    private fun ambiguousChoice(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        val ambiguousChoice = children.toSeparatedList<Concatenation, String>()
        val alternative = ambiguousChoice.items.map { reduceConcatenation(it) }
        return ChoiceAmbiguousDefault(alternative).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // concatenation : concatenationItem+ ;
    private fun concatenation(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Concatenation {
        val items = children as List<RuleItem>
        return ConcatenationDefault(items).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // concatenationItem = simpleItemOrGroup | listOfItems ; // a group can be mapped to a Choice so return RuleItem
    private fun concatenationItem(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem =
        children[0] as RuleItem

    // simpleItemOrGroup : simpleItem | group ; // a group can be mapped to a Choice so return RuleItem
    private fun simpleItemOrGroup(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem =
        children[0] as RuleItem

    // simpleItem : terminal | nonTerminal | embedded ;
    private fun simpleItem(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): SimpleItem =
        children[0] as SimpleItem

    // listOfItems = simpleList | separatedList ;
    // could also return optional
    private fun listOfItems(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): ConcatenationItem =
        children[0] as ConcatenationItem

    // multiplicity = '*' | '+' | '?' | range ;
    private fun multiplicity(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Pair<Int, Int> {
        val m = children[0]
        return when (m) {
            is String -> when (m) {
                "*" -> Pair(0, -1)
                "+" -> Pair(1, -1)
                "?" -> Pair(0, 1)
                else -> error("should not happen")
            }

            is Pair<*, *> -> m as Pair<Int, Int>
            else -> error("should not happen")
        }
    }

    //range = rangeBraced | rangeUnBraced ;
    private fun range(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Pair<Int, Int> =
        children[0] as Pair<Int, Int>

    //rangeUnBraced = POSITIVE_INTEGER rangeMaxOpt ;
    private fun rangeUnBraced(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Pair<Int, Int> {
        val min = (children[0] as String).toInt()
        val max = if (children[1] == null) {
            min
        } else {
            children[1] as Int
        }
        return Pair(min, max)
    }

    //rangeBraced = '{' POSITIVE_INTEGER rangeMaxOpt '}' ;
    private fun rangeBraced(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Pair<Int, Int> {
        val min = (children[1] as String).toInt()
        val max = if (null == children[2]) {
            min
        } else {
            children[2] as Int
        }
        return Pair(min, max)
    }

    // rangeMaxOpt = rangeMax? ;
    private fun rangeMaxOpt(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Int? {
        return children[0] as Int?
    }

    //rangeMax = rangeMaxUnbounded | rangeMaxBounded ;
    private fun rangeMax(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Int =
        children[0] as Int


    //rangeMaxUnbounded = '+' ;
    private fun rangeMaxUnbounded(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Int = -1

    //rangeMaxBounded = '..' POSITIVE_INTEGER ;
    private fun rangeMaxBounded(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Int =
        (children[1] as String).toInt()

    // simpleList = simpleItem multiplicity ;
    private fun simpleList(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): ConcatenationItem {
        val (min, max) = children[1] as Pair<Int, Int>
        val item = children[0] as RuleItem
        return when {
            min == 0 && max == 1 -> OptionalItemDefault(item).also { this.locationMap[it] = target.node.locationIn(sentence) }
            else -> SimpleListDefault(min, max, item).also { this.locationMap[it] = target.node.locationIn(sentence) }
        }
    }

    // separatedList : '[' simpleItem '/' terminal ']' multiplicity ;
    private fun separatedList(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): SeparatedList {
        val (min, max) = children[5] as Pair<Int, Int>
        val separator = children[3] as RuleItem
        val item = children[1] as RuleItem
        return SeparatedListDefault(min, max, item, separator).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // group = '(' groupedContent ')' ;
    private fun group(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem {
        val groupContent = children[1] as RuleItem
        return when (groupContent) {
            is Choice -> groupContent.also { this.locationMap[it] = target.node.locationIn(sentence) }
            is Concatenation -> {
                val reduced = reduceConcatenation(groupContent)
                GroupDefault(reduced).also { this.locationMap[it] = target.node.locationIn(sentence) }
            }

            else -> error("Internal Error: subtype of RuleItem not handled - ${groupContent::class.simpleName}")
        }
    }

    // groupedContent = concatenation | choice ;
    private fun groupedContent(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): RuleItem =
        children[0] as RuleItem


    // nonTerminal : IDENTIFIER ;
    private fun nonTerminal(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): NonTerminal {
        val nonTerminalRef = children[0] as String
        val nt = NonTerminalDefault(nonTerminalRef).also { this.locationMap[it] = target.node.locationIn(sentence) }
        return nt
    }

    // embedded = qualifiedName '::' nonTerminal ;
    private fun embedded(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Embedded {
        val namespace = _localStore["namespace"] as Namespace
        val embeddedGrammarStr = children[0] as String
        val embeddedStartRuleRef = children[2] as NonTerminal
        val embeddedGrammarRef = GrammarReferenceDefault(namespace, embeddedGrammarStr).also { this.locationMap[it] = target.node.locationIn(sentence) }
        return EmbeddedDefault(embeddedStartRuleRef.name, embeddedGrammarRef).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // terminal : LITERAL | PATTERN ;
    private fun terminal(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Terminal {
        // Must match what is done in AglStyleSyntaxAnalyser.selectorSingle
        val isPattern = when (target.alt.option) {
            0 -> false
            else -> true
        }
        val mt = children[0] as String
        val escaped = mt.substring(1, mt.length - 1)
        //TODO: check these unescapings, e.g. '\\n'
        val value = if (isPattern) {
            escaped.replace("\\\"", "\"")
        } else {
            escaped.replace("\\'", "'").replace("\\\\", "\\")
        }
        return TerminalDefault(value, isPattern).also { this.locationMap[it] = target.node.locationIn(sentence) }
    }

    // qualifiedName : (IDENTIFIER / '.')+ ;
    private fun qualifiedName(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): String {
        return (children as List<String>).joinToString(separator = "")
    }

    // preferenceRule = 'preference' simpleItem '{' preferenceOptionList '}' ;
    private fun preferenceRule(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): PreferenceRule {
        val forItem = children[1] as SimpleItem
        val optionList = children[3] as List<PreferenceOption>
        return PreferenceRuleDefault(forItem, optionList)
    }

    // preferenceOption = nonTerminal choiceNumber 'on' terminalList associativity ;
    private fun preferenceOption(target: SpptDataNodeInfo, children: List<Any?>, arg: Any?): PreferenceOption {
        val item = children[0] as NonTerminal
        val choiceNumber = when {
            null == children[1] -> 0
            else -> children[1] as Int
        }
        val terminalList = children[3] as List<SimpleItem>
        val assStr = children[4] as String
        val associativity = when (assStr) {
            "left" -> PreferenceOption.Associativity.LEFT
            "right" -> PreferenceOption.Associativity.RIGHT
            else -> error("Internal Error: associativity value '$assStr' not supported")
        }
        return PreferenceOptionDefault(item, choiceNumber, terminalList, associativity)
    }

    // choiceNumber = POSITIVE_INTEGER? ;
    private fun choiceNumber(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): Int? {
        val n = children[0] as String?
        return n?.toInt()
    }

    // associativity = 'left' | 'right' ;
    private fun associativity(target: SpptDataNodeInfo, children: List<Any?>, sentence: String): String =
        children[0] as String

    private fun reduceConcatenation(concat: Concatenation): RuleItem {
        return when (concat.items.size) {
            1 -> when (concat.items[0]) {
                is TangibleItem -> concat.items[0]
                is OptionalItem -> concat.items[0]
                is ListOfItems -> concat.items[0]
                is Choice -> concat.items[0]
                is Group -> {
                    val content = (concat.items[0] as Group).groupedContent
                    when (content) {
                        is Choice -> content
                        else -> concat
                    }
                }

                else -> error("Internal Error: subtype of ConcatenationItem not handled - ${concat.items[0]::class.simpleName}")
            }

            else -> concat
        }
    }

}