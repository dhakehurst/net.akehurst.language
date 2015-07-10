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
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class ItemNode2TangibleItem extends AbstractSemanticAnalysisRelation<TangibleItem> {

	@Override
	public String getNodeName() {
		return "item";
	}

	@Override
	public TangibleItem constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode itemNode = ((IBranch) left).getChild(0);
			if ("singleItem".equals(itemNode.getName())) {
				INode tNode = ((IBranch) itemNode).getChild(0);
				TangibleItem right = transformer.transformLeft2Right(
						(Class<Relation<INode, TangibleItem>>) (Class<?>) AbstractNode2TangibleItem.class, tNode);
				return right;
			} else if ("multi".equals(itemNode.getName())) {
				TangibleItem right = transformer.transformLeft2Right(
						(Class<Relation<INode, TangibleItem>>) (Class<?>) AbstractNode2TangibleItem.class, tNode);
				return right;
			} 
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to construct TangibleItem", e);
		}
	}

	@Override
	public INode constructRight2Left(TangibleItem right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, TangibleItem right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, TangibleItem right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
