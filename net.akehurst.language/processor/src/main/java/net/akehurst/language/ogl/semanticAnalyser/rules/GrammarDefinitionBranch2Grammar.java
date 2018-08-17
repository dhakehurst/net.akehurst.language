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
package net.akehurst.language.ogl.semanticAnalyser.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticAnalyser.OglSemanicAnalyserRuleBased;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.RuleDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class GrammarDefinitionBranch2Grammar implements BinaryRule<SPPTNode, GrammarDefault> {

	@Override
	public boolean isValidForLeft2Right(final SPPTNode left) {
		return left.getName().equals("grammarDefinition");
	}

	@Override
	public boolean isValidForRight2Left(final GrammarDefault right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final SPPTNode left, final GrammarDefault right, final BinaryTransformer transformer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GrammarDefault constructLeft2Right(final SPPTNode left, final BinaryTransformer transformer) {

		final SPPTBranch namespaceBranch = (SPPTBranch) ((SPPTBranch) left).getChild(0);
		final SPPTBranch grammarBranch = (SPPTBranch) ((SPPTBranch) left).getChild(1);
		final SPPTBranch grammarNameBranch = (SPPTBranch) grammarBranch.getChild(1);

		final NamespaceDefault namespace = transformer.transformLeft2Right(Node2Namespace.class, namespaceBranch);
		final String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, grammarNameBranch);

		final GrammarDefault right = new GrammarDefault(namespace, name);

		return right;

	}

	@Override
	public SPPTBranch constructRight2Left(final GrammarDefault arg0, final BinaryTransformer arg1) {
		// TODO Auto-generated method stub, handle extends !!
		return null;
	}

	@Override
	public void updateLeft2Right(final SPPTNode left, final GrammarDefault right, final BinaryTransformer transformer) {

		final SPPTBranch grammarBranch = (SPPTBranch) ((SPPTBranch) left).getChild(1);
		final SPPTBranch extendsBranch = (SPPTBranch) grammarBranch.getChild(2);
		final SPPTBranch rulesBranch = (SPPTBranch) grammarBranch.getChild(4);
		final List<SPPTBranch> ruleBranches = rulesBranch.getBranchNonSkipChildren();

		if (0 == extendsBranch.getMatchedTextLength()) {
			// no extended grammar
		} else {
			final SPPTBranch extendsListBranch = (SPPTBranch) ((SPPTBranch) extendsBranch.getChild(0)).getChild(1);
			final List<String> extendsList = new ArrayList<>();
			for (final SPPTNode n : extendsListBranch.getNonSkipChildren()) {
				if ("qualifiedName".equals(n.getName())) {
					final SPPTBranch b = (SPPTBranch) n;
					String qualifiedName = n.getMatchedText().trim();
					if (1 == b.getNonSkipChildren().size()) {
						qualifiedName = right.getNamespace().getQualifiedName() + "::" + qualifiedName;
					}
					extendsList.add(qualifiedName);
				}
			}

			final List<Grammar> extendedGrammars = ((OglSemanicAnalyserRuleBased) transformer).getGrammarLoader().resolve(extendsList.toArray(new String[0]));
			final List<GrammarDefault> grammars = extendedGrammars.stream().map((e) -> (GrammarDefault) e).collect(Collectors.toList());
			right.setExtends(grammars);
		}

		final List<? extends RuleDefault> rules = transformer.transformAllLeft2Right(AnyRuleNode2Rule.class, ruleBranches);
		right.setRule((List<Rule>) (List<?>) rules);

	}

	@Override
	public void updateRight2Left(final SPPTNode arg0, final GrammarDefault arg1, final BinaryTransformer arg2) {
		// TODO Auto-generated method stub

	}

}
