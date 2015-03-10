package net.akehurst.language.ogl;

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
		b.rule("root").concatenation(new NonTerminal("hello"), new NonTerminal("world"));
		b.rule("hello").concatenation(new TerminalLiteral("hello"));
		b.rule("world").concatenation(new TerminalLiteral("world!"));
		Grammar g = b.get();
	}
}
