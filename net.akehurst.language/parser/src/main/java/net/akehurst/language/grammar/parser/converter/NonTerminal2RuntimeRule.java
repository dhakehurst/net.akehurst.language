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
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class NonTerminal2RuntimeRule extends AbstractConcatinationItem2RuntimeRule<NonTerminal> {

	@Override
	public boolean isValidForLeft2Right(final NonTerminal arg0) {
		return true;
	}

	@Override
	public boolean isAMatch(final NonTerminal left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(final NonTerminal left, final ITransformer transformer) throws RuleNotFoundException, TransformException {
		final Converter converter = (Converter) transformer;
		RuntimeRule right = null;

		try {
			right = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getReferencedRule());
		} catch (final net.akehurst.language.core.grammar.RuleNotFoundException e) {
			throw new TransformException(e.getMessage(), e);
		}
		// right = converter.getFactory().createRuntimeRule(left.getReferencedRule(), RuntimeRuleKind.NON_TERMINAL);

		return right;
	}

	@Override
	public void updateLeft2Right(final NonTerminal arg0, final RuntimeRule arg1, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateRight2Left(final NonTerminal arg0, final RuntimeRule arg1, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public NonTerminal constructRight2Left(final RuntimeRule arg0, final ITransformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(final RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
