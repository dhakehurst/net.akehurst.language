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
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class TerminalPatternNode2Terminal extends AbstractNode2Terminal<TerminalPattern> {

	@Override
	public String getNodeName() {
		return "PATTERN";
	}

	@Override
	public boolean isValidForRight2Left(TerminalPattern right) {
		return true;
	}

	@Override
	public TerminalPattern constructLeft2Right(INode left, Transformer transformer) {
		INode child = ((IBranch) left).getChildren().get(0);
		ILeaf leaf = (ILeaf) child;
		String text = leaf.getMatchedText();
		String pattern = text.substring(1, text.length() - 1);
		TerminalPattern right = new TerminalPattern(pattern);
		return right;
	}

	@Override
	public INode constructRight2Left(TerminalPattern arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, TerminalPattern right, Transformer arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, TerminalPattern right, Transformer arg2) {
		// TODO Auto-generated method stub

	}

}