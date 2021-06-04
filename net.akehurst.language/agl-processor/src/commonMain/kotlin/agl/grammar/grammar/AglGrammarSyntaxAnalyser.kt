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

import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.syntaxAnalyser.GrammarLoader
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyserException
import net.akehurst.language.agl.ast.*
import net.akehurst.language.agl.grammar.GrammarRegistryDefault


class AglGrammarSyntaxAnalyser(
        val grammarRegistry: GrammarRegistry
) : SyntaxAnalyserAbstract() {

    var grammarLoader: GrammarLoader? = null

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
        this.register("choice", this::choice as BranchHandler<RuleItem>)
        this.register("simpleChoice", this::simpleChoice as BranchHandler<RuleItem>)
        this.register("priorityChoice", this::priorityChoice as BranchHandler<RuleItem>)
        this.register("ambiguousChoice", this::ambiguousChoice as BranchHandler<RuleItem>)
        this.register("concatenation", this::concatenation as BranchHandler<Concatenation>)
        this.register("concatenationItem", this::concatenationItem as BranchHandler<ConcatenationItem>)
        this.register("simpleItem", this::simpleItem as BranchHandler<SimpleItem>)
        this.register("multiplicity", this::multiplicity as BranchHandler<Pair<Int, Int>>)
        this.register("multi", this::multi as BranchHandler<Multi>)
        this.register("group", this::group as BranchHandler<Group>)
        this.register("separatedList", this::separatedList as BranchHandler<SeparatedList>)
        this.register("nonTerminal", this::nonTerminal as BranchHandler<NonTerminal>)
        this.register("terminal", this::terminal as BranchHandler<Terminal>)
        this.register("qualifiedName", this::qualifiedName as BranchHandler<String>)
    }

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return this.transform<T>(sppt.root.asBranch, "")
    }

    //   grammarDefinition : namespace grammar ;
    fun grammarDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val namespace = this.transform<Namespace>(children[0], null)
        val definitions = this.transform<List<Grammar>>(children[1], namespace)
        return definitions
    }

    // definitions = grammar+ ;
    fun definitions(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val definitions = target.branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.transform<Grammar>(it, arg)
        }
        return definitions
    }

    // namespace : 'namespace' qualifiedName ;
    fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Namespace {
        val qualifiedName = this.transform<String>(children[0], null)
        return NamespaceDefault(qualifiedName)
    }

    // grammar : 'grammar' IDENTIFIER extends? '{' rules '}' ;
    fun grammar(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Grammar {
        val namespace = arg as Namespace
        val name = target.nonSkipChildren[1].nonSkipMatchedText
        val extends = this.transform<List<Grammar>>(children[0], namespace)
        val result = GrammarDefault(namespace, name, mutableListOf())
        result.extends.addAll(extends)

        this.grammarRegistry.register(result)

        this.transform<List<Rule>>(children[1], result) //creating a Rule adds it to the grammar

        return result
    }

    // extends : 'extends' [qualifiedName / ',']+ ;
    fun extends(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val localNamespace = arg as Namespace
        return if (children[0].isEmptyMatch) {
            emptyList<Grammar>()
        } else {
            val extendNameList = children[0].branchNonSkipChildren[0].branchNonSkipChildren[0].branchNonSkipChildren[0].branchNonSkipChildren.map { it.nonSkipMatchedText }
            val extendedGrammars = extendNameList.map {
                this.grammarRegistry.find(localNamespace.qualifiedName, it)
            }
            extendedGrammars
        }
    }

    // rules : anyRule+ ;
    fun rules(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Rule> {
        // children will have one element, a multi.
        return children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<Rule>(it, arg)
        }
    }

    // rule : ruleTypeLabels IDENTIFIER ':' choice ';' ;
    fun rule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Rule {
        val grammar = arg as GrammarDefault
        val type = this.transform<List<String>>(children[0], arg)
        val isOverride = type.contains("override")
        val isSkip = type.contains("skip")
        val isLeaf = type.contains("leaf")
        val name = target.nonSkipChildren[1].nonSkipMatchedText
        val result = RuleDefault(grammar, name, isOverride, isSkip, isLeaf)
        val rhs = this.transform<RuleItem>(children[1], arg)
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
                it.branchNonSkipChildren[0].nonSkipChildren[0].isEmptyLeaf -> null
                else -> it.branchNonSkipChildren[0].nonSkipChildren[0].nonSkipMatchedText
            }
        }
    }

    // choice : simpleChoice < priorityChoice ;
    fun choice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return this.transform<RuleItem>(children[0], arg)
    }

    // simpleChoice : [concatenation / '|']* ;
    fun simpleChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        // children will have one element, an sList
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<Concatenation>(it, arg)
        }
        return if (alternative.isEmpty()) {
            EmptyRuleDefault()
        } else {
            ChoiceLongestDefault(alternative)
        }
    }

    // priorityChoice : [concatenation / '<']* ;
    fun priorityChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<Concatenation>(it, arg)
        }
        return if (alternative.isEmpty()) {
            EmptyRuleDefault()
        } else {
            ChoicePriorityDefault(alternative)
        }
    }
    // ambiguousChoice : [concatenation / '<']* ;
    fun ambiguousChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<Concatenation>(it, arg)
        }
        return if (alternative.isEmpty()) {
            EmptyRuleDefault()
        } else {
            ChoiceAmbiguousDefault(alternative)
        }
    }
    // concatenation : concatenationItem+ ;
    fun concatenation(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Concatenation {
        val items = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<ConcatenationItem>(it, arg)
        }
        return ConcatenationDefault(items)
    }

    // concatenationItem : simpleItem | multi | separatedList ;
    fun concatenationItem(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): ConcatenationItem {
        return this.transform<ConcatenationItem>(children[0], arg)
    }

    // simpleItem : terminal | nonTerminal | group ;
    fun simpleItem(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SimpleItem {
        return this.transform<SimpleItem>(children[0], arg)
    }

    // multiplicity : '*' | '+' | '?' | POSITIVE_INTEGER '+' | POSITIVE_INTEGER '..' POSITIVE_INTEGER ;
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

    // multi : simpleItem multiplicity ;
    fun multi(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Multi {
        val multiplicity = this.transform<Pair<Int, Int>>(children[1], arg)
        val min = multiplicity.first
        val max = multiplicity.second
        val item = this.transform<SimpleItem>(children[0], arg)
        return MultiDefault(min, max, item)
    }

    // group : '(' choice ')' ;
    fun group(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Group {
        val choice = this.transform<Choice>(children[0], arg)
        return GroupDefault(choice)
    }

    // separatedList : '[' simpleItem '/' terminal ']' multiplicity ;
    fun separatedList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SeparatedList {
        val multiplicity = this.transform<Pair<Int, Int>>(children[2], arg)
        val min = multiplicity.first
        val max = multiplicity.second
        val separator = this.transform<SimpleItem>(children[1], arg)
        val item = this.transform<SimpleItem>(children[0], arg)
        return SeparatedListDefault(min, max, separator, item)
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
        val value = if (isPattern) {
            escaped.replace("\\\"", "\"")
        } else {
            escaped.replace("\\'", "'").replace("\\\\","\\")
        }
        return TerminalDefault(value, isPattern)
    }

    // qualifiedName : (IDENTIFIER / '.')+ ;
    fun qualifiedName(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return target.nonSkipMatchedText //children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }.joinToString(".")
    }

}