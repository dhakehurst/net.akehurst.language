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

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2SeparatedList extends AbstractNode2ConcatenationItem<SeparatedList> {

	@Override
	public String getNodeName() {
		return "separatedList";
	}

	@Override
	public boolean isValidForRight2Left(SeparatedList right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SeparatedList constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode itemNode = ((IBranch) left).getChild(1);
			
			TangibleItem item = transformer.transformLeft2Right(
					(Class<Relation<INode, TangibleItem>>) (Class<?>) Node2SimpleItem.class, itemNode);
			
			INode separatorNode = ((IBranch) left).getChild(3);
			
			TerminalLiteral separator = transformer.transformLeft2Right(
					(Class<Relation<INode, TerminalLiteral>>) (Class<?>) AbstractNode2Terminal.class, separatorNode);
			
			INode multiplicityNode = ((IBranch) left).getChild(5);
			//TODO: this should really be done with transform rules!
			String multiplicityString = ((IBranch) multiplicityNode).getChild(0).getName();
			int min = -1;
			int max = -1;
			if ("*".equals(multiplicityString)) {
				min = 0;
				max = -1;
			} else if ("+".equals(multiplicityString)) {
				min = 1;
				max = -1;
			} else if ("?".equals(multiplicityString)) {
				min = 0;
				max = 1;
			}
			
			SeparatedList right = new SeparatedList(min, max, separator, item);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public INode constructRight2Left(SeparatedList right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, SeparatedList right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, SeparatedList right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
