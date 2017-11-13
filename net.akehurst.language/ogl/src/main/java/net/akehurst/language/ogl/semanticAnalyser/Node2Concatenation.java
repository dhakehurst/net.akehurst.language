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

import java.util.List;

import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2Concatenation extends AbstractSemanticAnalysisRule<Concatenation> {

	@Override
	public String getNodeName() {
		return "concatenation";
	}

	@Override
	public boolean isValidForRight2Left(final Concatenation right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final ISPBranch left, final Concatenation right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Concatenation constructLeft2Right(final ISPBranch left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final List<? extends ISPNode> allLeft = left.getNonSkipChildren();
		List<? extends ConcatenationItem> allRight;

		allRight = transformer.transformAllLeft2Right((Class<IBinaryRule<ISPNode, ConcatenationItem>>) (Class<?>) Node2ConcatenationItem.class, allLeft);

		final Concatenation right = new Concatenation(allRight.toArray(new ConcatenationItem[allRight.size()]));
		return right;

	}

	@Override
	public ISPBranch constructRight2Left(final Concatenation right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final ISPBranch left, final Concatenation right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPBranch left, final Concatenation right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
