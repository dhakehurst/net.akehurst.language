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

import net.akehurst.language.core.sppf.ILeaf;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;

public class IDENTIFIERBranch2String implements IBinaryRule<ISPPFNode, String> {

	@Override
	public boolean isAMatch(final ISPPFNode left, final String right, final ITransformer transformer) throws RuleNotFoundException {
		return true;
	}

	@Override
	public void updateLeft2Right(final ISPPFNode arg0, final String arg1, final ITransformer arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPPFNode arg0, final String arg1, final ITransformer arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public String constructLeft2Right(final ISPPFNode left, final ITransformer transformer) {
		final ILeaf leaf = (ILeaf) ((ISPPFBranch) left).getChildren().get(0);
		final String right = leaf.getMatchedText();
		return right;
	}

	@Override
	public ISPPFBranch constructRight2Left(final String arg0, final ITransformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(final ISPPFNode left) {
		return left.getName().equals("IDENTIFIER");
	}

	@Override
	public boolean isValidForRight2Left(final String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
