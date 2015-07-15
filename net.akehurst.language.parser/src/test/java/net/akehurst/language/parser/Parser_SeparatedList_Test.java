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

public class Parser_SeparatedList_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}
	
	Grammar as1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar as2() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar asSP() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SP").concatination(new TerminalLiteral(" "));
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar asb() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("asb").concatenation(new NonTerminal("as"), new NonTerminal("b"));
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}
	
	Grammar as0n() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(0, -1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

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
			Assert.assertEquals("{*as 1, 1}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
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
	public void as1_as_a() {
		// grammar, goal, input
		try {
			Grammar g = as1();
			String goal = "as";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 2}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.leaf("a", "a")
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as1_as_aa() {
		// grammar, goal, input
		try {
			Grammar g = as1();
			String goal = "as";
			String text = "a,a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 4}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.leaf("a", "a"),
					b.leaf(",", ","),
					b.leaf("a", "a")
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as1_as_aaa() {
		// grammar, goal, input
		try {
			Grammar g = as1();
			String goal = "as";
			String text = "a,a,a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 6}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.leaf("a", "a"),
					b.leaf(",", ","),
					b.leaf("a", "a"),
					b.leaf(",", ","),
					b.leaf("a", "a")
				);
			Assert.assertEquals(expected, tree.getRoot());
			
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as2_as_a() {
		// grammar, goal, input
		try {
			Grammar g = as2();
			String goal = "as";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 2}",st);
			
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
	public void as2_as_aa() {
		// grammar, goal, input
		try {
			Grammar g = as2();
			String goal = "as";
			String text = "a,a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 4}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
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
	public void as2_as_aaa() {
		// grammar, goal, input
		try {
			Grammar g = as2();
			String goal = "as";
			String text = "a,a,a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 6}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
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
	public void asSP_as_aaa() {
		// grammar, goal, input
		try {
			Grammar g = asSP();
			String goal = "as";
			String text = "a, a, a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 8}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
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
	public void asSP_as_aaaa() {
		// grammar, goal, input
		try {
			Grammar g = asSP();
			String goal = "as";
			String text = "a, a, a, a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 11}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
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
	public void asSP_as_aaa2() {
		// grammar, goal, input
		try {
			Grammar g = asSP();
			String goal = "as";
			String text = "a , a , a ";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*as 1, 11}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("as",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.leaf(",", ","),
					b.branch("SP",
						b.leaf(" ", " ")
					),
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("SP",
						b.leaf(" ", " ")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
