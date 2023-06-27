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

import net.akehurst.language.agl.grammar.grammar.asm.GrammarAbstract
import net.akehurst.language.agl.grammar.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.grammar.grammar.asm.NamespaceDefault
import net.akehurst.language.api.grammar.GrammarRule

internal object AglGrammarGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl"), "AglGrammar") {
    //companion object {
    const val goalRuleName = "grammarDefinition"
    private fun createRules(): List<GrammarRule> {
        val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl"), "AglGrammar")
        b.skip("WHITESPACE", true).concatenation(b.terminalPattern("\\s+"))
        b.skip("MULTI_LINE_COMMENT", true).concatenation(b.terminalPattern("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"))
        b.skip("SINGLE_LINE_COMMENT", true).concatenation(b.terminalPattern("//[^\\n\\r]*"))

        b.rule("grammarDefinition").concatenation(b.nonTerminal("namespace"), b.nonTerminal("definitions"))
        b.rule("definitions").multi(1, -1, b.nonTerminal("grammar"))
        b.rule("namespace").concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"))
        b.rule("grammar").concatenation(
            b.terminalLiteral("grammar"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("extendsOpt"),
            b.terminalLiteral("{"), b.nonTerminal("rules"), b.terminalLiteral("}")
        )
        b.rule("extendsOpt").multi(0, 1, b.nonTerminal("extends"))
        b.rule("extends").concatenation(b.terminalLiteral("extends"), b.nonTerminal("extendsList"))
        b.rule("extendsList").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("qualifiedName"))
        b.rule("rules").multi(1, -1, b.nonTerminal("rule"))
        b.rule("rule").choiceLongestFromConcatenationItem(b.nonTerminal("grammarRule"), b.nonTerminal("preferenceRule"))
        b.rule("grammarRule").concatenation(b.nonTerminal("ruleTypeLabels"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral("="), b.nonTerminal("rhs"), b.terminalLiteral(";"))
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
        b.rule("concatenationItem").choiceLongestFromConcatenationItem(b.nonTerminal("simpleItemOrGroup"), b.nonTerminal("listOfItems"))
        b.rule("simpleItemOrGroup").choiceLongestFromConcatenationItem(b.nonTerminal("simpleItem"), b.nonTerminal("group"))
        b.rule("simpleItem").choiceLongestFromConcatenationItem(b.nonTerminal("terminal"), b.nonTerminal("nonTerminal"), b.nonTerminal("embedded"))
        b.rule("listOfItems").choiceLongestFromConcatenationItem(b.nonTerminal("simpleList"), b.nonTerminal("separatedList"))  // TODO: Associative lists
        b.rule("multiplicity").choiceLongestFromConcatenationItem(
            b.terminalLiteral("*"),
            b.terminalLiteral("+"),
            b.terminalLiteral("?"),
            b.nonTerminal("range")
        )
        b.rule("range").choiceLongestFromConcatenationItem(
            b.nonTerminal("rangeUnBraced"),
            b.nonTerminal("rangeBraced"),
        )
        b.rule("rangeUnBraced").concatenation(b.nonTerminal("POSITIVE_INTEGER"), b.nonTerminal("rangeMaxOpt"))
        b.rule("rangeBraced").concatenation(b.terminalLiteral("{"), b.nonTerminal("POSITIVE_INTEGER"), b.nonTerminal("rangeMaxOpt"), b.terminalLiteral("}"))
        b.rule("rangeMaxOpt").multi(0, 1, b.nonTerminal("rangeMax"))
        b.rule("rangeMax").choiceLongestFromConcatenationItem(
            b.nonTerminal("rangeMaxUnbounded"),
            b.nonTerminal("rangeMaxBounded"),
        )
        b.rule("rangeMaxUnbounded").concatenation(b.terminalLiteral("+"))
        b.rule("rangeMaxBounded").concatenation(b.terminalLiteral(".."), b.nonTerminal("POSITIVE_INTEGER_GT_ZERO"))
        b.rule("simpleList").concatenation(b.nonTerminal("simpleItemOrGroup"), b.nonTerminal("multiplicity"))
        b.rule("separatedList").concatenation(
            b.terminalLiteral("["), b.nonTerminal("simpleItemOrGroup"), b.terminalLiteral("/"),
            b.nonTerminal("simpleItemOrGroup"), b.terminalLiteral("]"), b.nonTerminal("multiplicity")
        )
        b.rule("group").concatenation(b.terminalLiteral("("), b.nonTerminal("groupedContent"), b.terminalLiteral(")"))
        b.rule("groupedContent").choiceLongestFromConcatenationItem(b.nonTerminal("concatenation"), b.nonTerminal("choice"))
        b.rule("nonTerminal").concatenation(b.nonTerminal("IDENTIFIER"))
        b.rule("embedded").concatenation(b.nonTerminal("qualifiedName"), b.terminalLiteral("::"), b.nonTerminal("nonTerminal"))
        b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"))
        b.rule("terminal").choiceLongestFromConcatenationItem(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"))
        b.leaf("LITERAL").concatenation(b.terminalPattern("'([^'\\\\]|\\\\.)*'"))
        b.leaf("PATTERN").concatenation(b.terminalPattern("\"([^\"\\\\]|\\\\.)*\""))
        b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"))
        b.leaf("POSITIVE_INTEGER").concatenation(b.terminalPattern("[0-9]+"))
        b.leaf("POSITIVE_INTEGER_GT_ZERO").concatenation(b.terminalPattern("[1-9][0-9]*"))

        b.rule("preferenceRule").concatenation(
            b.terminalLiteral("preference"), b.nonTerminal("simpleItem"), b.terminalLiteral("{"),
            b.nonTerminal("preferenceOptionList"),
            b.terminalLiteral("}")
        )
        b.rule("preferenceOptionList").multi(1, -1, b.nonTerminal("preferenceOption"))
        b.rule("preferenceOption")
            .concatenation(b.nonTerminal("nonTerminal"), b.nonTerminal("choiceNumber"), b.terminalLiteral("on"), b.nonTerminal("terminalList"), b.nonTerminal("associativity"))
        b.rule("choiceNumber").multi(0, 1, b.nonTerminal("POSITIVE_INTEGER"))
        b.rule("terminalList").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("simpleItem"))
        b.rule("associativity").choiceLongestFromConcatenationItem(b.terminalLiteral("left"), b.terminalLiteral("right"))
        return b.grammar.grammarRule
    }
    //}

    const val styleStr: String = """'namespace' {
  foreground: darkgreen;
  font-style: bold;
}
'grammar' {
  foreground: darkgreen;
  font-style: bold;
}
'extends' {
  foreground: darkgreen;
  font-style: bold;
}
'override' {
  foreground: darkgreen;
  font-style: bold;
}
'skip' {
  foreground: darkgreen;
  font-style: bold;
}
'leaf' {
  foreground: darkgreen;
  font-style: bold;
}
LITERAL {
  foreground: blue;
}
PATTERN {
  foreground: darkblue;
}
IDENTIFIER {
  foreground: darkred;
  font-style: italic;
}"""

    init {
        super.grammarRule.addAll(createRules())
    }

    //TODO: gen this from the ASM
    override fun toString(): String = """
namespace net.akehurst.language.agl
grammar AglGrammar {
    skip WHITESPACE = "\s+" ;
    skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
    skip SINGLE_LINE_COMMENT = "//[\n\r]*?" ;

    grammarDefinition = namespace definitions ;
    namespace = 'namespace' qualifiedName ;
    definitions = grammar+ ;
    grammar = 'grammar' IDENTIFIER extendsOpt '{' rules '}' ;
    extendsOpt = extends?
    extends = 'extends' extendsList ;
    extendsList = [qualifiedName / ',']+ ;
    rules = rule+ ;
    rule = grammarRule | preferenceRule ;
    grammarRule = ruleTypeLabels IDENTIFIER '=' rhs ';' ;
    rhs = empty | concatenation | choice ;
    ruleTypeLabels = 'override'? 'skip'? 'leaf'? ;
    empty = ;
    choice = ambiguousChoice | priorityChoice | simpleChoice ;
    ambiguousChoice = [ concatenation / '||' ]2+ ;
    priorityChoice = [ concatenation / '<' ]2+ ;
    simpleChoice = [ concatenation / '|' ]2+ ;
    concatenation = concatenationItem+ ;
    concatenationItem = simpleItem | listOfItems ;
    simpleItemOrGroup = simpleItem | group ;
    simpleItem = terminal | nonTerminal | embedded ;
    listOfItems = simpleList | separatedList ;
    multiplicity = '*' | '+' | '?' | oneOrMore | range ;
    oneOrMore = POSITIVE_INTEGER '+' ;
    range = POSITIVE_INTEGER '..' POSITIVE_INTEGER ;
    simpleList = simpleItemOrGroup multiplicity ;
    separatedList = '[' simpleItemOrGroup '/' terminal ']' multiplicity ;
    group = '(' groupedContent ')' ;
    groupedContent = concatenation | choice ;
    nonTerminal = qualifiedName ;
    embedded = qualifiedName '::' nonTerminal ;
    terminal = LITERAL | PATTERN ;
    qualifiedName = [IDENTIFIER / '.']+ ;
    IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
    LITERAL = "'([^'\\]|\\'|\\\\)*'" ;
    PATTERN = "\"(\\\"|[^\"])*\"" ;
    POSITIVE_INTEGER = "[0-9]+" ;
    
    preferenceRule = 'preference' simpleItem '{' preferenceOptionList '}' ;
    preferenceOptionList = preferenceOption* ;
    preferenceOption = nonTerminal choiceNumber 'on' terminalList associativity ;
    choiceNumber = POSITIVE_INTEGER? ;
    terminalList = [simpleItem / ',']+ ;
}
    """.trimIndent()
}

