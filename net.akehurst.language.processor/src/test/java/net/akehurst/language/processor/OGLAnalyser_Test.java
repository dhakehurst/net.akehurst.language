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

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;

import org.junit.Assert;
import org.junit.Test;

public class OGLAnalyser_Test {

	<T> T process(String grammarText, Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();

			// List<IToken> tokens = proc.getLexicalAnaliser().lex(grammar);
			INodeType goal = proc.getGrammar().findRule("grammarDefinition").getNodeType();
			IParseTree tree = proc.getParser().parse(goal, grammarText);
			T t = proc.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	@Test
	public void a_a_a_A() {
		// grammar, goal, input, target
		try {
			String grammarText = "namespace test;" + System.lineSeparator();
			grammarText += "grammar A {" + System.lineSeparator();
			grammarText += " a : 'a' ;" + System.lineSeparator();
			grammarText += "}";

			Grammar grammar = this.process(grammarText, Grammar.class);
			Assert.assertNotNull(grammar);

			LanguageProcessor proc = new LanguageProcessor(grammar, "a", null);
			
			IParseTree tree = proc.getParser().parse(proc.getDefaultGoal(), "a");
			Assert.assertNotNull(tree);
			
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		} catch (UnableToAnalyseExeception e) {
			Assert.fail(e.getMessage());
		} catch (ParseTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
