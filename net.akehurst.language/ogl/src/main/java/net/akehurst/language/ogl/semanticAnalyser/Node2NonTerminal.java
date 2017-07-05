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
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2NonTerminal extends AbstractNode2TangibleItem<NonTerminal> {

	@Override
	public String getNodeName() {
		return "nonTerminal";
	}

	@Override
	public boolean isValidForRight2Left(final NonTerminal right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final IBranch left, final NonTerminal right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NonTerminal constructLeft2Right(final IBranch left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		// final INode ruleOwner = left.getParent();

		final String referencedRuleName = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, left.getChild(0));
		// final Rule owner = transformer.transformLeft2Right(NormalRuleNode2Rule.class, left);
		// final List<Integer> index = new ArrayList<>();
		final NonTerminal right = new NonTerminal(referencedRuleName);// , owner, index);
		return right;

	}

	@Override
	public IBranch constructRight2Left(final NonTerminal right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final IBranch left, final NonTerminal right, final ITransformer transformer) throws RuleNotFoundException, TransformException {

	}

	@Override
	public void updateRight2Left(final IBranch left, final NonTerminal right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
