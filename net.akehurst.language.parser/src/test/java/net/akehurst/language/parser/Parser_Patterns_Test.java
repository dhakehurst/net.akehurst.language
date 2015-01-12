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

public class Parser_Patterns_Test {

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
	
	IParseTree process(Grammar grammar, String text, String goalName) throws ParseFailedException {
		try {
			INodeType goal = grammar.findRule(goalName).getNodeType();
			IParser parser = new ScannerLessParser(grammar);
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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"a\"]]",st);

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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aa\"]]",st);

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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aaa\"]]",st);

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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"a\"], ':' : \":\", a : [\"[a]+\" : \"a\"]]",st);

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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aa\"], ':' : \":\", a : [\"[a]+\" : \"a\"]]",st);

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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"a\"], ':' : \":\", a : [\"[a]+\" : \"aa\"]]",st);

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
			Assert.assertEquals("as : [a : [\"[a]+\" : \"aaa\"], ':' : \":\", a : [\"[a]+\" : \"aaa\"]]",st);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
