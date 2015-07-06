/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
