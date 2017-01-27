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
package net.akehurst.language.parser;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class Parser_Expressions_Test extends AbstractParser_Test {
	
	Grammar expression_plus_multiply() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("e"));
		b.rule("e").priorityChoice(new NonTerminal("variable"), new NonTerminal("multiply"), new NonTerminal("plus"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	
	@Test
	public void emp_S_a() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
						
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("variable",
							b.leaf("a")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void emp_S_apa() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a+a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("plus",
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							),
							b.leaf("+"),
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void emp_S_ama() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a*a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
						
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("multiply",
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							),
							b.leaf("*"),
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}

	@Test
	public void emp_S_apama() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a+a*a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
						
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("plus",
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							),
							b.leaf("+"),
							b.branch("e",
								b.branch("multiply",
									b.branch("e",
										b.branch("variable",
											b.leaf("a")
										)
									),
									b.leaf("*"),
									b.branch("e",
										b.branch("variable",
											b.leaf("a")
										)
									)
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void emp_S_amapa() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a*a+a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("plus",
							b.branch("e",
								b.branch("multiply",
									b.branch("e",
										b.branch("variable",
											b.leaf("a")
										)
									),
									b.leaf("*"),
									b.branch("e",
										b.branch("variable",
											b.leaf("a")
										)
									)
								)
							),
							b.leaf("+"),
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}

	Grammar expression_if_then_else() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("e"));
		b.skip("WS").concatination( new TerminalPattern("\\s+") );
		b.rule("e").priorityChoice(new NonTerminal("variable"), new NonTerminal("multiply"), new NonTerminal("plus"), new NonTerminal("ifthenelse"), new NonTerminal("ifthen"));
		b.rule("ifthen").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("e"));
		b.rule("ifthenelse").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("e"), new TerminalLiteral("else"), new NonTerminal("e"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	@Test
	public void eite_S_a() {
		// grammar, goal, input
		try {
			Grammar g = expression_if_then_else();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("variable",
							b.leaf("a")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}

	@Test
	public void eite_S_ifathenaelsea() {
		// grammar, goal, input
		try {
			Grammar g = expression_if_then_else();
			String goal = "S";
			String text = "if a then a else a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("ifthenelse",
							b.leaf("if"),
							b.branch("WS", b.leaf("\\s+", " ") ),
							b.branch("e",
								b.branch("variable",
									b.leaf("a"),
									b.branch("WS", b.leaf("\\s+", " ") )
								)
							),
							b.leaf("then"),
							b.branch("WS", b.leaf("\\s+", " ") ),
							b.branch("e",
								b.branch("variable",
									b.leaf("a"),
									b.branch("WS", b.leaf("\\s+", " ") )
								)
							),			
							b.leaf("else"),
							b.branch("WS", b.leaf("\\s+", " ") ),
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	@Test
	public void eite_S_ifathena() {
		// grammar, goal, input
		try {
			Grammar g = expression_if_then_else();
			String goal = "S";
			String text = "if a then a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("ifthen",
							b.leaf("if"),
							b.branch("WS", b.leaf("\\s+", " ") ),
							b.branch("e",
								b.branch("variable",
									b.leaf("a"),
									b.branch("WS", b.leaf("\\s+", " ") )
								)
							),
							b.leaf("then"),
							b.branch("WS", b.leaf("\\s+", " ") ),
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}

	Grammar statement_if_then_else() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("statements"));
		b.skip("WS").concatination( new TerminalPattern("\\s+") );
		b.rule("statements").multi(1, -1, new NonTerminal("statement"));
		b.rule("statement").choice(new TerminalLiteral("return"), new NonTerminal("ifthenelse"), new NonTerminal("ifthen"));
		b.rule("e").priorityChoice(new NonTerminal("variable"), new NonTerminal("multiply"), new NonTerminal("plus"));
		b.rule("ifthen").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("statements"));
		b.rule("ifthenelse").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("statements"), new TerminalLiteral("else"), new NonTerminal("statements"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
}
