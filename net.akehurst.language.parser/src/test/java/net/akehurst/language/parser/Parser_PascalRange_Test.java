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

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class Parser_PascalRange_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}
	
	Grammar pascal() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("expr").choice(new NonTerminal("range"), new NonTerminal("real"));
		b.rule("range").concatenation(new NonTerminal("integer"), new TerminalLiteral(".."), new NonTerminal("integer"));
		b.rule("integer").concatenation(new TerminalPattern("[0-9]+"));
		b.rule("real").concatenation(new TerminalPattern("([0-9]+[.][0-9]*)|([.][0-9]+)"));

		return b.get();
	}
	
	@Test
	public void pascal_expr_p5() {
		// grammar, goal, input
		try {
			Grammar g = pascal();
			String goal = "expr";
			String text = ".5";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*expr 1, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("expr : [real : [\"([0-9]+[.][0-9]*)|([.][0-9]+)\" : \".5\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void pascal_expr_1p() {
		// grammar, goal, input
		try {
			Grammar g = pascal();
			String goal = "expr";
			String text = "1.";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*expr 1, 3}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("expr : [real : [\"([0-9]+[.][0-9]*)|([.][0-9]+)\" : \"1.\"]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void pascal_expr_1to5() {
		// grammar, goal, input
		try {
			Grammar g = pascal();
			String goal = "expr";
			String text = "1..5";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
			
			ToStringVisitor v = new ToStringVisitor("","");
			String st = tree.accept(v, "");
			Assert.assertEquals("{*expr 1, 5}",st);
			
			String nt = tree.getRoot().accept(v, "");
			Assert.assertEquals("expr : [range : [integer : [\"[0-9]+\" : \"1\"], '..' : \"..\", integer : [\"[0-9]+\" : \"5\"]]]",nt);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
