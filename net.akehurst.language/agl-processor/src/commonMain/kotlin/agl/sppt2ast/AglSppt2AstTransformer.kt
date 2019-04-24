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

package net.akehurst.language.agl.sppt2ast

import net.akehurst.language.api.analyser.GrammarLoader
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt2ast.UnableToTransformSppt2AstExeception
import net.akehurst.language.agl.ast.*
import net.akehurst.language.processor.BranchHandler
import net.akehurst.language.processor.SyntaxAnalyserAbstract

class AglSppt2AstTransformer : SyntaxAnalyserAbstract() {

    var grammarLoader: GrammarLoader? = null

    init {
        this.register("grammarDefinition", this::grammarDefinition as BranchHandler<Grammar>)
        this.register("namespace", this::namespace as BranchHandler<Namespace>)
        this.register("grammar", this::grammar as BranchHandler<Grammar>)
        this.register("extends", this::extends as BranchHandler<List<Grammar>>)
        this.register("rules", this::rules as BranchHandler<List<Rule>>)
        this.register("anyRule", this::anyRule as BranchHandler<Rule>)
        this.register("normalRule", this::normalRule as BranchHandler<Rule>)
        this.register("skipRule", this::skipRule as BranchHandler<Rule>)
        this.register("choice", this::choice as BranchHandler<RuleItem>)
        this.register("simpleChoice", this::simpleChoice as BranchHandler<RuleItem>)
        this.register("priorityChoice", this::priorityChoice as BranchHandler<RuleItem>)
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
        this.register("IDENTIFIER", this::IDENTIFIER as BranchHandler<String>)
        this.register("PATTERN", this::PATTERN as BranchHandler<Terminal>)
        this.register("LITERAL", this::LITERAL as BranchHandler<Terminal>)
    }

    override fun clear() {

    }

    override fun <T> transform(sppt: SharedPackedParseTree): T {
        return this.transform<T>(sppt.root.asBranch, "")
    }

    //   grammarDefinition : namespace grammar ;
    fun grammarDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Grammar {
        val namespace = this.transform<Namespace>(children[0], null)
        return this.transform<Grammar>(children[1], namespace)
    }

    // namespace : 'namespace' qualifiedName ;
    fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Namespace {
        val qualifiedName = this.transform<String>(children[0], null)
        return NamespaceDefault(qualifiedName)
    }

    // grammar : 'grammar' IDENTIFIER extends? '{' rules '}' ;
    fun grammar(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Grammar {
        val namespace = arg as Namespace
        val name = children[0].nonSkipMatchedText
        val extends = this.transform<List<Grammar>>(children[1], null)
        val result = GrammarDefault(namespace, name, mutableListOf())
        result.extends.addAll(extends)

        this.transform<List<Rule>>(children[2], result) //creating a Rule adds it to the grammar

        return result
    }

    // extends : 'extends' [qualifiedName / ',']+ ;
    fun extends(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        //TODO:
        return emptyList<Grammar>()
    }

    // rules : anyRule+ ;
    fun rules(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Rule> {
        // children will have one element, a multi.
        return children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<Rule>(it, arg)
        }
    }

    // anyRule : normalRule | skipRule ;
    fun anyRule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Rule {
        return this.transform<Rule>(children[0], arg)
    }

    // normalRule : IDENTIFIER ':' choice ';' ;
    fun normalRule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Rule {
        val grammar = arg as GrammarDefault
        val name = this.transform<String>(children[0], null)
        val result = RuleDefault(grammar, name, false)
        val rhs = this.transform<RuleItem>(children[1], arg)
        result.rhs = rhs
        return result
    }

    // skipRule : 'skip' IDENTIFIER ':' choice ';' ;
    fun skipRule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Rule {
        val grammar = arg as GrammarDefault
        val name = this.transform<String>(children[0], null)
        val result = RuleDefault(grammar, name, true)
        val rhs = this.transform<RuleItem>(children[1], arg)
        result.rhs = rhs
        return result
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
        } else  {
            ChoiceEqualDefault(alternative)
        }
    }

    // priorityChoice : [concatenation / '<']* ;
    fun priorityChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transform<Concatenation>(it, arg)
        }
        return if (alternative.isEmpty()) {
            EmptyRuleDefault()
        } else  {
            ChoicePriorityDefault(alternative)
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

    // multiplicity : '*' | '+' | '?'
    fun multiplicity(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Pair<Int, Int> {
        val symbol = target.nonSkipMatchedText
        return when (symbol) {
            "*" -> Pair(0, -1)
            "+" -> Pair(1, -1)
            "?" -> Pair(0, 1)
            else -> throw UnableToTransformSppt2AstExeception("cannot transform ${target}", null)
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
        val separator = this.transform<Terminal>(children[1], arg)
        val item = this.transform<SimpleItem>(children[0], arg)
        return SeparatedListDefault(min, max, separator, item)
    }

    // nonTerminal : IDENTIFIER ;
    fun nonTerminal(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): NonTerminal {
        val nonTerminalRef = this.transform<String>(children[0], arg)

        return NonTerminalDefault(nonTerminalRef)
    }

    // terminal : LITERAL | PATTERN ;
    fun terminal(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Terminal {
        return this.transform<Terminal>(children[0], arg)
    }

    // qualifiedName : (IDENTIFIER / '.')+ ;
    fun qualifiedName(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }.joinToString(".")
    }

    // IDENTIFIER : "[a-zA-Z_][a-zA-Z_0-9]*" ;
    fun IDENTIFIER(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return target.nonSkipMatchedText
    }

    // LITERAL : "'(?:\\\\?.)*?'" ;
    fun LITERAL(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Terminal {
        val mt = target.nonSkipMatchedText
        val value = mt.substring(1,mt.length-1)
        return TerminalDefault(value, false)
    }

    // PATTERN : "\"(?:\\\\?.)*?\"" ;
    fun PATTERN(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Terminal {
        val mt = target.nonSkipMatchedText
        val value = mt.substring(1,mt.length-1)
        return TerminalDefault(value, true)
    }
}