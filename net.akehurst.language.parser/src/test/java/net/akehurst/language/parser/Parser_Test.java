package net.akehurst.language.parser;

import java.util.List;

import net.akehurst.language.core.lexicalAnalyser.ILexicalAnalyser;
import net.akehurst.language.core.lexicalAnalyser.IToken;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.lexicalanalyser.LexicalAnalyser;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;

import org.junit.Assert;
import org.junit.Test;

public class Parser_Test {

	/**
	 * namespace test ;
	 * grammar Test {
	 *   a = 'a' ;
	 * }
	 * 
	 * @return
	 */
	Grammar a() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").concatination(new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar abc() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatination(new TerminalLiteral("abc"));
		return b.get();
	}
	
	Grammar a_b() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab").concatination(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatination(new TerminalLiteral("a"));
		b.rule("b").concatination(new TerminalLiteral("b"));
		return b.get();
	}
	
	Grammar a_b_c() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SPACE").concatination(new TerminalLiteral(" "));
		b.rule("abc").concatination(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatination(new TerminalLiteral("a"));
		b.rule("b").concatination(new TerminalLiteral("b"));
		b.rule("c").concatination(new TerminalLiteral("c"));
		return b.get();
	}
	
	Grammar a_b__c() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatination(new NonTerminal("ab"), new NonTerminal("c"));
		b.rule("ab").concatination(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatination(new TerminalLiteral("a"));
		b.rule("b").concatination(new TerminalLiteral("b"));
		b.rule("c").concatination(new TerminalLiteral("c"));
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
			Assert.assertEquals("a : ['a' : \"a\"]",st);
			
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
			Assert.assertEquals("abc : ['abc' : \"abc\"]",st);
			
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
			Assert.assertEquals("a : ['a' : \"a\"]",st);
			
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
			Assert.assertEquals("b : ['b' : \"b\"]",st);
			
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
			Assert.assertEquals("ab : [a : ['a' : \"a\"], b : ['b' : \"b\"]]",st);
			
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
			Assert.assertEquals("abc : [ab : [a : ['a' : \"a\"], b : ['b' : \"b\"]], c : ['c' : \"c\"]]",st);
			
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
			Assert.assertEquals("abc : [a : ['a' : \"a\"], b : ['b' : \"b\"], c : ['c' : \"c\"]]",st);

			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc2_abc_asbsc() {
		// grammar, goal, input
		try {
			Grammar g = a_b_c();
			String goal = "abc";
			String text = "a b c";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("abc : [a : ['a' : \"a\"], SPACE : [' ' : \" \"], b : ['b' : \"b\"], SPACE : [' ' : \" \"], c : ['c' : \"c\"]]",st);
			
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


}
