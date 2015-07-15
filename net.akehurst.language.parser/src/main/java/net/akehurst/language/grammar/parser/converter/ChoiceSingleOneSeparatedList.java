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
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class ChoiceSingleOneSeparatedList extends AbstractChoice2RuntimeRuleItem<AbstractChoice> {

	@Override
	public boolean isValidForLeft2Right(AbstractChoice left) {
		return (1==left.getAlternative().size()) && 1==left.getAlternative().get(0).getItem().size() && left.getAlternative().get(0).getItem().get(0) instanceof SeparatedList;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(AbstractChoice left, Transformer transformer) {
		SeparatedList item = (SeparatedList)left.getAlternative().get(0).getItem().get(0);
		try {
			RuntimeRuleItem right = transformer.transformLeft2Right(SeparatedList2RuntimeRuleItem.class, item);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Cannot constrcut right item for AbstractChoice "+left);
		}
	}
	
	@Override
	public void configureLeft2Right(AbstractChoice left, RuntimeRuleItem right, Transformer transformer) {
		//in other cases, a multi is converted to a group and the empty rule is added then,
		// but not in this special case
		try {
			SeparatedList item = (SeparatedList)left.getAlternative().get(0).getItem().get(0);
			if (0 == item.getMin()) {
				Converter converter = (Converter)transformer;
				RuntimeRule ruleThatIsEmpty = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getOwningRule());
				RuntimeRule rhs = converter.createEmptyRuleFor(ruleThatIsEmpty);
			}
		} catch (RelationNotFoundException ex) {
			throw new RuntimeException("Cannont configure AbstractChoice");
		}
	}

	@Override
	public void configureRight2Left(AbstractChoice arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AbstractChoice constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
