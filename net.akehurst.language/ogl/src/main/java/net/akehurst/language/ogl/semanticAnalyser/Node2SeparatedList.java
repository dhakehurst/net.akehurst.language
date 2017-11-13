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

import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Node2SeparatedList extends AbstractNode2ConcatenationItem<SeparatedList> {

	@Override
	public String getNodeName() {
		return "separatedList";
	}

	@Override
	public boolean isValidForRight2Left(final SeparatedList right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final ISPBranch left, final SeparatedList right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SeparatedList constructLeft2Right(final ISPBranch left, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final ISPNode itemNode = left.getChild(1);

		final TangibleItem item = transformer.transformLeft2Right((Class<IBinaryRule<ISPNode, TangibleItem>>) (Class<?>) Node2SimpleItem.class, itemNode);

		final ISPNode separatorNode = left.getChild(3);

		final TerminalLiteral separator = transformer.transformLeft2Right((Class<IBinaryRule<ISPNode, TerminalLiteral>>) (Class<?>) AbstractNode2Terminal.class,
				separatorNode);

		final ISPNode multiplicityNode = left.getChild(5);
		// TODO: this should really be done with transform rules!
		final String multiplicityString = ((ISPBranch) multiplicityNode).getChild(0).getName();
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

		final SeparatedList right = new SeparatedList(min, max, separator, item);
		return right;

	}

	@Override
	public ISPBranch constructRight2Left(final SeparatedList right, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final ISPBranch left, final SeparatedList right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final ISPBranch left, final SeparatedList right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		// TODO Auto-generated method stub

	}

}
