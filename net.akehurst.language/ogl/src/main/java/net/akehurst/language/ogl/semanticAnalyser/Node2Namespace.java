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
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2Namespace implements IBinaryRule<ISPPFBranch, Namespace> {

	@Override
	public boolean isAMatch(final ISPPFBranch left, final Namespace right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateLeft2Right(final ISPPFBranch arg0, final Namespace arg1, final ITransformer arg2) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPPFBranch arg0, final Namespace arg1, final ITransformer arg2) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public Namespace constructLeft2Right(final ISPPFBranch left, final ITransformer arg1) throws RuleNotFoundException, TransformException {
		final String qualifiedName = left.getChild(1).getMatchedText().trim();
		return new Namespace(qualifiedName);
	}

	@Override
	public ISPPFBranch constructRight2Left(final Namespace arg0, final ITransformer arg1) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(final ISPPFBranch left) {
		// TODO Auto-generated method stub
		return "namespace".equals(left.getChild(0).getName());
	}

	@Override
	public boolean isValidForRight2Left(final Namespace arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
