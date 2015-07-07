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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Concatenation2RuntimeRuleItem extends AbstractRuleItem2RuntimeRuleItem<Concatenation> {

	@Override
	public boolean isValidForLeft2Right(Concatenation arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(Concatenation left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		int maxRuleRumber = converter.getFactory().getRuntimeRuleSet().getTotalRuleNumber();
		return new RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, maxRuleRumber);
	}
	
	@Override
	public void configureLeft2Right(Concatenation left, RuntimeRuleItem right, Transformer transformer) {
		List<TangibleItem> tis = left.getItem();
		
		try {
			List<? extends RuntimeRule> rr = transformer.transformAllLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>)(Class<?>)AbstractTangibleItem2RuntimeRule.class, tis);
			if (rr.isEmpty()) {
				//add the EMPTY_RULE
				Converter converter = (Converter)transformer;
				rr = Arrays.asList( converter.getFactory().getEmptyRule() );
			}
			RuntimeRule[] items = rr.toArray(new RuntimeRule[rr.size()]);
			
			right.setItems(items);
		
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void configureRight2Left(Concatenation arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Concatenation constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
