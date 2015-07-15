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
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class SeparatedList2RuntimeRuleItem implements Relation<SeparatedList, RuntimeRuleItem> {

	@Override
	public boolean isValidForLeft2Right(SeparatedList left) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(SeparatedList left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRuleItem right = converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.SEPARATED_LIST);
		return right;
	}
	
	@Override
	public void configureLeft2Right(SeparatedList left, RuntimeRuleItem right, Transformer transformer) {
		TangibleItem ti = left.getItem();
		TerminalLiteral sep = left.getSeparator();
		
		try {
			RuntimeRule rr = transformer.transformLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>)(Class<?>)AbstractConcatinationItem2RuntimeRule.class, ti);
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
