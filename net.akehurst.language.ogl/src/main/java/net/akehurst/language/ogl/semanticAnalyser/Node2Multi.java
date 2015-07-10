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
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.SimpleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2Multi extends AbstractNode2ConcatenationItem<Multi> {

	@Override
	public String getNodeName() {
		return "multi";
	}

	@Override
	public boolean isValidForRight2Left(Multi right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Multi constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode itemNode = ((IBranch) left).getChild(0);
			
			SimpleItem item = transformer.transformLeft2Right(
					(Class<Relation<INode, SimpleItem>>) (Class<?>) Node2SimpleItem.class, itemNode);
			
			int min = 0;
			int max = -1;
			
			Multi right = new Multi(min, max, item);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public INode constructRight2Left(Multi right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Multi right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, Multi right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
