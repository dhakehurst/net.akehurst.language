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

import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.sppf.IParseTree;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public class test_OGL_Dot {

	<T> T process(final String grammarText, final Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();

			// List<IToken> tokens = proc.getLexicalAnaliser().lex(grammar);
			final IParseTree tree = proc.getParser().parse("grammarDefinition", grammarText);
			final T t = proc.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (final RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (final ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	<T> T processFile(final String grammarFile, final Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(grammarFile);
			final InputStreamReader reader = new InputStreamReader(is);

			final IParseTree tree = proc.getParser().parse("grammarDefinition", reader);
			final T t = proc.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (final RuleNotFoundException | ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	IParseTree processDot(final String ruleName, final String text) {
		try {
			final String grammarFile = "Dot.ogl";
			final Grammar grammar = this.processFile(grammarFile, Grammar.class);
			Assert.assertNotNull(grammar);
			final LanguageProcessor proc = new LanguageProcessor(grammar, null);
			final IParseTree tree = proc.getParser().parse(ruleName, text);
			return tree;
		} catch (ParseFailedException | UnableToAnalyseExeception | ParseTreeException | RuleNotFoundException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
			return null;
		}
	}

	@Test
	public void test() {

		final IParseTree tree = this.processDot("graph", "graph { }");
		Assert.assertNotNull(tree);

	}

	@Test
	public void test2() {

		final IParseTree tree = this.processDot("graph", "sTriCt GRapH { }");
		Assert.assertNotNull(tree);

	}

}
