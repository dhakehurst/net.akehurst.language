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
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2PriorityChoice extends AbstractRhsNode2RuleItem<ChoiceSimple> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "simpleChoice".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(ChoiceSimple right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ChoiceSimple constructLeft2Right(INode left, Transformer transformer) {
		try {
			List<? extends INode> allLeft = ((IBranch) left).getNonSkipChildren();
			List<? extends Concatenation> allRight;

			List<INode> concatenationNodes = new ArrayList<>();
			for (INode n: allLeft) {
				if ("concatenation".equals(n.getName())) {
					concatenationNodes.add(n);
				}
			}
			
			allRight = transformer.transformAllLeft2Right(
					(Class<Relation<INode, Concatenation>>) (Class<?>) Node2Concatenation.class, concatenationNodes);

			ChoiceSimple right = new ChoiceSimple(allRight.toArray(new Concatenation[0]));
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public INode constructRight2Left(ChoiceSimple right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, ChoiceSimple right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, ChoiceSimple right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
