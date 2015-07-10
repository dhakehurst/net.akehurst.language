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
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2NonTerminal extends AbstractNode2TangibleItem<NonTerminal> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "nonTerminal".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(NonTerminal right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NonTerminal constructLeft2Right(INode left, Transformer transformer) {
		try {
			String referencedRuleName = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch)left).getChild(0));
			NonTerminal right = new NonTerminal(referencedRuleName);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to copnstruct TangibleItem", e);
		}
	}

	@Override
	public INode constructRight2Left(NonTerminal right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, NonTerminal right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, NonTerminal right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
