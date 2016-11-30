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
package net.akehurst.language.parser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class Parser_Expressions_Test extends AbstractParser_Test {
	
	Grammar expression_plus_multiply() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("e"));
		b.rule("e").choice(new NonTerminal("plus"), new NonTerminal("multiply"), new NonTerminal("variable"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}
	
	
	@Test
	public void emp_S_a() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
//the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("variable",
							b.leaf("a")
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
	
	@Test
	public void emp_S_apa() {
		// grammar, goal, input
		try {
			Grammar g = expression_plus_multiply();
			String goal = "S";
			String text = "a+a";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			//the tree is marked as if it can still grow because the top rule is multi(1-3)
			
			ParseTreeBuilder b = this.builder(g, text, goal);;
			IBranch expected = 
				b.branch("S",
					b.branch("e",
						b.branch("plus",
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							),
							b.leaf("+"),
							b.branch("e",
								b.branch("variable",
									b.leaf("a")
								)
							)
						)
					)
				);
			Assert.assertEquals(expected, tree.getRoot());
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}	
	}
}
