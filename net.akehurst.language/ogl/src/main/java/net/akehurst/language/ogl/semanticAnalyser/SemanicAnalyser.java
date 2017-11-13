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
package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.analyser.IGrammarLoader;
import net.akehurst.language.core.analyser.ISemanticAnalyser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.transform.binary.BinaryTransformer;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class SemanicAnalyser extends BinaryTransformer implements ISemanticAnalyser {

	private IGrammarLoader grammarLoader;

	public SemanicAnalyser() {
		super.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractNode2Choice.class);
		super.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractNode2ConcatenationItem.class);
		super.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractNode2TangibleItem.class);
		super.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractNode2Terminal.class);
		super.registerRule((Class<? extends IBinaryRule<?, ?>>) (Class<?>) AbstractRhsNode2RuleItem.class);
		super.registerRule(AnyRuleNode2Rule.class);
		super.registerRule(GrammarDefinitionBranch2Grammar.class);
		super.registerRule(IDENTIFIERBranch2String.class);
		super.registerRule(Node2ChoicePriority.class);
		super.registerRule(Node2ChoiceSimple.class);
		super.registerRule(Node2Concatenation.class);
		super.registerRule(Node2ConcatenationItem.class);
		super.registerRule(Node2Group.class);
		super.registerRule(Node2Multi.class);
		super.registerRule(Node2Namespace.class);
		super.registerRule(Node2NonTerminal.class);
		super.registerRule(Node2SeparatedList.class);
		super.registerRule(Node2SimpleItem.class);
		super.registerRule(Node2Terminal.class);
		super.registerRule(NormalRuleNode2Rule.class);
		super.registerRule(SkipRuleNode2SkipRule.class);
		super.registerRule(TerminalLiteralNode2Terminal.class);
		super.registerRule(TerminalPatternNode2Terminal.class);
	}

	Grammar analyse(final ISharedPackedParseTree parseTree) throws UnableToAnalyseExeception {
		try {
			final ISPBranch root = (ISPBranch) parseTree.getRoot();
			final Grammar grammar = this.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, root);
			return grammar;
		} catch (final RuleNotFoundException | TransformException e) {
			throw new UnableToAnalyseExeception("Cannot Analyse ParseTree", e);
		}

	}

	@Override
	public <T> T analyse(final Class<T> targetType, final ISharedPackedParseTree forest) throws UnableToAnalyseExeception {
		return (T) this.analyse(forest);
	}

	@Override
	public IGrammarLoader getGrammarLoader() {
		return this.grammarLoader;
	}

	@Override
	public void setGrammarLoader(final IGrammarLoader value) {
		this.grammarLoader = value;
	}
}
