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

import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.RuleItem;
import net.akehurst.language.ogl.semanticStructure.SkipRule;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Rule2RuntimeRule implements Relation<Rule, RuntimeRule>{

	@Override
	public boolean isValidForLeft2Right(Rule left) {
		return true;
	}
	
	@Override
	public RuntimeRule constructLeft2Right(Rule left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = converter.getFactory().createRuntimeRule(left, RuntimeRuleKind.NON_TERMINAL);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Rule left, RuntimeRule right, Transformer transformer) {

		try {
			RuntimeRuleItem rrItem = transformer.transformLeft2Right((Class<? extends Relation<AbstractChoice, RuntimeRuleItem>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class, left.getRhs());
			right.setRhs(rrItem);
			right.setIsSkipRule(left instanceof SkipRule);
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Rule arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Rule constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
