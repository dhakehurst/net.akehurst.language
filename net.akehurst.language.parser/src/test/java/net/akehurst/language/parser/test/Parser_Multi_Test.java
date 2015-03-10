package net.akehurst.language.parser.test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.forrest.Factory;
import net.akehurst.language.parser.forrest.ParseTreeBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Parser_Multi_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new Factory();
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
			Assert.assertEquals("{*as 1, 2}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
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
			Assert.assertEquals("{*as 1, 3}",st); //the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
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
			Assert.assertEquals("{*as 1, 4}",st);
						
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
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
			Assert.assertEquals("{*ab01 1, 2}",st);
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
			IBranch expected = 
				b.branch("ab01",
					b.branch("a",
						b.leaf("a", "a")
					),
					b.branch("b01",
						b.emptyLeaf()
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
			Assert.assertEquals("{*ab01 1, 2}",st);
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
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
			Assert.assertEquals("{*ab01 1, 3}",st);
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
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
			Assert.assertEquals("{*ab01 1, 3}",st);
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
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
}
