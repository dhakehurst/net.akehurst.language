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


import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.transform.binary.AbstractTransformer;
import net.akehurst.transform.binary.Relation;

public class Converter extends AbstractTransformer {

	public Converter(Factory factory) {
		this.factory = factory;
		
		this.registerRule(Grammar2RuntimeRuleSet.class);
		this.registerRule(Rule2RuntimeRule.class);
		this.registerRule((Class<? extends Relation<?,?>>) (Class<?>) AbstractRuleItem2RuntimeRuleItem.class);
		this.registerRule(Choice2RuntimeRuleItem.class);
		this.registerRule(Concatenation2RuntimeRuleItem.class);
		this.registerRule(Multi2RuntimeRuleItem.class);
		this.registerRule(SeparatedList2RuntimeRuleItem.class);
		this.registerRule((Class<? extends Relation<?, ?>>) (Class<?>) AbstractTangibleItem2RuntimeRule.class);
		this.registerRule(NonTerminal2RuntimeRule.class);
		this.registerRule(Terminal2RuntimeRule.class);
	}
	
	Factory factory;
	public Factory getFactory() {
		return this.factory;
	}
	
}