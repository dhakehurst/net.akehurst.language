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

import java.util.Arrays;
import java.util.List;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Concatenation2RuntimeRuleItem implements IBinaryRule<Concatenation, RuntimeRuleItem> {

	@Override
	public boolean isValidForLeft2Right(final Concatenation arg0) {
		return true;
	}

	@Override
	public boolean isAMatch(final Concatenation left, final RuntimeRuleItem right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRuleItem constructLeft2Right(final Concatenation left, final ITransformer transformer) {
		final Converter converter = (Converter) transformer;
		return converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION);
	}

	@Override
	public void updateLeft2Right(final Concatenation left, final RuntimeRuleItem right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {
		final List<ConcatenationItem> tis = left.getItem();

		List<? extends RuntimeRule> rr = transformer.transformAllLeft2Right(
				(Class<? extends IBinaryRule<ConcatenationItem, RuntimeRule>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class, tis);
		if (rr.isEmpty()) {
			// add an EMPTY_RULE
			final RuntimeRule ruleThatIsEmpty = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getOwningRule());
			final Converter converter = (Converter) transformer;
			final RuntimeRule rhs = converter.createEmptyRuleFor(ruleThatIsEmpty);
			rr = Arrays.asList(rhs);
		}
		final RuntimeRule[] items = rr.toArray(new RuntimeRule[rr.size()]);

		right.setItems(items);

	}

	@Override
	public void updateRight2Left(final Concatenation arg0, final RuntimeRuleItem arg1, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public Concatenation constructRight2Left(final RuntimeRuleItem arg0, final ITransformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(final RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
