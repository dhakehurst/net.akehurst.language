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
import java.util.List;

import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.AbstractTransformer;
import net.akehurst.transform.binary.Relation;

public class Converter extends AbstractTransformer {

	public Converter(Factory factory) {
		this.factory = factory;
		this.virtualRule_cache = new ArrayList<>();
		
		this.registerRule((Class<? extends Relation<?,?>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class);
		this.registerRule((Class<? extends Relation<?,?>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class);
		this.registerRule(AbstractSimpleItem2RuntimeRule.class);
		this.registerRule(ChoiceEmpty2RuntimeRuleItem.class);
		this.registerRule(ChoiceMultiple2RuntimeRuleItem.class);
		this.registerRule(ChoiceSingleConcatenation2RuntimeRuleItem.class);
		this.registerRule(ChoiceSingleOneMulti.class);
		this.registerRule(Concatenation2RuntimeRule.class);
//		this.registerRule(Concatenation2RuntimeRuleItem.class);
		this.registerRule(Grammar2RuntimeRuleSet.class);
		this.registerRule(Rule2RuntimeRule.class);
		this.registerRule(Multi2RuntimeRule.class);
		this.registerRule(Multi2RuntimeRuleItem.class);
		this.registerRule(NonTerminal2RuntimeRule.class);
		this.registerRule(PriorityChoice2RuntimeRuleItem.class);
//		this.registerRule(SeparatedList2RuntimeRuleItem.class);
		this.registerRule(Terminal2RuntimeRule.class);
	}
	
	Factory factory;
	public Factory getFactory() {
		return this.factory;
	}
	
	List<RuntimeRule> virtualRule_cache;
	RuntimeRule createVirtualRule(Concatenation concatenation) {
		Grammar grammar = concatenation.getOwningRule().getGrammar();
		RuleForGroup r = new RuleForGroup(grammar, new ChoiceSimple(concatenation));
		RuntimeRule rr = this.getFactory().createRuntimeRule(r, RuntimeRuleKind.NON_TERMINAL);
		this.virtualRule_cache.add(rr);
		return rr;
	}
	RuntimeRule createVirtualRule(Multi multi) {
		Grammar grammar = multi.getOwningRule().getGrammar();
		RuleForGroup r = new RuleForGroup(grammar, new ChoiceSimple(new Concatenation(multi)));
		RuntimeRule rr = this.getFactory().createRuntimeRule(r, RuntimeRuleKind.NON_TERMINAL);
		this.virtualRule_cache.add(rr);
		return rr;
	}
}
