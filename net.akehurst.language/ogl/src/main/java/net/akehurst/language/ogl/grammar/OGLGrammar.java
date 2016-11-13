/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.ogl.grammar;

import java.util.List;

import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

/**
 * <code>
 * namespace net::akehurst::language::ogl::grammar;
 * 
 * grammar OGL {
 * 
 *   grammarDefinition : namespace grammar ;
 *   namespace : 'namespace' qualifiedName ';' ;
 *   grammar : 'grammar' IDENTIFIER '{' rules '}' ;
 *   rules : anyRule+ ;
 *   anyRule : normalRule | skipRule ; 
 *   normalRule : IDENTIFIER ':' choice ';' ;
 *   skipRule : 'skip' IDENTIFIER ':' choice ';' ;
 *   choice : simpleChoice < priorityChoice ;
 *   simpleChoice : (concatenation / '|')* ;
 *   priorityChoice : (concatenation / '<')* ;
 *   concatenation : concatenationItem+ ;
 *   concatenationItem : simpleItem | multi | separatedList ;
 *   simpleItem : terminal | nonTerminal | group ;
 *   multiplicity : '*' | '+' | '?'
 *   multi : simpleItem multiplicity ;
 *   group : '(' choice ')' ;
 *   separatedList : '(' simpleItem '/' terminal ')' multiplicity ;
 *   nonTerminal : IDENTIFIER ;
 *   terminal : LITERAL | PATTERN ;
 *   LITERAL : "'(?:\\\\?.)*?'" ;
 *   PATTERN : "\"(?:\\\\?.)*?\"" ;
 *   qualifiedName : (IDENTIFIER / '::')+ ;
 *   IDENTIFIER : "[a-zA-Z_][a-zA-Z_0-9]*";
 *   
 * }
 * </code>
 * @author akehurst
 *
 */
public class OGLGrammar extends Grammar {
	
	static List<Rule> createRules() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("MULTI_LINE_COMMENT").concatination( new TerminalPattern("/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/") );
		b.skip("SINGLE_LINE_COMMENT").concatination( new TerminalPattern("//.*?$") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new NonTerminal("rules"), new TerminalLiteral("}") );
		b.rule("rules").multi(1,-1,new NonTerminal("anyRule") );
		b.rule("anyRule").choice(new NonTerminal("normalRule"), new NonTerminal("skipRule") );
		b.rule("skipRule").concatenation( new TerminalLiteral("skip"), new NonTerminal("IDENTIFIER"), new TerminalLiteral(":"), new NonTerminal("choice"), new TerminalLiteral(";") );
		b.rule("normalRule").concatenation( new NonTerminal("IDENTIFIER"), new TerminalLiteral(":"), new NonTerminal("choice"), new TerminalLiteral(";") );
		b.rule("choice").priorityChoice(new NonTerminal("simpleChoice"), new NonTerminal("priorityChoice") );
		b.rule("simpleChoice").separatedList(0, -1, new TerminalLiteral("|"), new NonTerminal("concatenation") );
		b.rule("priorityChoice").separatedList(0, -1, new TerminalLiteral("<"), new NonTerminal("concatenation") );
		b.rule("concatenation").multi(1,-1,new NonTerminal("concatenationItem") );
		b.rule("concatenationItem").choice(
				new NonTerminal("simpleItem"),
				new NonTerminal("multi"),
				new NonTerminal("separatedList")
		);
		b.rule("simpleItem").choice(
				new NonTerminal("terminal"),
				new NonTerminal("nonTerminal"),
				new NonTerminal("group")
		);
		b.rule("multi").concatenation( new NonTerminal("simpleItem"), new NonTerminal("multiplicity") );
		b.rule("multiplicity").choice(new TerminalLiteral("*"), new TerminalLiteral("+"), new TerminalLiteral("?"));
		b.rule("group").concatenation( new TerminalLiteral("("), new NonTerminal("choice"), new TerminalLiteral(")") );
		b.rule("separatedList").concatenation( new TerminalLiteral("("), new NonTerminal("simpleItem"), new TerminalLiteral("/"), new NonTerminal("LITERAL"), new TerminalLiteral(")"), new NonTerminal("multiplicity") );
		b.rule("nonTerminal").choice(new NonTerminal("IDENTIFIER"));
		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );
		b.rule("terminal").choice(
				new NonTerminal("LITERAL"),
				new NonTerminal("PATTERN")
		);
		b.rule("LITERAL").concatenation( new TerminalPattern("'(?:\\\\?.)*?'") );
		b.rule("PATTERN").concatenation( new TerminalPattern("\"(?:\\\\?.)*?\"") );
		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get().getRule();
	}
	
	public OGLGrammar() {
		super( new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		this.setRule(createRules());
	}
	
}
