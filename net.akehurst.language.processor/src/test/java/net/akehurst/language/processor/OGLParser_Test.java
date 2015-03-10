package net.akehurst.language.processor;

import net.akehurst.language.core.parser.IBranch;
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
import net.akehurst.language.parser.ScannerLessParser;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.forrest.ParseTreeBuilder;

import org.junit.Assert;
import org.junit.Test;

public class OGLParser_Test {
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

	Grammar ns1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("IDENTIFIER"), new TerminalLiteral(";") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void ns1_namespace_test() {
		try {
			Grammar g = ns1();
			String goal = "grammarDefinition";
			String text = "namespace test;" + System.lineSeparator();

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("Tree {*grammarDefinition 1, 17}",st);
			
			ParseTreeBuilder b = new ParseTreeBuilder(g, goal, text);
			IBranch expected =
				b.branch("grammarDefinition",
					b.branch("namespace",
						b.leaf("namespace", "namespace"),
						b.branch("WHITESPACE",
							b.leaf("\\s+", " ")
						),
						b.branch("IDENTIFIER",
							b.leaf("[a-zA-Z_][a-zA-Z_0-9]*", "test")
						),
						b.leaf(";",";")
					),
					b.branch("WHITESPACE",
						b.leaf("\\s+", " ")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	
	Grammar g1() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("IDENTIFIER"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new TerminalLiteral("}") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void g1_noRules() {
		try {
			Grammar g = g1();
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "}";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	Grammar qualifiedName() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		
		b.rule("qualifiedName").separatedList(1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void qualifiedName_a() {
		try {
			Grammar g = qualifiedName();
			String goal = "qualifiedName";
			String text = "a";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void qualifiedName__a_b() {
		try {
			Grammar g = qualifiedName();
			String goal = "qualifiedName";
			String text = "a::b";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void qualifiedName__a_b_c_d() {
		try {
			Grammar g = qualifiedName();
			String goal = "qualifiedName";
			String text = "a::b::c::d";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	Grammar noRules() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new TerminalLiteral("}") );

		b.rule("qualifiedName").separatedList(1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}
	
	@Test
	public void noRules_1() {
		try {
			Grammar g = noRules();
			String goal = "grammarDefinition";
			String text = "namespace test::ns::nss;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "}";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	Grammar nonTerminalOnly() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatination( new TerminalPattern("\\s+") );
		b.skip("COMMENT").concatination( new TerminalPattern("(?s)/\\*.*?\\*/") );
		
		b.rule("grammarDefinition").concatenation( new NonTerminal("namespace"), new NonTerminal("grammar") );
		b.rule("namespace").concatenation( new TerminalLiteral("namespace"), new NonTerminal("qualifiedName"), new TerminalLiteral(";") );
		b.rule("grammar").concatenation( new TerminalLiteral("grammar"), new NonTerminal("IDENTIFIER"), new TerminalLiteral("{"), new NonTerminal("rules"), new TerminalLiteral("}") );
		b.rule("rules").multi(1,-1,new NonTerminal("rule") );
		b.rule("rule").concatenation( new NonTerminal("IDENTIFIER"), new TerminalLiteral("="), new NonTerminal("nonTerminal"), new TerminalLiteral(";"));
		b.rule("nonTerminal").choice(new NonTerminal("IDENTIFIER"));

		b.rule("qualifiedName").separatedList(1, new TerminalLiteral("::"), new NonTerminal("IDENTIFIER") );

		b.rule("IDENTIFIER").concatenation( new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*") );
		
		return b.get();
	}

	@Test
	public void nonTerminalOnly_1() {
		try {
			Grammar g = nonTerminalOnly();
			String goal = "grammarDefinition";
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "a = b ;";
			text += "}";

			IParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void a() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();
			
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += " SP ?= ' ' ;" + System.lineSeparator();
			text += " a := 'a' ;" + System.lineSeparator();
			text += "}";

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
