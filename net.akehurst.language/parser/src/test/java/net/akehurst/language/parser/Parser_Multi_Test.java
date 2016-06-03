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

public class Parser_Multi_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}
	
	Grammar ab01() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab01").concatenation(new NonTerminal("a"), new NonTerminal("b01"));
		b.rule("b01").multi(0, 1, new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}
	
	Grammar ab01_2() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab01$group1").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("ab01").choice(new NonTerminal("ab01$group1"), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}
	
	Grammar as13() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(1, 3, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar as0n() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(0, -1, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar as0nbs0n() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("asbs").concatenation(new NonTerminal("as"),new NonTerminal("bs"));
		b.rule("as").multi(0, -1, new NonTerminal("a"));
		b.rule("bs").multi(0, -1, new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}
	
	Grammar abs1m1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abs").multi(1, -1, new NonTerminal("ab"));
		b.rule("ab").choice(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}
	
	@Test
	public void as0n_as_empty() {
		// grammar, goal, input
		try {
			Grammar g = as0n();
			String goal = "as";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as (2,1,1,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.emptyLeaf("as")
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void as0nbs0n_asbs_empty() {
		// grammar, goal, input
		try {
			Grammar g = as0nbs0n();
			String goal = "asbs";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*asbs (2,1,1,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("asbs",
					b.branch("as",
						b.emptyLeaf("as")
					),
					b.branch("bs",
						b.emptyLeaf("bs")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void as0nbs0n_asbs_b() {
		// grammar, goal, input
		try {
			Grammar g = as0nbs0n();
			String goal = "asbs";
			String text = "b";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*asbs (2,1,2,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("asbs",
					b.branch("as",
						b.emptyLeaf("as")
					),
					b.branch("bs",
						b.branch("b",
							b.leaf("b")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void as0nbs0n_asbs_bb() {
		// grammar, goal, input
		try {
			Grammar g = as0nbs0n();
			String goal = "asbs";
			String text = "bb";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*asbs (2,1,3,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected =
				b.branch("asbs",
					b.branch("as",
						b.emptyLeaf("as")
					),
					b.branch("bs",
						b.branch("b",
							b.leaf("b")
						),
						b.branch("b",
								b.leaf("b")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void as13_as_a() {
		// grammar, goal, input
		try {
			Grammar g = as13();
			String goal = "as";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as (2,1,2,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as13_as_aa() {
		// grammar, goal, input
		try {
			Grammar g = as13();
			String goal = "as";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as (2,1,3,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("a",
						b.leaf("a", "a")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as13_as_aaa() {
		// grammar, goal, input
		try {
			Grammar g = as13();
			String goal = "as";
			String text = "aaa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as (2,1,4,-1)}",st);
						
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("a",
						b.leaf("a", "a")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
						
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab01_ab01_a() {
		// grammar, goal, input
		try {
			Grammar g = ab01();
			String goal = "ab01";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*ab01 (2,1,2,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("ab01",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("b01",
						b.emptyLeaf("b01")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab01_2_ab01_a() {
		// grammar, goal, input
		try {
			Grammar g = ab01_2();
			String goal = "ab01";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*ab01 (2,1,2,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("ab01",
					b.branch("a",
						b.leaf("a", "a")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ab01_2_ab01_ab() {
		// grammar, goal, input
		try {
			Grammar g = ab01_2();
			String goal = "ab01";
			String text = "ab";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*ab01 (2,1,3,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("ab01",
					b.branch("ab01$group1",
						b.branch("a",
							b.leaf("a", "a")
						),
						b.branch("b",
							b.leaf("b", "b")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ab01_ab01_ab() {
		// grammar, goal, input
		try {
			Grammar g = ab01();
			String goal = "ab01";
			String text = "ab";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*ab01 (2,1,3,-1)}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("ab01",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("b01",
						b.branch("b",
							b.leaf("b", "b")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ab01_ab01_aa() {
		// grammar, goal, input
		try {
			Grammar g = ab01();
			String goal = "ab01";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			
			Assert.fail("This parse should fail");
			
		} catch (ParseFailedException e) {
			// this should occur
		}
	}

	@Test
	public void abs1m1_abs_ababababababab() {
		// grammar, goal, input
		try {
			Grammar g = abs1m1();
			String goal = "abs";
			String text = "ababababababababababababababababababababababababababababababababababababababababababababababababab";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abs (2,1,99,-1)}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
//			ParseTreeBuilder b = this.builder(g, text, goal);;
//			IBranch expected = 
//				b.branch("as",
//					b.branch("a",
//						b.leaf("a", "a")
//					)
//				);
//			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

}
