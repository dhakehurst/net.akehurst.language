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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.event.ListSelectionEvent;

import net.akehurst.language.core.analyser.IGrammar;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class GrammarDefinitionBranch2Grammar implements Relation<INode, Grammar> {

	@Override
	public void configureLeft2Right(INode left, Grammar right, Transformer transformer) {
		try {
			IBranch grammarBranch = (IBranch) ((IBranch)left).getChild(1);
			IBranch extendsBranch = (IBranch) grammarBranch.getChild(2);
			IBranch rulesBranch = (IBranch) grammarBranch.getChild(4);
			List<INode> ruleBranches = rulesBranch.getNonSkipChildren();
			
			if (0==extendsBranch.getMatchedTextLength()) {
				//no extended grammar
			} else {
				IBranch extendsListBranch = (IBranch) ((IBranch)extendsBranch.getChild(0)).getChild(1);
				List<String> extendsList = new ArrayList<>();
				for (INode n: extendsListBranch.getNonSkipChildren()) {
					if ("qualifiedName".equals(n.getName())) {
						IBranch b = (IBranch)n;
						String qualifiedName = n.getMatchedText().trim();
						if (1==b.getNonSkipChildren().size()) {
							qualifiedName = right.getNamespace().getQualifiedName() + "::"+qualifiedName;
						}
						extendsList.add(qualifiedName);
					}
				}
				
				List<IGrammar> extendedGrammars =((SemanicAnalyser)transformer).getGrammarLoader().resolve(extendsList.toArray(new String[0]));
				List<Grammar> grammars = extendedGrammars.stream().map((e)->(Grammar)e).collect(Collectors.toList());
				right.setExtends(grammars);
			}
			
			List<? extends Rule> rules = transformer.transformAllLeft2Right(AnyRuleNode2Rule.class, ruleBranches);
			right.setRule((List<Rule>) rules);
			
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public void configureRight2Left(INode arg0, Grammar arg1, Transformer arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public Grammar constructLeft2Right(INode left, Transformer transformer) {
		try {
			IBranch namespaceBranch = (IBranch) ((IBranch)left).getChild(0);
			IBranch grammarBranch = (IBranch) ((IBranch)left).getChild(1);
			IBranch grammarNameBranch = (IBranch) grammarBranch.getChild(1);

			Namespace namespace = transformer.transformLeft2Right(Node2Namespace.class, namespaceBranch);
			String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, grammarNameBranch);

			Grammar right = new Grammar(namespace, name);

			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to construct Grammar", e);
		}
	}

	@Override
	public IBranch constructRight2Left(Grammar arg0, Transformer arg1) {
		// TODO Auto-generated method stub, handle extends !!
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return left.getName().equals("grammarDefinition");
	}

	@Override
	public boolean isValidForRight2Left(Grammar right) {
		// TODO Auto-generated method stub
		return false;
	}

}
