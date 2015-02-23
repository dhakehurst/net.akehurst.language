package net.akehurst.language.parser;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parserTree.simple.ParseTreeFactory;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.ogl.semanticModel.TerminalPattern;

import org.junit.Assert;
import org.junit.Test;

public class SkipRule_Test extends AbstractParser_Test {
	
	Grammar as() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatination( new TerminalPattern("\\s+") );

		b.rule("as").multi(1,3,new NonTerminal("a"));
		b.rule("a").concatination(new TerminalLiteral("a"));
		return b.get();
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
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			
			Assert.assertEquals("Tree {*as 0, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], a : ['a' : \"a\"], a : ['a' : \"a\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as_as_aWSaWSa() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = "a a a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			
			Assert.assertEquals("Tree {*as 0, 5}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as_as_WSaWSaWSa() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = " a a a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			
			Assert.assertEquals("Tree {*as 0, 5}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as_as_aWSaWSaWS() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = "a a a ";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			
			Assert.assertEquals("Tree {*as 0, 5}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void as_as_WSaWSaWSaWS() {
		// grammar, goal, input
		try {
			Grammar g = as();
			String goal = "as";
			String text = " a a a ";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			
			Assert.assertEquals("Tree {*as 0, 5}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"], WS : [\"\\s+\" : \" \"], a : ['a' : \"a\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
