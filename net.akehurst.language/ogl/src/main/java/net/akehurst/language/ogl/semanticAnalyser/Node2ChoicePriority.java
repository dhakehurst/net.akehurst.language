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

import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.ogl.semanticStructure.ChoicePriority;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2ChoicePriority extends AbstractNode2Choice<ChoicePriority> {

	@Override
	public String getNodeName() {
		return "priorityChoice";
	}

	@Override
	public boolean isValidForRight2Left(final ChoicePriority right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final ISPBranch left, final ChoicePriority right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ChoicePriority constructLeft2Right(final ISPBranch left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final List<? extends ISPNode> allLeft = left.getNonSkipChildren();
		List<? extends Concatenation> allRight;

		final List<ISPNode> concatenationNodes = new ArrayList<>();
		for (final ISPNode n : allLeft) {
			if ("concatenation".equals(n.getName())) {
				concatenationNodes.add(n);
			}
		}

		allRight = transformer.transformAllLeft2Right((Class<IBinaryRule<ISPNode, Concatenation>>) (Class<?>) Node2Concatenation.class, concatenationNodes);

		final ChoicePriority right = new ChoicePriority(allRight.toArray(new Concatenation[0]));
		return right;

	}

	@Override
	public ISPBranch constructRight2Left(final ChoicePriority right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final ISPBranch left, final ChoicePriority right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPBranch left, final ChoicePriority right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
