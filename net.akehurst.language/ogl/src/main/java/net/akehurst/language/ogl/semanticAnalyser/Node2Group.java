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
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2Group extends AbstractNode2TangibleItem<Group> {

	@Override
	public String getNodeName() {
		return "group";
	}

	@Override
	public boolean isValidForRight2Left(final Group right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final INode left, final Group right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Group constructLeft2Right(final INode left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final INode choiceNode = ((IBranch) left).getChild(1);
		final AbstractChoice choice = transformer.transformLeft2Right((Class<IBinaryRule<INode, AbstractChoice>>) (Class<?>) AbstractNode2Choice.class,
				((IBranch) choiceNode).getChild(0));
		final Group right = new Group(choice);
		return right;

	}

	@Override
	public INode constructRight2Left(final Group right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final INode left, final Group right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final INode left, final Group right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
