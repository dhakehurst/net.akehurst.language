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

import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.SimpleItem;
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Multi2RuntimeRuleItem implements Relation<Multi, RuntimeRuleItem> {

	@Override
	public boolean isValidForLeft2Right(Multi left) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(Multi left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		int maxRuleRumber = converter.getFactory().getRuntimeRuleSet().getTotalRuleNumber();
		RuntimeRuleItem right = new RuntimeRuleItem(RuntimeRuleItemKind.MULTI,maxRuleRumber);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Multi left, RuntimeRuleItem right, Transformer transformer) {
		SimpleItem ti = left.getItem();
		
		try {
			RuntimeRule rr = transformer.transformLeft2Right((Class<? extends Relation<SimpleItem, RuntimeRule>>)(Class<?>)AbstractConcatinationItem2RuntimeRule.class, ti);
			RuntimeRule[] items = new RuntimeRule[]{ rr };
			
			right.setItems(items);
			right.setMultiMin(left.getMin());
			right.setMultiMax(left.getMax());
		
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Multi arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Multi constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
