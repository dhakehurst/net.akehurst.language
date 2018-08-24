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

package net.akehurst.language.ogl.grammar

import net.akehurst.language.ogl.semanticStructure.NamespaceDefault
import net.akehurst.language.ogl.semanticStructure.GrammarAbstract
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault
import net.akehurst.language.api.grammar.Rule

class OGLGrammar : GrammarAbstract(NamespaceDefault("net.akehurst.language.ogl.grammar"), "OGL", createRules()) {

}

private fun createRules(): List<Rule> {
	val b: GrammarBuilderDefault = GrammarBuilderDefault(NamespaceDefault("net.akehurst.language.ogl.grammar"), "OGL");
	b.skip("WHITESPACE").concatenation(b.terminalPattern("\\s+"));
	b.skip("MULTI_LINE_COMMENT").concatenation(b.terminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/"));
	b.skip("SINGLE_LINE_COMMENT").concatenation(b.terminalPattern("//.*?$"));

	b.rule("grammarDefinition").concatenation(b.nonTerminal("namespace"), b.nonTerminal("grammar"));
	b.rule("namespace").concatenation(b.terminalLiteral("namespace"), b.nonTerminal("qualifiedName"), b.terminalLiteral(";"));
	b.rule("grammar").concatenation(b.terminalLiteral("grammar"), b.nonTerminal("IDENTIFIER"), b.nonTerminal("extends"),
			b.terminalLiteral("{"), b.nonTerminal("rules"), b.terminalLiteral("}"));
	b.rule("extends").multi(0, 1, b.nonTerminal("extends1"));
	b.rule("extends1").concatenation(b.terminalLiteral("extends"), b.nonTerminal("extends2"));
	b.rule("extends2").separatedList(1, -1, b.terminalLiteral(","), b.nonTerminal("qualifiedName"));
	b.rule("rules").multi(1, -1, b.nonTerminal("anyRule"));
	b.rule("anyRule").choice(b.nonTerminal("normalRule"), b.nonTerminal("skipRule"));
	b.rule("skipRule").concatenation(b.terminalLiteral("skip"), b.nonTerminal("IDENTIFIER"), b.terminalLiteral(":"),
			b.nonTerminal("choice"), b.terminalLiteral(";"));
	b.rule("normalRule").concatenation(b.nonTerminal("IDENTIFIER"), b.terminalLiteral(":"), b.nonTerminal("choice"),
			b.terminalLiteral(";"));
	b.rule("choice").priorityChoice(b.nonTerminal("simpleChoice"), b.nonTerminal("priorityChoice"));
	b.rule("simpleChoice").separatedList(0, -1, b.terminalLiteral("|"), b.nonTerminal("concatenation"));
	b.rule("priorityChoice").separatedList(0, -1, b.terminalLiteral("<"), b.nonTerminal("concatenation"));
	b.rule("concatenation").multi(1, -1, b.nonTerminal("concatenationItem"));
	b.rule("concatenationItem").choice(b.nonTerminal("simpleItem"), b.nonTerminal("multi"), b.nonTerminal("separatedList"));
	b.rule("simpleItem").choice(b.nonTerminal("terminal"), b.nonTerminal("b.nonTerminal"), b.nonTerminal("group"));
	b.rule("multi").concatenation(b.nonTerminal("simpleItem"), b.nonTerminal("multiplicity"));
	b.rule("multiplicity").choice(b.terminalLiteral("*"), b.terminalLiteral("+"), b.terminalLiteral("?"));
	b.rule("group").concatenation(b.terminalLiteral("("), b.nonTerminal("choice"), b.terminalLiteral(")"));
	b.rule("separatedList").concatenation(b.terminalLiteral("["), b.nonTerminal("simpleItem"), b.terminalLiteral("/"),
			b.nonTerminal("LITERAL"), b.terminalLiteral("]"), b.nonTerminal("multiplicity"));
	b.rule("nonTerminal").choice(b.nonTerminal("IDENTIFIER"));
	b.rule("qualifiedName").separatedList(1, -1, b.terminalLiteral("::"), b.nonTerminal("IDENTIFIER"));
	b.rule("terminal").choice(b.nonTerminal("LITERAL"), b.nonTerminal("PATTERN"));
	b.rule("LITERAL").concatenation(b.terminalPattern("'(?:\\\\?.)*?'"));
	b.rule("PATTERN").concatenation(b.terminalPattern("\"(?:\\\\?.)*?\""));
	b.rule("IDENTIFIER").concatenation(b.terminalPattern("[a-zA-Z_][a-zA-Z_0-9]*"));

	return b.grammar.rule
}