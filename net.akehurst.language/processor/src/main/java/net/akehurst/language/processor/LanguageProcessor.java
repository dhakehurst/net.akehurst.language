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

import java.io.Reader;

import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.IGrammar;
import net.akehurst.language.core.analyser.ISemanticAnalyser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;

public class LanguageProcessor implements ILanguageProcessor {

	public LanguageProcessor(final IGrammar grammar, final ISemanticAnalyser semanticAnalyser) {
		this.grammar = grammar;
		// this.defaultGoalName = defaultGoalName;
		// this.lexicalAnalyser = new LexicalAnalyser(grammar.findTokenTypes());
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
		this.parser = new ScannerLessParser3(this.parseTreeFactory, grammar);
		this.semanticAnalyser = semanticAnalyser;
	}

	private final RuntimeRuleSetBuilder parseTreeFactory;
	private final IGrammar grammar;
	private final IParser parser;
	private final ISemanticAnalyser semanticAnalyser;

	@Override
	public IGrammar getGrammar() {
		return this.grammar;
	}

	@Override
	public IParser getParser() {
		return this.parser;
	}

	public ISemanticAnalyser getSemanticAnalyser() {
		return this.semanticAnalyser;
	}

	@Override
	public <T> T process(final Reader reader, final String grammarRuleName, final Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
		try {

			final IParseTree tree = this.getParser().parse(grammarRuleName, reader);
			if (null == this.getSemanticAnalyser()) {
				throw new UnableToAnalyseExeception("No SemanticAnalyser supplied", null);
			}
			final T t = this.getSemanticAnalyser().analyse(targetType, tree);

			return t;
		} catch (final RuleNotFoundException e) {
			throw new ParseFailedException(e.getMessage(), null);
		} catch (final ParseTreeException e) {
			throw new ParseFailedException(e.getMessage(), null);
		}
	}
}
