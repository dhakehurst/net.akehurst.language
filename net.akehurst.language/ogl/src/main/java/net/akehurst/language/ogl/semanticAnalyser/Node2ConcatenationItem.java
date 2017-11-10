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

import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2ConcatenationItem extends AbstractSemanticAnalysisRule<ConcatenationItem> {

	@Override
	public String getNodeName() {
		return "concatenationItem";
	}

	@Override
	public boolean isAMatch(final ISPPFBranch left, final ConcatenationItem right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ConcatenationItem constructLeft2Right(final ISPPFBranch left, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		final ISPPFNode itemNode = left.getChild(0);

		final ConcatenationItem right = transformer
				.transformLeft2Right((Class<IBinaryRule<ISPPFNode, TangibleItem>>) (Class<?>) AbstractNode2ConcatenationItem.class, itemNode);
		return right;
	}

	@Override
	public ISPPFBranch constructRight2Left(final ConcatenationItem right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final ISPPFBranch left, final ConcatenationItem right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPPFBranch left, final ConcatenationItem right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
