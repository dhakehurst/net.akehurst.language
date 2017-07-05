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
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2Multi extends AbstractNode2ConcatenationItem<Multi> {

	@Override
	public String getNodeName() {
		return "multi";
	}

	@Override
	public boolean isValidForRight2Left(final Multi right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final IBranch left, final Multi right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Multi constructLeft2Right(final IBranch left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final INode itemNode = left.getChild(0);
		final INode multiplicityNode = left.getChild(1);

		final SimpleItem item = transformer.transformLeft2Right((Class<IBinaryRule<INode, SimpleItem>>) (Class<?>) Node2SimpleItem.class, itemNode);

		// TODO: this should really be done with transform rules!
		Multi right = null;
		final String multiplicityString = ((IBranch) multiplicityNode).getChild(0).getName();
		if ("*".equals(multiplicityString)) {
			final int min = 0;
			final int max = -1;
			right = new Multi(min, max, item);
		} else if ("+".equals(multiplicityString)) {
			final int min = 1;
			final int max = -1;
			right = new Multi(min, max, item);
		} else if ("?".equals(multiplicityString)) {
			final int min = 0;
			final int max = 1;
			right = new Multi(min, max, item);
		}
		return right;

	}

	@Override
	public IBranch constructRight2Left(final Multi right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final IBranch left, final Multi right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final IBranch left, final Multi right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
