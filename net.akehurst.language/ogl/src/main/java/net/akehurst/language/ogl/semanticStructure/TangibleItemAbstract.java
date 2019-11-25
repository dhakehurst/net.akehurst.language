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
package net.akehurst.language.ogl.semanticStructure;

import java.util.List;

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.NodeType;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.TangibleItem;

public abstract class TangibleItemAbstract extends SimpleItemAbstract implements TangibleItem {

	private List<Integer> index;

	public abstract NodeType getNodeType() throws GrammarRuleNotFoundException;

	@Override
	public List<Integer> getIndex() {
		return this.index;
	}

	@Override
	public RuleItem getSubItem(final int i) {
		// Terminals and NonTerminals do not have sub items
		return null;
	}

	@Override
	public void setOwningRule(final RuleDefault value, final List<Integer> index) {
		this.owningRule = value;
		this.index = index;
	}
}
