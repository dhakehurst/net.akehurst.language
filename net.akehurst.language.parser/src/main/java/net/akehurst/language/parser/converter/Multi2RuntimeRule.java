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
import net.akehurst.language.ogl.semanticStructure.TangibleItem;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Multi2RuntimeRule extends AbstractConcatinationItem2RuntimeRule<Multi> {

	@Override
	public boolean isValidForLeft2Right(Multi arg0) {
		return true;
	}
	
	@Override
	public RuntimeRule constructLeft2Right(Multi left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Multi left, RuntimeRule right, Transformer transformer) {
		try {
			
			RuntimeRuleItem ruleItem = transformer.transformLeft2Right(Multi2RuntimeRuleItem.class, left);
			right.setRhs(ruleItem);

		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Multi arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Multi constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
