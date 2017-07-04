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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.akehurst.language.core.analyser.IGrammar;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class GrammarDefinitionBranch2Grammar implements IBinaryRule<INode, Grammar> {

	@Override
	public boolean isValidForLeft2Right(final INode left) {
		return left.getName().equals("grammarDefinition");
	}

	@Override
	public boolean isValidForRight2Left(final Grammar right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final INode left, final Grammar right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Grammar constructLeft2Right(final INode left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final IBranch namespaceBranch = (IBranch) ((IBranch) left).getChild(0);
		final IBranch grammarBranch = (IBranch) ((IBranch) left).getChild(1);
		final IBranch grammarNameBranch = (IBranch) grammarBranch.getChild(1);

		final Namespace namespace = transformer.transformLeft2Right(Node2Namespace.class, namespaceBranch);
		final String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, grammarNameBranch);

		final Grammar right = new Grammar(namespace, name);

		return right;

	}

	@Override
	public IBranch constructRight2Left(final Grammar arg0, final ITransformer arg1) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub, handle extends !!
		return null;
	}

	@Override
	public void updateLeft2Right(final INode left, final Grammar right, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final IBranch grammarBranch = (IBranch) ((IBranch) left).getChild(1);
		final IBranch extendsBranch = (IBranch) grammarBranch.getChild(2);
		final IBranch rulesBranch = (IBranch) grammarBranch.getChild(4);
		final List<INode> ruleBranches = rulesBranch.getNonSkipChildren();

		if (0 == extendsBranch.getMatchedTextLength()) {
			// no extended grammar
		} else {
			final IBranch extendsListBranch = (IBranch) ((IBranch) extendsBranch.getChild(0)).getChild(1);
			final List<String> extendsList = new ArrayList<>();
			for (final INode n : extendsListBranch.getNonSkipChildren()) {
				if ("qualifiedName".equals(n.getName())) {
					final IBranch b = (IBranch) n;
					String qualifiedName = n.getMatchedText().trim();
					if (1 == b.getNonSkipChildren().size()) {
						qualifiedName = right.getNamespace().getQualifiedName() + "::" + qualifiedName;
					}
					extendsList.add(qualifiedName);
				}
			}

			final List<IGrammar> extendedGrammars = ((SemanicAnalyser) transformer).getGrammarLoader().resolve(extendsList.toArray(new String[0]));
			final List<Grammar> grammars = extendedGrammars.stream().map((e) -> (Grammar) e).collect(Collectors.toList());
			right.setExtends(grammars);
		}

		final List<? extends Rule> rules = transformer.transformAllLeft2Right(AnyRuleNode2Rule.class, ruleBranches);
		right.setRule((List<Rule>) rules);

	}

	@Override
	public void updateRight2Left(final INode arg0, final Grammar arg1, final ITransformer arg2) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
