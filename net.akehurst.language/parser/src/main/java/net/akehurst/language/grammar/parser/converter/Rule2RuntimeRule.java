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
import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.SkipRule;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Rule2RuntimeRule implements IBinaryRule<Rule, RuntimeRule> {

	@Override
	public boolean isValidForLeft2Right(final Rule left) {
		return true;
	}

	@Override
	public boolean isAMatch(final Rule left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(final Rule left, final ITransformer transformer) {
		final Converter converter = (Converter) transformer;
		final RuntimeRule right = converter.getFactory().createRuntimeRule(left);
		return right;
	}

	@Override
	public void updateLeft2Right(final Rule left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final RuntimeRuleItem rrItem = transformer.transformLeft2Right(
				(Class<? extends IBinaryRule<AbstractChoice, RuntimeRuleItem>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class, left.getRhs());
		right.setRhs(rrItem);
		right.setIsSkipRule(left instanceof SkipRule);

	}

	@Override
	public void updateRight2Left(final Rule arg0, final RuntimeRule arg1, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public Rule constructRight2Left(final RuntimeRule arg0, final ITransformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(final RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
