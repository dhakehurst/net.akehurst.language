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

package net.akehurst.language.agl.grammar

import net.akehurst.language.agl.ast.NamespaceDefault
import net.akehurst.language.agl.ast.GrammarAbstract
import net.akehurst.language.agl.ast.GrammarBuilderDefault
import net.akehurst.language.api.grammar.Rule

class AglGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.agl.grammar"), "Agl", createRules()) {

}

private fun createRules(): List<Rule> {
	val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.agl.grammar"), "OGL");
	b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
	b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
	b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//.*?$"));

	b.rule("grammarDefinition").concatenation(b.nonTerminal("namespace"), b.nonTerminal("grammar"));
	b.rule("namespace").concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"));
	b.rule("grammar").concatenation(b.terminalLiteral("grammar"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("extends"),
			b.terminalLiteral("{"), b.nonTerminal("rules"), b.terminalLiteral("}"));
	b.rule("extends").multi(0, 1, b.nonTerminal("extends1"));
	b.rule("extends1").concatenation(b.terminalLiteral("extends"), b.nonTerminal("extends2"));
	b.rule("extends2").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("qualifiedName"));
	b.rule("rules").multi(1, -1, b.nonTerminal("anyRule"));
	b.rule("anyRule").choiceEqual(b.concatenation(b.nonTerminal("normalRule")), b.concatenation(b.nonTerminal("skipRule")));
	b.rule("skipRule").concatenation(b.terminalLiteral("skip"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral("="),
			b.nonTerminal("choice"), b.terminalLiteral(";"));
	//TODO: choice has ambiguity, if resolved by priority, then wrong result with "a < b < c", it matches "choiceEqual { a }" as higher priority
	// make rule = choice | concatination, and choices must have multiple items
	b.rule("normalRule").concatenation(b.nonTerminal("IDENTIFIER"), b.terminalLiteral("="), b.nonTerminal("choice"),
			b.terminalLiteral(";"));
	b.rule("choice").choicePriority(b.concatenation(b.nonTerminal("choicePriority")), b.concatenation(b.nonTerminal("choiceLongestMatch")), b.concatenation(b.nonTerminal("choiceAmbiguous")));
	b.rule("choiceLongestMatch").separatedList(0, -1, b.terminalLiteral("|"), b.nonTerminal("concatenation"));
	b.rule("choicePriority").separatedList(0, -1, b.terminalLiteral("<"), b.nonTerminal("concatenation"));
	b.rule("choiceAmbiguous").separatedList(0, -1, b.terminalLiteral("||"), b.nonTerminal("concatenation"));
	b.rule("concatenation").multi(1, -1, b.nonTerminal("concatenationItem"));
	b.rule("concatenationItem").choiceEqual(b.concatenation(b.nonTerminal("simpleItem")), b.concatenation(b.nonTerminal("multi")), b.concatenation(b.nonTerminal("separatedList")));
	b.rule("simpleItem").choiceEqual(b.concatenation(b.nonTerminal("terminal")), b.concatenation(b.nonTerminal("nonTerminal")), b.concatenation(b.nonTerminal("group")));
	b.rule("multi").concatenation(b.nonTerminal("simpleItem"), b.nonTerminal("multiplicity"));
	b.rule("multiplicity").choiceEqual(b.concatenation(b.terminalLiteral("*")), b.concatenation(b.terminalLiteral("+")), b.concatenation(b.terminalLiteral("?")));
	b.rule("group").concatenation(b.terminalLiteral("("), b.nonTerminal("choice"), b.terminalLiteral(")"));
	b.rule("separatedList").concatenation(b.terminalLiteral("["), b.nonTerminal("simpleItem"), b.terminalLiteral("/"),
			b.nonTerminal("LITERAL"), b.terminalLiteral("]"), b.nonTerminal("multiplicity"));
	b.rule("nonTerminal").choiceEqual(b.concatenation(b.nonTerminal("IDENTIFIER")));
	b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("."), b.nonTerminal("IDENTIFIER"));
	b.rule("terminal").choiceEqual(b.concatenation(b.nonTerminal("LITERAL")), b.concatenation(b.nonTerminal("PATTERN")));
	b.rule("LITERAL").concatenation(b.terminalPattern("'(?:\\\\?.)*?'"));
	b.rule("PATTERN").concatenation(b.terminalPattern("\"(?:\\\\?.)*?\""));
	b.rule("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9-]*"));

	return b.grammar.rule
}