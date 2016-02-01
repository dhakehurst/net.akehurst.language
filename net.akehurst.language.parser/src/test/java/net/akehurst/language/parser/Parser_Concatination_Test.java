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
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class Parser_Concatination_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}
	
	Grammar aempty() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").choice();
		return b.get();
	}
	
	Grammar a() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar abc() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatenation(new TerminalLiteral("abc"));
		return b.get();
	}
	
	Grammar a_b() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		return b.get();
	}
	
	Grammar a_b_c() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SPACE").concatination(new TerminalLiteral(" "));
		b.rule("abc").concatenation(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}
	
	Grammar a_b__c() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatenation(new NonTerminal("ab"), new NonTerminal("c"));
		b.rule("ab").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}
	
	Grammar a__b_c() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatenation(new NonTerminal("a"), new NonTerminal("bc"));
		b.rule("bc").concatenation(new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}
	
	@Test
	public void aempty_a_empty() {
		// grammar, goal, input
		try {
			Grammar g = aempty();
			String goal = "a";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*a (2,1,1,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("a",
						b.emptyLeaf("a")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void a_a_a() {
		// grammar, goal, input
		try {
			Grammar g = a();
			String goal = "a";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*a (2,1,2,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("a",
						b.leaf("a", "a")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void a_a_b() {
		// grammar, goal, input
		try {
			Grammar g = a();
			String goal = "a";
			String text = "b";
			
			IParseTree tree = this.process(g, text, goal);

			Assert.fail("This parse should fail");

		} catch (ParseFailedException e) {
			// this should occur
		}
	}

	@Test
	public void abc1_abc_abc() {
		// grammar, goal, input
		try {
			Grammar g = abc();
			String goal = "abc";
			String text = "abc";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc (2,1,4,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
					b.branch("abc",
						b.leaf("abc", "abc")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ab_a_a() {
		// grammar, goal, input
		try {
			Grammar g = a_b();
			String goal = "a";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*a (2,1,2,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
					b.branch("a",
						b.leaf("a", "a")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_a_b() {
		// grammar, goal, input
		try {
			Grammar g = a_b();
			String goal = "a";
			String text = "b";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");
			
		} catch (ParseFailedException e) {
			// this should occur
		}
	}

	@Test
	public void ab_b_b() {
		// grammar, goal, input
		try {
			Grammar g = a_b();
			String goal = "b";
			String text = "b";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*b (2,1,2,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
					b.branch("b",
						b.leaf("b", "b")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ab_ab_ab() {
		// grammar, goal, input
		try {
			Grammar g = a_b();
			String goal = "ab";
			String text = "ab";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*ab (2,1,3,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("ab",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("b",
						b.leaf("b", "b")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ab_ab_aa() {
		// grammar, goal, input
		try {
			Grammar g = a_b();
			String goal = "ab";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");
			
		} catch (ParseFailedException e) {
			//
		}
	}
	
	@Test
	public void ab_ab_b() {
		// grammar, goal, input
		try {
			Grammar g = a_b();
			String goal = "ab";
			String text = "b";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");
			
		} catch (ParseFailedException e) {
			//
		}
	}

	@Test
	public void abc_abc_abc() {
		// grammar, goal, input
		try {
			Grammar g = a_b__c();
			String goal = "abc";
			String text = "abc";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc (2,1,4,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("abc", 
					b.branch("ab",
						b.branch("a",
							b.leaf("a", "a")
						),
						b.branch("b",
							b.leaf("b", "b")
						)
					),
					b.branch("c",
						b.leaf("c", "c")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc2_abc_abc() {
		// grammar, goal, input
		try {
			Grammar g = a_b_c();
			String goal = "abc";
			String text = "abc";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc (2,1,4,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("abc", 
						b.branch("a",
							b.leaf("a", "a")
						),
						b.branch("b",
							b.leaf("b", "b")
						),
					b.branch("c",
						b.leaf("c", "c")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc2_abc_aSPbSPc() {
		// grammar, goal, input
		try {
			Grammar g = a_b_c();
			String goal = "abc";
			String text = "a b c";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc (2,1,6,-1)}",st);

			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("abc", 
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("SPACE",
							b.leaf(" ", " ")
					),
					b.branch("b",
						b.leaf("b", "b")
					),
					b.branch("SPACE",
							b.leaf(" ", " ")
					),
					b.branch("c",
						b.leaf("c", "c")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abc_abc_acb() {
		// grammar, goal, input
		try {
			Grammar g = a_b__c();
			String goal = "abc";
			String text = "acb";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");
			
		} catch (ParseFailedException e) {
			//
		}
	}
	
	@Test
	public void abc_abc_abcd() {
		// grammar, goal, input
		Grammar g = a_b_c();
		String goal = "abc";
		String text = "abcd";
		try {
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");
			
		} catch (ParseFailedException e) {
			IParseTree tree = e.getLongestMatch();
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("abc", 
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("b",
						b.leaf("b", "b")
					),
					b.branch("c",
						b.leaf("c", "c")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
		}
	}

	@Test
	public void abc4_abc_abc() {
		// grammar, goal, input
		try {
			Grammar g = a__b_c();
			String goal = "abc";
			String text = "abc";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc (2,1,4,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("abc", 
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("bc",
						b.branch("b",
							b.leaf("b", "b")
						),
						b.branch("c",
							b.leaf("c", "c")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

}
