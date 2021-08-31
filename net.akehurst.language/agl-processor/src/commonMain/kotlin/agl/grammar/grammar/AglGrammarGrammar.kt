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

import net.akehurst.language.agl.ast.GrammarAbstract
import net.akehurst.language.agl.ast.GrammarBuilderDefault
import net.akehurst.language.agl.ast.NamespaceDefault
import net.akehurst.language.api.grammar.Rule

/**
    grammar Agl {
        skip WHITESPACE = "\s+" ;
        skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
        skip SINGLE_LINE_COMMENT = "//.*?$" ;

        grammarDefinition = namespace definitions ;
        namespace = 'namespace' qualifiedName ;
        definitions = grammar+ ;
        grammar = 'grammar' IDENTIFIER extends? '{' rules '}' ;
        extends = 'extends' [qualifiedName, ',']+ ;
        rules = rule+ ;
        rule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
        rhs = empty | concatenation | choice ;
        ruleTypeLabels = 'override'? 'skip'? 'leaf'? ;
        empty = ;
        choice = ambiguousChoice | priorityChoice | simpleChoice ;
        ambiguousChoice = [ concatenation, '||' ]2+ ;
        priorityChoice = [ concatenation, '<' ]2+ ;
        simpleChoice = [ concatenation, '|' ]2+ ;
        concatenation = concatenationItem+ ;
        concatenationItem = simpleItem | listOfItems ;
        simpleItem = terminal | nonTerminal | group ;
        listOfItems = simpleList | separatedList ;
        multiplicity = '*' | '+' | '?' | oneOrMore | range ;
        oneOrMore = POSITIVE_INTEGER '+' ;
        range = POSITIVE_INTEGER '..' POSITIVE_INTEGER ;
        simpleList = simpleItem multiplicity ;
        separatedList = '[' simpleItem ',' terminal ']' multiplicity ;
        group = '(' choice ')' ;
        nonTerminal = qualifiedName ;
        terminal = LITERAL | PATTERN ;
        qualifiedName = [IDENTIFIER, '.']+ ;
        IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
        LITERAL = "'([^'\\]|\\'|\\\\)*'" ;
        PATTERN = "\"(\\\"|[^\"])*\"" ;
        POSITIVE_INTEGER = "[0-9]+" ;
    }
 */
internal class AglGrammarGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglGrammar", createRules()) {
    companion object {
        const val goalRuleName = "grammarDefinition"
    }
}

private fun createRules(): List<Rule> {
    val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglGrammar")
    b.skip("WHITESPACE", true).concatenation(b.terminalPattern("\\s+"))
    b.skip("MULTI_LINE_COMMENT", true).concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"))
    b.skip("SINGLE_LINE_COMMENT", true).concatenation(b.terminalPattern("//[^\\n\\r]*"))

    b.rule("grammarDefinition").concatenation(b.nonTerminal("namespace"), b.nonTerminal("definitions"))
    b.rule("definitions").multi(1, -1, b.nonTerminal("grammar"))
    b.rule("namespace").concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"))
    b.rule("grammar").concatenation(
        b.terminalLiteral("grammar"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("extends"),
        b.terminalLiteral("{"), b.nonTerminal("rules"), b.terminalLiteral("}")
    )
    b.rule("extends").multi(0, 1, b.nonTerminal("extends1"))
    b.rule("extends1").concatenation(b.terminalLiteral("extends"), b.nonTerminal("extends2"))
    b.rule("extends2").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("qualifiedName"))
    b.rule("rules").multi(1, -1, b.nonTerminal("rule"))
    b.rule("rule").concatenation(b.nonTerminal("ruleTypeLabels"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral("="), b.nonTerminal("rhs"), b.terminalLiteral(";"))
    b.rule("ruleTypeLabels").concatenation(b.nonTerminal("isOverride"), b.nonTerminal("isSkip"), b.nonTerminal("isLeaf"))
    b.rule("isOverride").multi(0, 1, b.terminalLiteral("override"))
    b.rule("isSkip").multi(0, 1, b.terminalLiteral("skip"))
    b.rule("isLeaf").multi(0, 1, b.terminalLiteral("leaf"))
    b.rule("rhs").choiceLongestFromConcatenationItem(b.nonTerminal("empty"), b.nonTerminal("concatenation"), b.nonTerminal("choice"))
    b.rule("empty").empty()
    b.rule("choice").choiceLongestFromConcatenationItem(b.nonTerminal("ambiguousChoice"), b.nonTerminal("priorityChoice"), b.nonTerminal("simpleChoice"))
    b.rule("ambiguousChoice").separatedList(2, -1, b.terminalLiteral("||"), b.nonTerminal("concatenation"))
    b.rule("priorityChoice").separatedList(2, -1, b.terminalLiteral("<"), b.nonTerminal("concatenation"))
    b.rule("simpleChoice").separatedList(2, -1, b.terminalLiteral("|"), b.nonTerminal("concatenation"))
    b.rule("concatenation").multi(1, -1, b.nonTerminal("concatenationItem"))
    b.rule("concatenationItem").choiceLongestFromConcatenationItem(b.nonTerminal("simpleItem"), b.nonTerminal("listOfItems"))
    b.rule("simpleItem").choiceLongestFromConcatenationItem(b.nonTerminal("terminal"), b.nonTerminal("nonTerminal"), b.nonTerminal("group"))
    b.rule("listOfItems").choiceLongestFromConcatenationItem(b.nonTerminal("simpleList"), b.nonTerminal("separatedList"))  // TODO: Associative lists
    b.rule("multiplicity").choiceLongestFromConcatenationItem(
        b.terminalLiteral("*"),
        b.terminalLiteral("+"),
        b.terminalLiteral("?"),
        b.nonTerminal("oneOrMore"),
        b.nonTerminal("range")
    )
    b.rule("oneOrMore").concatenation(b.nonTerminal("POSITIVE_INTEGER"), b.terminalLiteral("+"))
    b.rule("range").concatenation(b.nonTerminal("POSITIVE_INTEGER"), b.terminalLiteral(".."), b.nonTerminal("POSITIVE_INTEGER"))
    b.rule("simpleList").concatenation(b.nonTerminal("simpleItem"), b.nonTerminal("multiplicity"))
    b.rule("separatedList").concatenation(
        b.terminalLiteral("["), b.nonTerminal("simpleItem"), b.terminalLiteral(","),
        b.nonTerminal("simpleItem"), b.terminalLiteral("]"), b.nonTerminal("multiplicity")
    )
    b.rule("group").concatenation(b.terminalLiteral("("), b.nonTerminal("groupedContent"), b.terminalLiteral(")"))
    b.rule("groupedContent").choiceLongestFromConcatenationItem(b.nonTerminal("concatenation"),b.nonTerminal("choice"))
    b.rule("nonTerminal").concatenation(b.nonTerminal("qualifiedName"))
    b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"))
    b.rule("terminal").choiceLongestFromConcatenationItem(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"))
    b.leaf("LITERAL").concatenation(b.terminalPattern("'([^'\\\\]|\\\\.)*'"))
    b.leaf("PATTERN").concatenation(b.terminalPattern("\"([^\"\\\\]|\\\\.)*\""))
    b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"))
    b.leaf("POSITIVE_INTEGER").concatenation(b.terminalPattern("[0-9]+"))
    return b.grammar.rule
}