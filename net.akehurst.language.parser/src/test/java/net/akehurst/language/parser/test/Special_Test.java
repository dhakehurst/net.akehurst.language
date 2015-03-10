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
import net.akehurst.language.parser.forrest.Factory;
import net.akehurst.language.parser.forrest.ParseTreeBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Special_Test extends AbstractParser_Test {
	
	@Before
	public void before() {
		this.parseTreeFactory = new Factory();
	}
	
	Grammar S() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		//b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		b.rule("S$group1").concatenation(new TerminalLiteral("a"), new NonTerminal("S"), new NonTerminal("B"), new NonTerminal("B"));
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
			Assert.assertEquals("{*S 1, 4}",st);
			
			ParseTreeBuilder b = new ParseTreeBuilder(this.parseTreeFactory, g, goal, text);
			IBranch expected =
				b.branch("S",
					b.branch("S$group1",
						b.leaf("a", "a"),
						b.branch("S",
							b.leaf("a", "a")
						),
						b.branch("B",
							b.emptyLeaf()
						),
						b.branch("B",
							b.leaf("b", "b")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
}
