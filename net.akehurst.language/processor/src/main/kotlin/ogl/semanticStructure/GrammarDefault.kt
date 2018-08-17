/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.ogl.semanticStructure

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.grammar.NodeType
import net.akehurst.language.api.grammar.Rule
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.grammar.Terminal

data class GrammarDefault(override val namespace: Namespace, override val name: String) : Grammar {

	override val extends: MutableList<Grammar>

	override val rule: MutableList<Rule>

	init {
		this.extends = mutableListOf<Grammar>();
		this.rule = mutableListOf<Rule>();
		this.allTerminal_cache = null;
	}

	public var allRule: List<Rule> by lazy {
		this.extends.flatMap{it.allRule}.plus(this.rule)
	}

	public var allTerminal: Set<Terminal> by lazy {
		this.allRule.flatMap{it.rhs.allTerminal}
	}

	public override fun findAllRule(ruleName: String): Rule
	{
		val all = this.allRule.filter{it.name == ruleName}
		when {
			all.isEmpty() -> throw GrammarRuleNotFoundException ("${ruleName} in Grammar(${this.name}).findAllRule")
			all.size() > 1 -> throw GrammarRuleNotFoundException ("More than one rule named ${ruleName} in Grammar(${this.name}).findAllRule")
		}
	    return all.first() 
	}

	public override fun findAllTerminal(terminalPattern: String): Terminal
	{
		this.allTerminal.firstOrNull{it.value == terminalPattern} ?: throw GrammarRuleNotFoundException ("Terminal '${terminalPattern}' not found in Grammar(${this.name}).findAllTerminal")
	}

	Set<Terminal> findAllTerminal()
	{
		final Set < Terminal > result = new HashSet<>();
		for (final Rule rule : this.getAllRule()) {
		final RuleItem ri = rule.getRhs();
		result.addAll(this.findAllTerminal(0, rule, ri));
	}
		return result;
	}

	Set<Terminal> findAllTerminal(final int totalItems, final Rule rule, final RuleItem item)
	{
		final Set < Terminal > result = new HashSet<>();
		if (item instanceof TerminalAbstract) {
			final TerminalAbstract t = (TerminalAbstract) item;
			result.add(t);
		} else if (item instanceof MultiDefault) {
			result.addAll(this.findAllTerminal(totalItems, rule, ((MultiDefault) item).getItem()));
		} else if (item instanceof ChoiceAbstract) {
			for (final ConcatenationDefault ti : ((ChoiceAbstract) item).getAlternative()) {
				result.addAll(this.findAllTerminal(totalItems, rule, ti));
			}
		} else if (item instanceof ConcatenationDefault) {
			for (final ConcatenationItemAbstract ti : ((ConcatenationDefault) item).getItem()) {
				result.addAll(this.findAllTerminal(totalItems, rule, ti));
			}
		} else if (item instanceof SeparatedListDefault) {
			result.addAll(this.findAllTerminal(totalItems, rule, ((SeparatedListDefault) item).getSeparator()));
			result.addAll(this.findAllTerminal(totalItems, rule, ((SeparatedListDefault) item).getItem()));
		} else if (item instanceof GroupDefault) {
			result.addAll(this.findAllTerminal(totalItems, rule, ((GroupDefault) item).getChoice()));
		} else if (item instanceof NonTerminalDefault) {
			// add nothing
		} else {
			throw new RuntimeException ("Internal Error: Should never happen");
		}
		return result;
	}

	public List<ITokenType> findTokenTypes()
	{
		final List < ITokenType > result = new ArrayList<>();
		for (final Terminal t : this.getAllTerminal()) {
		final String pattern = t.getValue();
		final String identity = ((TerminalAbstract) t).getOwningRule().getName();
		final TokenType tt = new TokenType (identity, pattern, t instanceof TerminalPatternDefault);
		if (!result.contains(tt)) {
			result.add(tt);
		}
	}
		return result;
	}

	@Override
	public Set<NodeType> findAllNodeType()
	{
		final Set < NodeType > res = new HashSet<>();
		for (final Terminal t : this.getAllTerminal()) {
		try {
			res.add(((TerminalAbstract) t).getNodeType());
		} catch (final GrammarRuleNotFoundException e) {
			e.printStackTrace();
		}
	}

		for (final Rule r : this.getAllRule()) {
		res.add(r.getNodeType());
	}

		return res;
	}

	public NodeType findNodeType(final String ruleName) throws GrammarRuleNotFoundException
	{
		for (final Rule r : this.getAllRule()) {
		if (r.getName().equals(ruleName)) {
			return new RuleNodeTypeDefault (r);
		}
	}
		throw new GrammarRuleNotFoundException (ruleName);
	}


}
