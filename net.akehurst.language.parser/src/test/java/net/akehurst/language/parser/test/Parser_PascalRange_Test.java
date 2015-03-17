package net.akehurst.language.parser.test;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.ogl.semanticModel.TerminalPattern;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.runtime.Factory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Parser_PascalRange_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new Factory();
	}
	
	Grammar pascal() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("expr").choice(new NonTerminal("range"), new NonTerminal("real"));
		b.rule("range").concatenation(new NonTerminal("integer"), new TerminalLiteral(".."), new NonTerminal("integer"));
		b.rule("integer").concatenation(new TerminalPattern("[0-9]+"));
		b.rule("real").concatenation(new TerminalPattern("([0-9]+[.][0-9]*)|([.][0-9]+)"));

		return b.get();
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
			Assert.assertEquals("{*expr 1, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("expr : [real : [\"([0-9]+[.][0-9]*)|([.][0-9]+)\" : \".5\"]]",nt);
			
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
			Assert.assertEquals("{*expr 1, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("expr : [real : [\"([0-9]+[.][0-9]*)|([.][0-9]+)\" : \"1.\"]]",nt);
			
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
			Assert.assertEquals("{*expr 1, 5}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("expr : [range : [integer : [\"[0-9]+\" : \"1\"], '..' : \"..\", integer : [\"[0-9]+\" : \"5\"]]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
