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

import net.akehurst.language.core.sppt.ISPLeaf;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;

public class TerminalPatternNode2Terminal extends AbstractNode2Terminal<TerminalPattern> {

	@Override
	public String getNodeName() {
		return "PATTERN";
	}

	@Override
	public boolean isValidForRight2Left(final TerminalPattern right) {
		return true;
	}

	@Override
	public boolean isAMatch(final ISPBranch left, final TerminalPattern right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TerminalPattern constructLeft2Right(final ISPBranch left, final ITransformer transformer) {
		final ISPNode child = left.getChildren().get(0);
		final ISPLeaf leaf = (ISPLeaf) child;
		final String text = leaf.getMatchedText();
		final String pattern = text.substring(1, text.length() - 1);
		final TerminalPattern right = new TerminalPattern(pattern);
		return right;
	}

	@Override
	public ISPBranch constructRight2Left(final TerminalPattern arg0, final ITransformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final ISPBranch left, final TerminalPattern right, final ITransformer arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPBranch left, final TerminalPattern right, final ITransformer arg2) {
		// TODO Auto-generated method stub

	}

}
