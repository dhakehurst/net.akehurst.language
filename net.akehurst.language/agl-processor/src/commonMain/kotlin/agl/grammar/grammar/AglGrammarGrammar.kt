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
    AGL :
        grammar Agl {
            skip WHITESPACE = "\s+" ;
            skip MULTI_LINE_COMMENT = "/\*[^*]*\*+(?:[^*`/`][^*]*\*+)*`/`" ;
            skip SINGLE_LINE_COMMENT = "//.*?$" ;

            grammarDefinition = namespace grammar ;
            namespace = 'namespace' qualifiedName ;
            grammar = 'grammar' IDENTIFIER extends? '{' rules '}' ;
            extends = 'extends' [qualifiedName / ',']+ ;
            rules = anyRule+ ;
            anyRule = normalRule | skipRule ;
            normalRule = IDENTIFIER '=' choice ';' ;
            skipRule = 'skip' IDENTIFIER '=' choice ';' ;
            choice = simpleChoice < priorityChoice ;
            simpleChoice = [concatenation / '|']* ;
            priorityChoice = [concatenation / '<']* ;
            concatenation = concatenationItem+ ;
            concatenationItem = simpleItem | multi | separatedList ;
            simpleItem = terminal | nonTerminal | group ;
            multiplicity = '*' | '+' | '?' ;
            multi = simpleItem multiplicity ;
            group = '(' choice ')' ;
            separatedList = '[' simpleItem '/' terminal ']' multiplicity ;
            nonTerminal = IDENTIFIER ;
            terminal = LITERAL | PATTERN ;
            qualifiedName = [IDENTIFIER / '.']+ ;
            IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9]*";
            LITERAL = "'([^'\\]|\\'|\\\\)*'" ;
            PATTERN = "\"(\\\"|[^\"])*\"" ;
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
    b.rule("grammar").concatenation(b.terminalLiteral("grammar"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("extends"),
            b.terminalLiteral("{"), b.nonTerminal("rules"), b.terminalLiteral("}"))
    b.rule("extends").multi(0, 1, b.nonTerminal("extends1"))
    b.rule("extends1").concatenation(b.terminalLiteral("extends"), b.nonTerminal("extends2"))
    b.rule("extends2").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("qualifiedName"))
    b.rule("rules").multi(1, -1, b.nonTerminal("rule"))
    b.rule("rule").concatenation(b.nonTerminal("ruleTypeLabels"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral("="), b.nonTerminal("choice"), b.terminalLiteral(";"))
    b.rule("ruleTypeLabels").concatenation(b.nonTerminal("isOverride"), b.nonTerminal("isSkip"), b.nonTerminal("isLeaf"))
    b.rule("isOverride").multi(0, 1, b.terminalLiteral("override"))
    b.rule("isSkip").multi(0, 1, b.terminalLiteral("skip"))
    b.rule("isLeaf").multi(0, 1, b.terminalLiteral("leaf"))
    //TODO: choice has ambiguity, if resolved by priority, then wrong result with "a < b < c", it matches "choiceEqual { a }" as higher priority
    // make rule = choice | concatination, and choices must have multiple items
    b.rule("choice").choiceLongestFromConcatenation(b.concatenation(b.nonTerminal("ambiguousChoice")), b.concatenation(b.nonTerminal("priorityChoice")), b.concatenation(b.nonTerminal("simpleChoice")))
    b.rule("simpleChoice").separatedList(0, -1, b.terminalLiteral("|"), b.nonTerminal("concatenation"))
    b.rule("priorityChoice").separatedList(0, -1, b.terminalLiteral("<"), b.nonTerminal("concatenation"))
    b.rule("ambiguousChoice").separatedList(0, -1, b.terminalLiteral("||"), b.nonTerminal("concatenation"))
    //b.rule("choice").choicePriority(b.concatenation(b.nonTerminal("choicePriority")), b.concatenation(b.nonTerminal("choiceLongestMatch")), b.concatenation(b.nonTerminal("choiceAmbiguous")));
    //b.rule("choiceLongestMatch").separatedList(0, -1, b.terminalLiteral("|"), b.nonTerminal("concatenation"));
    //b.rule("choicePriority").separatedList(0, -1, b.terminalLiteral("<"), b.nonTerminal("concatenation"));
    //b.rule("choiceAmbiguous").separatedList(0, -1, b.terminalLiteral("||"), b.nonTerminal("concatenation"));
    b.rule("concatenation").multi(1, -1, b.nonTerminal("concatenationItem"))
    b.rule("concatenationItem").choiceLongestFromConcatenation(b.concatenation(b.nonTerminal("simpleItem")), b.concatenation(b.nonTerminal("multi")), b.concatenation(b.nonTerminal("separatedList")))
    b.rule("simpleItem").choiceLongestFromConcatenation(b.concatenation(b.nonTerminal("terminal")), b.concatenation(b.nonTerminal("nonTerminal")), b.concatenation(b.nonTerminal("group")))
    b.rule("multi").concatenation(b.nonTerminal("simpleItem"), b.nonTerminal("multiplicity"))
    b.rule("multiplicity").choiceLongestFromConcatenation(//
            b.concatenation(b.terminalLiteral("*")), //
            b.concatenation(b.terminalLiteral("+")), //
            b.concatenation(b.terminalLiteral("?")), //
            b.concatenation(b.nonTerminal("POSITIVE_INTEGER"), b.terminalLiteral("+")), //
            b.concatenation(b.nonTerminal("POSITIVE_INTEGER"), b.terminalLiteral(".."), b.nonTerminal("POSITIVE_INTEGER")) //
    );
    b.rule("group").concatenation(b.terminalLiteral("("), b.nonTerminal("choice"), b.terminalLiteral(")"))
    b.rule("separatedList").concatenation(b.terminalLiteral("["), b.nonTerminal("simpleItem"), b.terminalLiteral("/"),
            b.nonTerminal("simpleItem"), b.terminalLiteral("]"), b.nonTerminal("multiplicity"))
    b.rule("nonTerminal").choiceLongestFromConcatenation(b.concatenation(b.nonTerminal("qualifiedName")))
    b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"))
    b.rule("terminal").choiceLongestFromConcatenation(b.concatenation(b.nonTerminal("LITERAL")), b.concatenation(b.nonTerminal("PATTERN")))
    b.leaf("LITERAL").concatenation(b.terminalPattern("'([^'\\\\]|\\\\.)*'"))
    b.leaf("PATTERN").concatenation(b.terminalPattern("\"([^\"\\\\]|\\\\.)*\""))
    b.leaf("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"))
    b.leaf("POSITIVE_INTEGER").concatenation(b.terminalPattern("[0-9]+"))
    return b.grammar.rule
}