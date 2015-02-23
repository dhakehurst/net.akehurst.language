package net.akehurst.language.parser;

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
import net.akehurst.language.ogl.semanticModel.TerminalPattern;

import org.junit.Assert;
import org.junit.Test;

public class Parser_Patterns_Test extends AbstractParser_Test {

	Grammar as() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").concatination(new NonTerminal("a"));
		b.rule("a").concatination(new TerminalPattern("[a]+"));
		return b.get();
	}
	
	Grammar asxas() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").concatination(new NonTerminal("a"), new TerminalLiteral(":"), new NonTerminal("a"));
		b.rule("a").concatination(new TerminalPattern("[a]+"));
		return b.get();
	}
	
	@Test
	public void as_as_a() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 1}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"a\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aa() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 2}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aa\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as_as_aaa() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = "aaa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aaa\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asxas_as_a() {
		// grammar, goal, input
		try {
			Grammar g = asxas();
			String goal = "as";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (ParseFailedException e) {
			//
		}
	}

	@Test
	public void asxas_as_aa() {
		// grammar, goal, input
		try {
			Grammar g = asxas();
			String goal = "as";
			String text = "aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (ParseFailedException e) {
			//
		}
	}
	
	@Test
	public void asxas_as_axa() {
		// grammar, goal, input
		try {
			Grammar g = asxas();
			String goal = "as";
			String text = "a:a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"a\"], ':' : \":\", a : [\"[a]+\" : \"a\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void asxas_as_aaxa() {
		// grammar, goal, input
		try {
			Grammar g = asxas();
			String goal = "as";
			String text = "aa:a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 4}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aa\"], ':' : \":\", a : [\"[a]+\" : \"a\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void asxas_as_axaa() {
		// grammar, goal, input
		try {
			Grammar g = asxas();
			String goal = "as";
			String text = "a:aa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 4}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"a\"], ':' : \":\", a : [\"[a]+\" : \"aa\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void asxas_as_aaaxaaa() {
		// grammar, goal, input
		try {
			Grammar g = asxas();
			String goal = "as";
			String text = "aaa:aaa";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*as 0, 7}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aaa\"], ':' : \":\", a : [\"[a]+\" : \"aaa\"]]",nt);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
