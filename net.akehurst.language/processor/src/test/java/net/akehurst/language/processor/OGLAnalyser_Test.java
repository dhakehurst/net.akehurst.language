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
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.analyser.UnableToAnalyseExeception;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;

public class OGLAnalyser_Test {

	<T> T process(final String grammarText, final Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();

			// List<IToken> tokens = proc.getLexicalAnaliser().lex(grammar);
			final SharedPackedParseTree tree = proc.getParser().parse("grammarDefinition", grammarText);
			final T t = proc.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (final GrammarRuleNotFoundException e) {
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

			final SharedPackedParseTree tree = proc.getParser().parse("grammarDefinition", reader);
			final T t = proc.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (final GrammarRuleNotFoundException | ParseTreeException e) {
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

			final GrammarDefault grammar = this.process(grammarText, GrammarDefault.class);
			Assert.assertNotNull(grammar);

			final LanguageProcessorDefault proc = new LanguageProcessorDefault(grammar, null);

			final SharedPackedParseTree tree = proc.getParser().parse("a", new StringReader("a"));
			Assert.assertNotNull(tree);

		} catch (ParseFailedException | UnableToAnalyseExeception | ParseTreeException | GrammarRuleNotFoundException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void singleQuote_singleQuote_q() {
		// grammar, goal, input, target
		try {
			String grammarText = "namespace test;" + System.lineSeparator();
			grammarText += "grammar A {" + System.lineSeparator();
			grammarText += " singleQuote : '\\'' ;" + System.lineSeparator();
			grammarText += "}";

			final GrammarDefault grammar = this.process(grammarText, GrammarDefault.class);
			Assert.assertNotNull(grammar);

			final LanguageProcessorDefault proc = new LanguageProcessorDefault(grammar, null);

			final SharedPackedParseTree tree = proc.getParser().parse("singleQuote", "'");
			Assert.assertNotNull(tree);

		} catch (ParseFailedException | UnableToAnalyseExeception | ParseTreeException | GrammarRuleNotFoundException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void StringCharacter_StringCharacter_a() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException, UnableToAnalyseExeception {
		// grammar, goal, input, target

		String grammarText = "namespace test;" + System.lineSeparator();
		grammarText += "grammar A {" + System.lineSeparator();
		grammarText += " StringCharacter : \"[^\\x22\\\\]\" ;" + System.lineSeparator();
		grammarText += "}";

		final GrammarDefault grammar = this.process(grammarText, GrammarDefault.class);
		Assert.assertNotNull(grammar);

		final LanguageProcessorDefault proc = new LanguageProcessorDefault(grammar, null);

		final SharedPackedParseTree tree = proc.getParser().parse("StringCharacter", "a");
		Assert.assertNotNull(tree);

	}

	@Test
	public void StringLiteral_StringLiteral_1() throws ParseFailedException, UnableToAnalyseExeception, ParseTreeException, GrammarRuleNotFoundException {
		// grammar, goal, input, target

		String grammarText = "namespace test;" + System.lineSeparator();
		grammarText += "grammar A {" + System.lineSeparator();
		grammarText += " StringLiteral : '\"' StringCharacters? '\"' ;" + System.lineSeparator();
		grammarText += " StringCharacters : StringCharacter+ ;" + System.lineSeparator();
		grammarText += " StringCharacter : \"[^\\x22\\\\]\" ;" + System.lineSeparator();
		grammarText += "}";

		final GrammarDefault grammar = this.process(grammarText, GrammarDefault.class);
		Assert.assertNotNull(grammar);

		final LanguageProcessorDefault proc = new LanguageProcessorDefault(grammar, null);

		final SharedPackedParseTree tree = proc.getParser().parse("StringLiteral", "\"abc\"");
		Assert.assertNotNull(tree);

	}
}
