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
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.SkipRule;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class SkipRuleNode2SkipRule extends NormalRuleNode2Rule {

	@Override
	public boolean isValidForLeft2Right(final INode left) {
		return "skipRule".equals(left.getName());
	}

	@Override
	public SkipRule constructLeft2Right(final INode left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final INode grammarNode = left.getParent().getParent().getParent().getParent();
		final Grammar grammar = transformer.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, grammarNode);
		final String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch) left).getChild(1));
		final SkipRule right = new SkipRule(grammar, name);
		return right;

	}

	@Override
	public void updateLeft2Right(final INode left, final Rule right, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final INode rhsNode = ((IBranch) left).getChild(3);
		final INode item = ((IBranch) rhsNode).getChild(0);
		final AbstractChoice ruleItem = transformer.transformLeft2Right((Class<IBinaryRule<INode, AbstractChoice>>) (Class<?>) AbstractNode2Choice.class, item);
		right.setRhs(ruleItem);

	}

}
