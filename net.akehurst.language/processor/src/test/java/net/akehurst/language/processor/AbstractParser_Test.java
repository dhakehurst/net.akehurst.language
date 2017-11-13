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

import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;

abstract public class AbstractParser_Test {

	protected RuntimeRuleSetBuilder parseTreeFactory;

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}

	ParseTreeBuilder builder(final Grammar grammar, final String text, final String goal) {
		return new ParseTreeBuilder(this.parseTreeFactory, grammar, goal, text, 0);
	}

	protected ISharedPackedParseTree process(final Grammar grammar, final String text, final String goalName) throws ParseFailedException {
		try {
			final IParser parser = new ScannerLessParser3(this.parseTreeFactory, grammar);
			final ISharedPackedParseTree tree = parser.parse(goalName, text);
			return tree;
		} catch (final RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (final ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

}
