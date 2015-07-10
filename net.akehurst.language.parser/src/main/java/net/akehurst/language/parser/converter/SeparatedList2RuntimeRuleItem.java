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
package net.akehurst.language.parser.converter;

import java.util.List;

import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class SeparatedList2RuntimeRuleItem extends AbstractRuleItem2RuntimeRuleItem<SeparatedList> {

	@Override
	public boolean isValidForLeft2Right(SeparatedList arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(SeparatedList left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		int maxRuleRumber = converter.getFactory().getRuntimeRuleSet().getTotalRuleNumber();
		RuntimeRuleItem right = new RuntimeRuleItem(RuntimeRuleItemKind.SEPARATED_LIST,maxRuleRumber);
		return right;
	}
	
	@Override
	public void configureLeft2Right(SeparatedList left, RuntimeRuleItem right, Transformer transformer) {
		TangibleItem ti = left.getConcatination();
		Terminal sep = left.getSeparator();
		
		try {
			RuntimeRule rr = transformer.transformLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class, ti);
			RuntimeRule rrsep = transformer.transformLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class, sep);
			RuntimeRule[] items = new RuntimeRule[]{ rr, rrsep };
			
			right.setItems(items);
			right.setMultiMin(left.getMin());
			right.setMultiMax(left.getMax());
		
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(SeparatedList arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SeparatedList constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
