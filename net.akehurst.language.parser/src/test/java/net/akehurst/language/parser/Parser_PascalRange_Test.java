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

public class Parser_PascalRange_Test {

	Grammar pascal() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("expr").choice(new NonTerminal("range"), new NonTerminal("real"));
		b.rule("range").concatination(new NonTerminal("integer"), new TerminalLiteral(".."), new NonTerminal("integer"));
		b.rule("integer").concatination(new TerminalPattern("[0-9]+"));
		b.rule("real").concatination(new TerminalPattern("([0-9]+[.][0-9]*)|([.][0-9]+)"));

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
	public void pascal_expr_p5() {
		// grammar, goal, input
		try {
			Grammar g = pascal();
			String goal = "expr";
			String text = ".5";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("expr : [real : [\"([0-9]+[.][0-9]*)|([.][0-9]+)\" : \".5\"]]",st);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void pascal_expr_1p() {
		// grammar, goal, input
		try {
			Grammar g = pascal();
			String goal = "expr";
			String text = "1.";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("expr : [real : [\"([0-9]+[.][0-9]*)|([.][0-9]+)\" : \"1.\"]]",st);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void pascal_expr_1to5() {
		// grammar, goal, input
		try {
			Grammar g = pascal();
			String goal = "expr";
			String text = "1..5";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("expr : [range : [integer : [\"[0-9]+\" : \"1\"], '..' : \"..\", integer : [\"[0-9]+\" : \"5\"]]]",st);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
