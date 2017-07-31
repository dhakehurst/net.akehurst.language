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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class Grammar_Test {

	@Test
	public void helloWorld() {
		// namespace test;
		// grammar HelloWorld {
		// root = hello whitespace world ;
		// hello = 'hello' ;
		// world = 'world!' ;
		// whitespace = "\\s+";
		// }

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "HelloWorld");
		b.rule("root").concatenation(new NonTerminal("hello"), new NonTerminal("world"));
		b.rule("hello").concatenation(new TerminalLiteral("hello"));
		b.rule("world").concatenation(new TerminalLiteral("world!"));
		final Grammar g = b.get();
	}

	@Test
	public void findAllNodeType() {

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "HelloWorld");
		b.rule("root").concatenation(new NonTerminal("hello"), new NonTerminal("world"));
		b.rule("hello").concatenation(new TerminalLiteral("hello"));
		b.rule("world").concatenation(new TerminalLiteral("world!"));
		final Grammar g = b.get();

		final Set<INodeType> types = g.findAllNodeType();

		Assert.assertEquals(5, types.size());
	}

	@Test
	public void test() {

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "TestG");
		b.skip("SKIP").concatenation(new TerminalPattern("\\s+"));
		b.rule("port").concatenation(new TerminalLiteral("port"));
		final Grammar g = b.get();

		final Set<INodeType> types = g.findAllNodeType();

		Assert.assertEquals(4, types.size());
	}

}
