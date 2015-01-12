package net.akehurst.language.ebnf;

import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;

import org.junit.Test;

public class Grammar_Test {

	@Test
	public void helloWorld() {
		// namespace test;
		// grammar HelloWorld {
		//   root = hello whitespace world ;
		//   hello = 'hello' ;
		//   world = 'world!' ;
		//   whitespace = "\\s+";
		// }
		
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "HelloWorld");
		b.rule("root").concatination(new NonTerminal("hello"), new NonTerminal("world"));
		b.rule("hello").concatination(new TerminalLiteral("hello"));
		b.rule("world").concatination(new TerminalLiteral("world!"));
		Grammar g = b.get();
	}
}
