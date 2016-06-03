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

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class ChoiceMultiple2RuntimeRuleItem extends AbstractChoice2RuntimeRuleItem<ChoiceSimple> {

	@Override
	public boolean isValidForLeft2Right(ChoiceSimple left) {
		return (1 < left.getAlternative().size());
	}

	@Override
	public RuntimeRuleItem constructLeft2Right(ChoiceSimple left, Transformer transformer) {
		Converter converter = (Converter) transformer;
		int maxRuleRumber = converter.getFactory().getRuntimeRuleSet().getTotalRuleNumber();
		RuntimeRuleItem right = converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.CHOICE);
		return right;
	}

	@Override
	public void configureLeft2Right(ChoiceSimple left, RuntimeRuleItem right, Transformer transformer) {
		try {
			List<RuntimeRule> rrAlternatives = new ArrayList<>();

			for (Concatenation concat : left.getAlternative()) {

				if (concat.getItem().size() > 1) {
					// create a new virtual rule to contain the concatenation
					// items
					RuntimeRule rr = transformer.transformLeft2Right(
							(Class<? extends Relation<Concatenation, RuntimeRule>>) (Class<?>) Concatenation2RuntimeRule.class,
							concat);
					rrAlternatives.add(rr);
				} else {
					RuntimeRule rr = transformer.transformLeft2Right(
							(Class<? extends Relation<ConcatenationItem, RuntimeRule>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class,
							concat.getItem().get(0));
					rrAlternatives.add(rr);
				}
			}

			RuntimeRule[] items = rrAlternatives.toArray(new RuntimeRule[rrAlternatives.size()]);
			right.setItems(items);

		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(ChoiceSimple arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public ChoiceSimple constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
