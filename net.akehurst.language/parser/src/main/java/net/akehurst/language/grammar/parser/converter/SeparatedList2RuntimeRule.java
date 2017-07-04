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
package net.akehurst.language.grammar.parser.converter;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class SeparatedList2RuntimeRule extends AbstractConcatinationItem2RuntimeRule<SeparatedList> {

	@Override
	public boolean isValidForLeft2Right(final SeparatedList arg0) {
		return true;
	}

	@Override
	public boolean isAMatch(final SeparatedList left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(final SeparatedList left, final ITransformer transformer) {
		final Converter converter = (Converter) transformer;
		final RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}

	@Override
	public void updateLeft2Right(final SeparatedList left, final RuntimeRule right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {

		final RuntimeRuleItem ruleItem = transformer.transformLeft2Right(SeparatedList2RuntimeRuleItem.class, left);
		right.setRhs(ruleItem);

	}

	@Override
	public void updateRight2Left(final SeparatedList arg0, final RuntimeRule arg1, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public SeparatedList constructRight2Left(final RuntimeRule arg0, final ITransformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(final RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
