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

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.GrammarBuilder;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.test.AbstractParser_Test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ListsSpeed_Test extends AbstractParser_Test {

	@Before
	public void before() {
		this.parseTreeFactory = new Factory();
	}
	
	Grammar as_rr() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new NonTerminal("as"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as_lr() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("as"), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as_multi() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(1, -1, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar as2_rr() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new TerminalLiteral(","), new NonTerminal("as"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as2_lr() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("as"), new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as2_multi() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(1, -1, new NonTerminal("as$group1"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new TerminalLiteral(","));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}
	
	Grammar as2_sl() {
		GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new TerminalLiteral("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}
	
	@Test
	public void rr_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as_rr();
			String goal = "as";
			String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void lr_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as_lr();
			String goal = "as";
			String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void multi_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as_multi();
			String goal = "as";
			String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void rr_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as2_rr();
			String goal = "as";
			String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	
	@Test
	public void lr_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as2_lr();
			String goal = "as";
			String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void multi_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as2_multi();
			String goal = "as";
			String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void sl_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() {
		// grammar, goal, input
		try {
			Grammar g = as2_sl();
			String goal = "as";
			String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
}
