package net.akehurst.language.parser.test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.parser.runtime.Factory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Parser_SeparatedList_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new Factory();
	}
	
	Grammar as1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, new TerminalLiteral(","), new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar as2() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar asSP() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SP").concatination(new TerminalLiteral(" "));
		b.rule("as").separatedList(1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar asb() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("asb").concatenation(new NonTerminal("as"), new NonTerminal("b"));
		b.rule("as").separatedList(1, new TerminalLiteral(","), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : ['a' : \"a\"]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : ['a' : \"a\", ',' : \",\", 'a' : \"a\", ',' : \",\", 'a' : \"a\"]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"]]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], ',' : \",\", a : ['a' : \"a\"]]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], ',' : \",\", a : ['a' : \"a\"], ',' : \",\", a : ['a' : \"a\"]]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"]]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"]]",nt);
			
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
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], SP : [' ' : \" \"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], SP : [' ' : \" \"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], SP : [' ' : \" \"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
