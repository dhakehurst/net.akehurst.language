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
package net.akehurst.language.processor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class OGL_Test extends AbstractParser_Test {

	Grammar ogl() {
		return new OGLGrammar();
	}

	@Test
	public void ogl_grammarDefinition_1_normalRule() {
		// grammar, goal, input
		try {
			Grammar g = ogl();
			String goal = "grammarDefinition";
			String text = "namespace test; grammar G { a : 'a' ;}";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ogl_grammarDefinition_2_normalRule() {
		// grammar, goal, input
		try {
			Grammar g = ogl();
			String goal = "grammarDefinition";
			String text = "namespace test; grammar G { a : 'a'; b:'b'; }";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ogl_grammarDefinition_3_normalRule() {
		// grammar, goal, input
		try {
			Grammar g = ogl();
			String goal = "grammarDefinition";
			String text = "namespace test; grammar G { a : 'a'; b:'b'; c:'c'; }";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ogl_grammarDefinition_choice() {
		// grammar, goal, input
		try {
			Grammar g = ogl();
			String goal = "grammarDefinition";
			String text = "namespace test; grammar G { abc:a|b|c; a : 'a'; b:'b'; c:'c'; }";
			
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ogl_grammarDefinition_normalRule_minimumWS() {
		// grammar, goal, input
		try {
			Grammar g = ogl();
			String goal = "grammarDefinition";
			String text = "namespace test; grammar G {sp:' ';}";
			//String text = "namespace test; grammar A { SP ?= ' ' ; a := 'a' ; }";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void ogl_grammarDefinition_normalRule_lotsWS() {
		// grammar, goal, input
		try {
			Grammar g = ogl();
			String goal = "grammarDefinition";
			String text = " namespace test ;  grammar G  {  sp  :   ' '  ;  } ";
			//String text = "namespace test; grammar A { SP ?= ' ' ; a := 'a' ; }";
			
			IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);
		
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
