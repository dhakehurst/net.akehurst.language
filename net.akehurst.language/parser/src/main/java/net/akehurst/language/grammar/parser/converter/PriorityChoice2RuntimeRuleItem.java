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
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.ChoicePriority;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.ConcatenationItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class PriorityChoice2RuntimeRuleItem extends AbstractChoice2RuntimeRuleItem<ChoicePriority> {

	@Override
	public boolean isValidForLeft2Right(ChoicePriority left) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(ChoicePriority left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		int maxRuleRumber = converter.getFactory().getRuntimeRuleSet().getTotalRuleNumber();
		RuntimeRuleItem right = converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.CHOICE);
		return right;
	}
	
	@Override
	public void configureLeft2Right(ChoicePriority left, RuntimeRuleItem right, Transformer transformer) {
		try {
			RuntimeRule ruleThatIsEmpty = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getOwningRule());
			Converter converter = (Converter) transformer;
			RuntimeRule rhs = converter.createEmptyRuleFor(ruleThatIsEmpty);
			List<RuntimeRule> rrAlternatives = Arrays.asList(rhs);
	
			RuntimeRule[] items = rrAlternatives.toArray(new RuntimeRule[rrAlternatives.size()]);
			right.setItems(items);
		} catch (RelationNotFoundException ex) {
			throw new RuntimeException("Cannot configure ChoicePriority");
		}
	}

	@Override
	public void configureRight2Left(ChoicePriority arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ChoicePriority constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
