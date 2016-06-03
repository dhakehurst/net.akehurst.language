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
import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.SimpleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2Group extends AbstractNode2TangibleItem<Group> {

	@Override
	public String getNodeName() {
		return "group";
	}
	
	@Override
	public boolean isValidForRight2Left(Group right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Group constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode choiceNode = ((IBranch)left).getChild(1);
			AbstractChoice choice = transformer.transformLeft2Right((Class<Relation<INode, AbstractChoice>>) (Class<?>)AbstractNode2Choice.class, ((IBranch)choiceNode).getChild(0));
			Group right = new Group(choice);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to construct NonTerminal", e);
		}
	}

	@Override
	public INode constructRight2Left(Group right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Group right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, Group right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
