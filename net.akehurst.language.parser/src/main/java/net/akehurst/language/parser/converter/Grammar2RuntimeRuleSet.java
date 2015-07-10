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
import java.util.Set;

import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleSet;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Grammar2RuntimeRuleSet implements Relation<Grammar, RuntimeRuleSet> {

	@Override
	public boolean isValidForLeft2Right(Grammar arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleSet constructLeft2Right(Grammar left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		int totalRuleNumber = left.getAllRule().size() + left.getAllTerminal().size();
		RuntimeRuleSet right = converter.getFactory().createRuntimeRuleSet(totalRuleNumber);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Grammar left, RuntimeRuleSet right, Transformer transformer) {
		Converter converter = (Converter)transformer;
		List<Rule> rules = left.getAllRule();
		List<Terminal> terminals = Arrays.asList(left.getAllTerminal().toArray(new Terminal[0]));
		
		try {

			List<? extends RuntimeRule> runtimeRules = transformer.transformAllLeft2Right(Rule2RuntimeRule.class, rules);
			List<? extends RuntimeRule> runtimeRules2 = transformer.transformAllLeft2Right(Terminal2RuntimeRule.class, terminals);

			List<RuntimeRule> rr = new ArrayList<>();
			rr.add(converter.getFactory().getEmptyRule());
			rr.addAll(runtimeRules);
			rr.addAll(runtimeRules2);
			rr.addAll(converter.virtualRule_cache);
			
			right.setRuntimeRules(rr);
			
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Grammar arg0, RuntimeRuleSet arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Grammar constructRight2Left(RuntimeRuleSet arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleSet arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
