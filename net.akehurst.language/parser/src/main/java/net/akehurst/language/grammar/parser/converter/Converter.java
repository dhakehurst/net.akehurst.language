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

import net.akehurst.language.grammar.parser.runtime.RuleForGroup;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.RuleItem;
import net.akehurst.language.ogl.semanticStructure.SeparatedList;
import net.akehurst.transform.binary.AbstractTransformer;
import net.akehurst.transform.binary.Relation;

public class Converter extends AbstractTransformer {

	public Converter(RuntimeRuleSetBuilder factory) {
		this.factory = factory;
		this.virtualRule_cache = new ArrayList<>();
		
		this.registerRule((Class<? extends Relation<?,?>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class);
		this.registerRule((Class<? extends Relation<?,?>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class);
		this.registerRule((Class<? extends Relation<?,?>>) (Class<?>) AbstractSimpleItem2RuntimeRule.class);
		this.registerRule(ChoiceSimpleEmpty2RuntimeRuleItem.class);
		this.registerRule(ChoiceSimpleMultiple2RuntimeRuleItem.class);
		this.registerRule(ChoiceSimpleSingleConcatenation2RuntimeRuleItem.class);
		this.registerRule(ChoiceAbstractSingleOneMulti.class);
		this.registerRule(ChoiceAbstractSingleOneSeparatedList.class);
		this.registerRule(ChoicePriorityEmpty2RuntimeRuleItem.class);
		this.registerRule(ChoicePriorityMultiple2RuntimeRuleItem.class);
		this.registerRule(ChoicePrioritySingleConcatenation2RuntimeRuleItem.class);
		this.registerRule(Concatenation2RuntimeRule.class);
		this.registerRule(Concatenation2RuntimeRuleItem.class);
		this.registerRule(Grammar2RuntimeRuleSet.class);
		this.registerRule(Group2RuntimeRule.class);
		this.registerRule(Multi2RuntimeRule.class);
		this.registerRule(Multi2RuntimeRuleItem.class);
		this.registerRule(NonTerminal2RuntimeRule.class);
		this.registerRule(PriorityChoice2RuntimeRuleItem.class);
		this.registerRule(Rule2RuntimeRule.class);
		this.registerRule(SeparatedList2RuntimeRule.class);
		this.registerRule(SeparatedList2RuntimeRuleItem.class);
		this.registerRule(Terminal2RuntimeRule.class);
	}
	
	RuntimeRuleSetBuilder factory;
	public RuntimeRuleSetBuilder getFactory() {
		return this.factory;
	}
	
	String createIndexString(RuleItem item) {
		String str = "";
		for(Integer i: item.getIndex()) {
			str+=i + ".";
		}
		str = str.substring(0,str.length()-1);
		return str;
	}
	
	List<RuntimeRule> virtualRule_cache;
	RuntimeRule createVirtualRule(Group group) {
		Grammar grammar = group.getOwningRule().getGrammar();
		String name = "$group."+group.getOwningRule().getName()+"$";
		RuleForGroup r = new RuleForGroup(grammar, name, group.getChoice());
		RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		return rr;
	}
	RuntimeRule createVirtualRule(Concatenation concatenation) {
		Grammar grammar = concatenation.getOwningRule().getGrammar();
		String name = "$group."+concatenation.getOwningRule().getName()+"$";
		RuleForGroup r = new RuleForGroup(grammar, name, new ChoiceSimple(concatenation));
		RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		return rr;
	}
	
	int multiNum;
	RuntimeRule createVirtualRule(Multi multi) {
		Grammar grammar = multi.getOwningRule().getGrammar();
		String name = "$"+multi.getOwningRule().getName()+"."+multi.getItem().getName()+".multi"+createIndexString(multi);//(multiNum++);
		RuleForGroup r = new RuleForGroup(grammar, name, new ChoiceSimple(new Concatenation(multi)));
		RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		
		if (0 == multi.getMin()) {
			RuntimeRule ruleThatIsEmpty = rr;
			RuntimeRule rhs = this.createEmptyRuleFor(ruleThatIsEmpty);
		}
		
		return rr;
	}
	int sepListNum;
	RuntimeRule createVirtualRule(SeparatedList sepList) {
		Grammar grammar = sepList.getOwningRule().getGrammar();
		String name = "$sepListG."+sepList.getOwningRule().getName()+(sepListNum++);
		RuleForGroup r = new RuleForGroup(grammar, name, new ChoiceSimple(new Concatenation(sepList)));
		RuntimeRule rr = this.getFactory().createRuntimeRule(r);
		this.virtualRule_cache.add(rr);
		if (0 == sepList.getMin()) {
			RuntimeRule ruleThatIsEmpty = rr;
			RuntimeRule rhs = this.createEmptyRuleFor(ruleThatIsEmpty);
		}
		return rr;
	}
	public RuntimeRule createEmptyRuleFor(RuntimeRule right) {
		RuntimeRule rr = this.getFactory().createEmptyRule(right);
		this.virtualRule_cache.add(rr);
		return rr;
	}

}
