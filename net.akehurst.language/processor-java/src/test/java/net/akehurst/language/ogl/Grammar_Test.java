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

import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.api.grammar.NodeType;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

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

		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "HelloWorld");
		b.rule("root").concatenation(new NonTerminalDefault("hello"), new NonTerminalDefault("world"));
		b.rule("hello").concatenation(new TerminalLiteralDefault("hello"));
		b.rule("world").concatenation(new TerminalLiteralDefault("world!"));
		final GrammarDefault g = b.get();
	}

	@Test
	public void findAllNodeType() {

		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "HelloWorld");
		b.rule("root").concatenation(new NonTerminalDefault("hello"), new NonTerminalDefault("world"));
		b.rule("hello").concatenation(new TerminalLiteralDefault("hello"));
		b.rule("world").concatenation(new TerminalLiteralDefault("world!"));
		final GrammarDefault g = b.get();

		final Set<NodeType> types = g.findAllNodeType();

		Assert.assertEquals(5, types.size());
	}

	@Test
	public void test() {

		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "TestG");
		b.skip("SKIP").concatenation(new TerminalPatternDefault("\\s+"));
		b.rule("port").concatenation(new TerminalLiteralDefault("port"));
		final GrammarDefault g = b.get();

		final Set<NodeType> types = g.findAllNodeType();

		Assert.assertEquals(4, types.size());
	}

}
