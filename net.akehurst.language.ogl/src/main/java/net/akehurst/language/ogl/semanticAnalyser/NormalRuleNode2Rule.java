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

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.RuleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class NormalRuleNode2Rule implements Relation<INode, Rule> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "normalRule".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(Rule right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Rule constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode grammarNode = left.getParent().getParent().getParent().getParent();
			Grammar grammar = transformer.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, grammarNode);
			String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch) left).getChild(0));
			Rule right = new Rule(grammar, name);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public INode constructRight2Left(Rule right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Rule right, Transformer transformer) {
		try {
			INode rhsNode = ((IBranch) left).getChild(2);
			INode item = ((IBranch) rhsNode).getChild(0);
			AbstractChoice ruleItem = transformer
					.transformLeft2Right((Class<Relation<INode, AbstractChoice>>) (Class<?>) AbstractNode2Choice.class, item);
			right.setRhs(ruleItem);
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public void configureRight2Left(INode left, Rule right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
