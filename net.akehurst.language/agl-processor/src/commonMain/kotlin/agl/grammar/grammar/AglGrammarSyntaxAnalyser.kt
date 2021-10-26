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

import net.akehurst.language.agl.grammar.grammar.asm.*
import net.akehurst.language.agl.grammar.GrammarRegistryDefault
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.analyser.SyntaxAnalyserException
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree


internal class AglGrammarSyntaxAnalyser(
    val grammarRegistry: GrammarRegistry
) : SyntaxAnalyserAbstract<List<Grammar>, GrammarContext>() {

    var grammarLoader: GrammarLoader? = null
    private val _issues = mutableListOf<LanguageIssue>()

    init {
        this.register("grammarDefinition", this::grammarDefinition as BranchHandler<Grammar>)
        this.register("namespace", this::namespace as BranchHandler<Namespace>)
        this.register("definitions", this::definitions as BranchHandler<List<Grammar>>)
        this.register("grammar", this::grammar as BranchHandler<Grammar>)
        this.register("extends", this::extends as BranchHandler<List<Grammar>>)
        this.register("rules", this::rules as BranchHandler<List<Rule>>)
        this.register("rule", this::rule as BranchHandler<Rule>)
        this.register("ruleTypeLabels", this::ruleTypeLabels as BranchHandler<List<String>>)
        // this.register("ruleType", this::ruleType as BranchHandler<Rule>)
        this.register("rhs", this::rhs as BranchHandler<Rule>)
        this.register("empty", this::empty as BranchHandler<Rule>)
        this.register("choice", this::choice as BranchHandler<RuleItem>)
        this.register("simpleChoice", this::simpleChoice as BranchHandler<RuleItem>)
        this.register("priorityChoice", this::priorityChoice as BranchHandler<RuleItem>)
        this.register("ambiguousChoice", this::ambiguousChoice as BranchHandler<RuleItem>)
        this.register("concatenation", this::concatenation as BranchHandler<Concatenation>)
        this.register("concatenationItem", this::concatenationItem as BranchHandler<ConcatenationItem>)
        this.register("simpleItem", this::simpleItem as BranchHandler<SimpleItem>)
        this.register("listOfItems", this::listOfItems as BranchHandler<ListOfItems>)
        this.register("multiplicity", this::multiplicity as BranchHandler<Pair<Int, Int>>)
        this.register("simpleList", this::simpleList as BranchHandler<SimpleList>)
        this.register("group", this::group as BranchHandler<Group>)
        this.register("groupedContent", this::groupedContent as BranchHandler<Group>)
        this.register("separatedList", this::separatedList as BranchHandler<SeparatedList>)
        this.register("nonTerminal", this::nonTerminal as BranchHandler<NonTerminal>)
        this.register("terminal", this::terminal as BranchHandler<Terminal>)
        this.register("qualifiedName", this::qualifiedName as BranchHandler<String>)
    }

    override fun clear() {
        _issues.clear()
    }

    override fun configure(configurationContext: SentenceContext, configuration: String): List<LanguageIssue> {
        //TODO
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, context: GrammarContext?): Pair<List<Grammar>, List<LanguageIssue>> {
        val grammars = this.transformBranch<List<Grammar>>(sppt.root.asBranch, "")
        return Pair(grammars, _issues) //TODO
    }

    // grammarDefinition : namespace definitions ;
    private fun grammarDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val namespace = this.transformBranch<Namespace>(children[0], null)
        val definitions = this.transformBranch<List<Grammar>>(children[1], namespace)
        return definitions
    }

    // definitions = grammar+ ;
    private fun definitions(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val definitions = children.map {
            this.transformBranch<Grammar>(it, arg)
        }
        return definitions
    }

    // namespace : 'namespace' qualifiedName ;
    fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Namespace {
        val qualifiedName = this.transformBranch<String>(children[0], null)
        return NamespaceDefault(qualifiedName)
    }

    // grammar : 'grammar' IDENTIFIER extends? '{' rules '}' ;
    fun grammar(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Grammar {
        val namespace = arg as Namespace
        val name = target.nonSkipChildren[1].nonSkipMatchedText
        val extends = this.transformBranch<List<Grammar>>(children[0], namespace)
        val result = GrammarDefault(namespace, name)
        result.extends.addAll(extends)

        this.grammarRegistry.register(result)

        this.transformBranch<List<Rule>>(children[1], result) //creating a Rule adds it to the grammar

        return result
    }

    // extends : 'extends' [qualifiedName / ',']+ ;
    fun extends(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val localNamespace = arg as Namespace
        return if (children.isEmpty()) {
            emptyList<Grammar>()
        } else {
            val extendNameList = children[0].branchNonSkipChildren[0].branchNonSkipChildren.map { it.nonSkipMatchedText }
            val extendedGrammars = extendNameList.map {
                this.grammarRegistry.find(localNamespace.qualifiedName, it)
            }
            extendedGrammars
        }
    }

    // rules : rule+ ;
    fun rules(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Rule> {
        return children.mapIndexed { index, it ->
            this.transformBranch<Rule>(it, arg)
        }
    }

    // rule : ruleTypeLabels IDENTIFIER ':' rhs ';' ;
    fun rule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Rule {
        val grammar = arg as GrammarDefault
        val type = this.transformBranch<List<String>>(children[0], arg)
        val isOverride = type.contains("override")
        val isSkip = type.contains("skip")
        val isLeaf = type.contains("leaf")
        val name = target.nonSkipChildren[1].nonSkipMatchedText
        val result = RuleDefault(grammar, name, isOverride, isSkip, isLeaf)
        val rhs = this.transformBranch<RuleItem>(children[1], arg)
        result.rhs = rhs
        return result
    }

    // ruleTypeLabels : isSkip isLeaf ;
    // isOverride = 'override' ? ;
    // isSkip = 'leaf' ? ;
    // isLeaf = 'skip' ? ;
    fun ruleTypeLabels(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<String> {
        return children.mapNotNull {
            when {
                it.nonSkipChildren[0].isEmptyLeaf -> null
                else -> it.nonSkipChildren[0].nonSkipMatchedText
            }
        }
    }

    // rhs = empty | concatenation | choice ;
    fun rhs(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return this.transformBranch<RuleItem>(children[0], arg)
    }

    // empty = ;
    fun empty(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return EmptyRuleDefault()
    }

    // choice = ambiguousChoice | priorityChoice | simpleChoice ;
    fun choice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return this.transformBranch<RuleItem>(children[0], arg)
    }

    // simpleChoice : [concatenation, '|']* ;
    fun simpleChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        // children will have one element, an sList
        val alternative = children.mapIndexed { index, it ->
            this.transformBranch<Concatenation>(it, arg)
        }
        return ChoiceLongestDefault(alternative)
    }

    // priorityChoice : [concatenation, '<']* ;
    fun priorityChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children.mapIndexed { index, it ->
            this.transformBranch<Concatenation>(it, arg)
        }
        return ChoicePriorityDefault(alternative)
    }

    // ambiguousChoice : [concatenation, '||']* ;
    fun ambiguousChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children.mapIndexed { index, it ->
            this.transformBranch<Concatenation>(it, arg)
        }
        return ChoiceAmbiguousDefault(alternative)
    }

    // concatenation : concatenationItem+ ;
    fun concatenation(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Concatenation {
        val items = children.mapIndexed { index, it ->
            this.transformBranch<ConcatenationItem>(it, arg)
        }
        return ConcatenationDefault(items)
    }

    // concatenationItem = simpleItem | listOfItems ;
    fun concatenationItem(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): ConcatenationItem {
        return this.transformBranch<ConcatenationItem>(children[0], arg)
    }

    // simpleItem : terminal | nonTerminal | group ;
    fun simpleItem(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SimpleItem {
        return this.transformBranch<SimpleItem>(children[0], arg)
    }

    // listOfItems = simpleList | separatedList ;
    fun listOfItems(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): ListOfItems {
        return this.transformBranch<ListOfItems>(children[0], arg)
    }

    // multiplicity = '*' | '+' | '?' | oneOrMore | range ;
    // oneOrMore = POSITIVE_INTEGER '+' ;
    // range = POSITIVE_INTEGER '..' POSITIVE_INTEGER ;
    fun multiplicity(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Pair<Int, Int> {
        val symbol = target.nonSkipMatchedText
        return when (symbol) {
            "*" -> Pair(0, -1)
            "+" -> Pair(1, -1)
            "?" -> Pair(0, 1)
            else -> {
                val multArgs = children[0].nonSkipChildren
                when (multArgs.size) {
                    2 -> {
                        val min = multArgs[0].nonSkipMatchedText.toInt()
                        Pair(min, -1)
                    }
                    3 -> {
                        val min = multArgs[0].nonSkipMatchedText.toInt()
                        val max = multArgs[2].nonSkipMatchedText.toInt()
                        Pair(min, max)
                    }
                    else -> throw SyntaxAnalyserException("cannot transform ${target}", null)
                }
            }

        }
    }

    // simpleList = simpleItem multiplicity ;
    fun simpleList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SimpleList {
        val (min, max) = this.transformBranch<Pair<Int, Int>>(children[1], arg)
        val item = this.transformBranch<SimpleItem>(children[0], arg)
        return SimpleListDefault(min, max, item)
    }

    // separatedList : '[' simpleItem '/' terminal ']' multiplicity ;
    fun separatedList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SeparatedList {
        val (min, max) = this.transformBranch<Pair<Int, Int>>(children[2], arg)
        val separator = this.transformBranch<SimpleItem>(children[1], arg)
        val item = this.transformBranch<SimpleItem>(children[0], arg)
        return SeparatedListDefault(min, max, item, separator, SeparatedListKind.Flat)
    }

    // group : '(' choice ')' ;
    fun group(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Group {
        val groupContent = this.transformBranch<RuleItem>(children[0], arg)
        return when (groupContent) {
            is Choice -> GroupDefault(groupContent)
            is Concatenation -> GroupDefault(ChoiceLongestDefault(listOf(groupContent)))
            else -> error("Intertnal Error: type of group content not handled '${groupContent::class.simpleName}'")
        }
    }

    fun groupedContent(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val content = this.transformBranch<RuleItem>(children[0], arg)
        return content
    }

    // nonTerminal : IDENTIFIER ;
    fun nonTerminal(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): NonTerminal {
        val thisGrammar = arg as Grammar
        val nonTerminalRef = target.nonSkipChildren[0].nonSkipMatchedText
        return if (nonTerminalRef.contains(".")) {
            val embeddedGrammarRef = nonTerminalRef.substringBeforeLast(".")
            val embeddedStartRuleRef = nonTerminalRef.substringAfterLast(".")
            val embeddedGrammar = GrammarRegistryDefault.find(thisGrammar.namespace.qualifiedName, embeddedGrammarRef)
            NonTerminalDefault(embeddedStartRuleRef, embeddedGrammar, true)
        } else {
            NonTerminalDefault(nonTerminalRef, thisGrammar, false)
        }
    }

    // terminal : LITERAL | PATTERN ;
    fun terminal(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Terminal {
        val isPattern = target.nonSkipChildren[0].name == "PATTERN"
        val mt = target.nonSkipMatchedText
        val escaped = mt.substring(1, mt.length - 1)
        //TODO: check these unescapings, e.g. '\\n'
        val value = if (isPattern) {
            escaped.replace("\\\"", "\"")
        } else {
            escaped.replace("\\'", "'").replace("\\\\", "\\")
        }
        return TerminalDefault(value, isPattern)
    }

    // qualifiedName : (IDENTIFIER / '.')+ ;
    fun qualifiedName(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return target.nonSkipMatchedText //children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }.joinToString(".")
    }

}