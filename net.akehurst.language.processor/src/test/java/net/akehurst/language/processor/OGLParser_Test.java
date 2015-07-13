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
package net.akehurst.language.processor;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.ScannerLessParser;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ForrestFactory;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OGLParser_Test {
	RuntimeRuleSetBuilder parseTreeFactory;
	
	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}
	
	ParseTreeBuilder builder(Grammar grammar, String text, String goal) {
		ForrestFactory ff = new ForrestFactory(this.parseTreeFactory, text);
		return new ParseTreeBuilder(ff, grammar, goal, text);
	}
	
	IParseTree process(Grammar grammar, String text, String goalName) throws ParseFailedException {
		try {
			INodeType goal = grammar.findRule(goalName).getNodeType();
			IParser parser = new ScannerLessParser(this.parseTreeFactory, grammar);
			IParseTree tree = parser.parse(goal, text);
			return tree;
		} catch (RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	Grammar ns1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("IDENTIFIER"), new TerminalLiteral(";") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void ns1_namespace_test() {
		try {
			Grammar g = ns1();
			String goal = "grammarDefinition";
			String text = "namespace test;" + System.lineSeparator();

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*grammarDefinition 1, 17}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected =
				b.branch("grammarDefinition",
					b.branch("namespace",
						b.leaf("namespace", "namespace"),
						b.branch("WHITESPACE",
							b.leaf("\\s+", " ")
						),
						b.branch("IDENTIFIER",
							b.leaf("[a-zA-Z_][a-zA-Z_0-9]*", "test")
						),
						b.leaf(";",";")
					),
					b.branch("WHITESPACE",
						b.leaf("\\s+", " ")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	
	Grammar g1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("IDENTIFIER"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new TerminalLiteral("}") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void g1_noRules() {
		try {
			Grammar g = g1();
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "}";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void g1_emptyRule() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();

			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += " a :  ;" + System.lineSeparator();
			text += "}";
			
			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	Grammar qualifiedName() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		
		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void qualifiedName_a() {
		try {
			Grammar g = qualifiedName();
			String goal = "qualifiedName";
			String text = "a";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void qualifiedName__a_b() {
		try {
			Grammar g = qualifiedName();
			String goal = "qualifiedName";
			String text = "a::b";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void qualifiedName__a_b_c_d() {
		try {
			Grammar g = qualifiedName();
			String goal = "qualifiedName";
			String text = "a::b::c::d";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	Grammar noRules() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new TerminalLiteral("}") );

		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void noRules_1() {
		try {
			Grammar g = noRules();
			String goal = "grammarDefinition";
			String text = "namespace test::ns::nss;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "}";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	Grammar nonTerminalOnly() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new NonTerminal("rules"), new TerminalLiteral("}") );
		b.rule("rules").multi(1,-1,new NonTerminal("rule") );
		b.rule("rule").concatenation( new NonTerminal("IDENTIFIER"), new TerminalLiteral("="), new NonTerminal("nonTerminal"), new TerminalLiteral(";"));
		b.rule("nonTerminal").choice(new NonTerminal("IDENTIFIER"));

		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}

	@Test
	public void nonTerminalOnly_1() {
		try {
			Grammar g = nonTerminalOnly();
			String goal = "grammarDefinition";
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "a = b ;";
			text += "}";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void a() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();
			
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += " skip SP : ' ' ;" + System.lineSeparator();
			text += " a : 'a' ;" + System.lineSeparator();
			text += "}";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void a2() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();
			
			String text = "namespace test; grammar A { skip SP : ' ' ; a : 'a' ; }";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void separatedList_1n() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();
			
			String text = "namespace test; grammar A { sepList : ('a' / ',')+; }";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void separatedList_0n() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();
			
			String text = "namespace test; grammar A { sepList : ('a' / ',')*; }";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
