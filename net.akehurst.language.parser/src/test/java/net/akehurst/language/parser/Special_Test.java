package net.akehurst.language.parser;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parserTree.simple.ParseTreeFactory;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;

import org.junit.Assert;
import org.junit.Test;

public class Special_Test {

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
	
	Grammar S() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		//b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		b.rule("S$group1").concatination(new TerminalLiteral("a"), new NonTerminal("S"), new NonTerminal("B"), new NonTerminal("B"));
		b.rule("S").choice(new NonTerminal("S$group1"), new TerminalLiteral("a"));
		//b.rule("B").choice(new NonTerminal("B1"), new NonTerminal("B2"));
		b.rule("B").multi(0,1,new TerminalLiteral("b"));
		return b.get();
	}
	
	@Test
	public void S_S_aab() {
		// grammar, goal, input
		try {
			Grammar g = S();
			String goal = "S";
			String text = "aab";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*S 0, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			
			ParseTreeFactory ptb = new ParseTreeFactory();
//			IParseTree expected =
//				ptb.createBranch("S",
//					ptb.createBranch("S$group1",
//						ptb.createLeaf("a", "a"),
//						ptb.createBranch("S",
//							ptb.createLeaf("a", "a")
//						),
//						ptb.createBranch("B",
//							ptb.createEmptyLeaf()
//						),
//						ptb.createBranch("B",
//							ptb.createLeaf("b", "b")
//						)
//					)
//				);
			Assert.assertEquals("S : [S$group1 : ['a' : \"a\", S : ['a' : \"a\"], B : [ : \"\"], B : ['b' : \"b\"]]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
}
