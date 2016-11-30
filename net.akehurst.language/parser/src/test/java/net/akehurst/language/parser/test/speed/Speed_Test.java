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
package net.akehurst.language.parser.test.speed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.language.parser.AbstractParser_Test;

public class Speed_Test extends AbstractParser_Test {
	
	Grammar abcds() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abcds").choice(new NonTerminal("abcd"), new NonTerminal("abcds.group1"));
		b.rule("abcds.group1").concatenation(new NonTerminal("abcd"), new NonTerminal("abcds"));
		b.rule("abcd").concatenation(new NonTerminal("abs"), new NonTerminal("cds"));
		b.rule("abs").choice(new NonTerminal("ab"),new NonTerminal("abs.group1"));
		b.rule("abs.group1").concatenation(new NonTerminal("ab"), new NonTerminal("abs"));
		b.rule("cds").choice(new NonTerminal("cd"),new NonTerminal("cds.group1"));
		b.rule("cds.group1").concatenation(new NonTerminal("cd"), new NonTerminal("cds"));
		b.rule("ab").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("cd").concatenation(new NonTerminal("c"), new NonTerminal("d"));
		b.rule("a").choice(new TerminalLiteral("a"));
		b.rule("b").choice(new TerminalLiteral("b"));
		b.rule("c").choice(new TerminalLiteral("c"));
		b.rule("d").choice(new TerminalLiteral("d"));
		return b.get();
	}

	@Test
	public void abcds_abcds_abcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "abcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abcds_abcds_abcdabcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "abcdabcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abcds_abcds_ababcdcdababcdcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "ababcdcdababcdcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abcds_abcds_abababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "abababcdcdcdabababcdcdcdabababcdcdcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abcds_abcds_abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abcds_abcds_abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void abcds_abcds_2_x_abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			Grammar g = abcds();
			String goal = "abcds";
			String text = "abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	Grammar params() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("params").concatenation(new TerminalLiteral("("), new NonTerminal("paramList"),new TerminalLiteral(")"));
		b.rule("paramList").separatedList(0, -1, new TerminalLiteral(","), new NonTerminal("param"));
		b.rule("param").concatenation(new NonTerminal("type"),new NonTerminal("id"));
		b.rule("type").choice(new TerminalLiteral("int"));
		b.rule("id").choice(new TerminalPattern("[a-zA-Z_][a-zA-Z_0-9]*"));
		b.skip("WS").concatination( new TerminalPattern("\\s+") );
		return b.get();
	}
	
	@Test
	public void params_1() {
		// grammar, goal, input
		try {
			Grammar g = params();
			String goal = "params";
			String text = "(int a)";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	@Test
	public void params_2() {
		// grammar, goal, input
		try {
			Grammar g = params();
			String goal = "params";
			String text = "(int a,int b)";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	@Test
	public void params_3() {
		// grammar, goal, input
		try {
			Grammar g = params();
			String goal = "params";
			String text = "(int a, int b, int c)";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
