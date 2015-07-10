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
package net.akehurst.language.parser.test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.forrest.ForrestFactory;
import net.akehurst.language.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.parser.runtime.Factory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Parser_Choice_Test extends AbstractParser_Test {
	
	Grammar abc() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").choice(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}
	
	Grammar aempty() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").choice();
		return b.get();
	}
	
	@Test
	public void aempty_a_empty() {
		// grammar, goal, input
		try {
			Grammar g = aempty();
			String goal = "a";
			String text = "";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("", "");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*a 1, 1}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
					b.branch("a",
						b.leaf("", "")
					);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abc_abc_a() {
		// grammar, goal, input
		try {
			Grammar g = abc();
			String goal = "abc";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc 1, 2}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
				b.branch("abc",
					b.branch("a",
						b.leaf("a", "a")
					)
				);
			Assert.assertEquals(expected, tree.getRoot());

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc_abc_b() {
		// grammar, goal, input
		try {
			Grammar g = abc();
			String goal = "abc";
			String text = "b";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc 1, 2}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
				b.branch("abc",
					b.branch("b",
						b.leaf("b", "b")
					)
				);
			
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc_abc_c() {
		// grammar, goal, input
		try {
			Grammar g = abc();
			String goal = "abc";
			String text = "c";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*abc 1, 2}",st);
			
			ParseTreeBuilder b = this.builder(g, text, goal);
			IBranch expected = 
				b.branch("abc",
					b.branch("c",
						b.leaf("c", "c")
					)
				);
			
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
