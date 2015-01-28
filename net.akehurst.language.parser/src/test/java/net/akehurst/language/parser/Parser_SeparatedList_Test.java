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

import org.junit.Assert;
import org.junit.Test;

public class Parser_SeparatedList_Test {

	Grammar as1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, new TerminalLiteral(","), new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar as2() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatination(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar asSP() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SP").concatination(new TerminalLiteral(" "));
		b.rule("as").separatedList(1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatination(new TerminalLiteral("a"));

		return b.get();
	}
	
	Grammar asb() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("asb").concatination(new NonTerminal("as"), new NonTerminal("b"));
		b.rule("as").separatedList(1, new TerminalLiteral(","), new NonTerminal("b"));
		b.rule("a").concatination(new TerminalLiteral("a"));
		b.rule("b").concatination(new TerminalLiteral("b"));

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
			Assert.assertEquals("Tree {*as 0, 1}",st);
			
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
			Assert.assertEquals("Tree {*as 0, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : ['a' : \"a\", ',' : \",\", 'a' : \"a\"]",nt);
			
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
			Assert.assertEquals("Tree {*as 0, 5}",st);
			
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
			Assert.assertEquals("Tree {*as 0, 1}",st);
			
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
			Assert.assertEquals("Tree {*as 0, 3}",st);
			
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
			Assert.assertEquals("Tree {*as 0, 5}",st);
			
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
			Assert.assertEquals("Tree {*as 0, 7}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"]]",nt);
			
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
			Assert.assertEquals("Tree {*as 0, 10}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("as : [a : ['a' : \"a\"], SP : [' ' : \" \"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], SP : [' ' : \" \"], ',' : \",\", SP : [' ' : \" \"], a : ['a' : \"a\"], SP : [' ' : \" \"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
