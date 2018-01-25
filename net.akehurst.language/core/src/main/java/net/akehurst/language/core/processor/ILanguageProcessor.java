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
package net.akehurst.language.core.processor;

import java.io.Reader;
import java.util.List;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.parser.ICompletionItem;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;

public interface ILanguageProcessor {
	IGrammar getGrammar();

	IParser getParser();

	// INodeType getDefaultGoal();

	<T> T process(String text, String goalRuleName, Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception;

	<T> T process(Reader reader, String goalRuleName, Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception;

	/**
	 * returns list of names of expected rules
	 *
	 * @param text
	 *            text to parse
	 * @param goalRuleName
	 *            name of a rule in the grammar that is the goal rule
	 * @param position
	 *            position in the text (from reader) at which to provide completions
	 * @param desiredDepth
	 *            depth of nested rules to search when constructing possible completions
	 * @return
	 * @throws ParseFailedException
	 * @throws ParseTreeException
	 */
	List<ICompletionItem> expectedAt(String text, String goalRuleName, int position, int desiredDepth) throws ParseFailedException, ParseTreeException;

	List<ICompletionItem> expectedAt(Reader reader, String goalRuleName, int position, int desiredDepth) throws ParseFailedException, ParseTreeException;
}