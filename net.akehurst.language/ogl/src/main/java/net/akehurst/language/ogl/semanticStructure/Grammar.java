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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.lexicalAnalyser.ITokenType;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.RuleNotFoundException;

public class Grammar {

	public Grammar(Namespace namespace, String name) {
		this.namespace = namespace;
		this.name = name;
		this.extends_ = new ArrayList<>();
		this.rule = new ArrayList<>();
	}
	
	Namespace namespace;
	public Namespace getNamespace() {
		return this.namespace;
	}
	
	String name;
	public String getName() {
		return this.name;
	}
	
	List<Grammar> extends_;
	public List<Grammar> getExtends() {
		return this.extends_;
	}
	public void setExtends(List<Grammar> value) {
		this.extends_ = value;
	}
	
	List<Rule> rule;
	public List<Rule> getRule() {
		return this.rule;
	}
	public void setRule(List<Rule> value) {
		this.rule = value;
	}
	
	public List<Rule> getAllRule() {
		ArrayList<Rule> allRules = new ArrayList<>();
		allRules.addAll(this.getRule());
		for(Grammar pg: this.getExtends()) {
			allRules.addAll(pg.getAllRule());
		}
		return allRules;
	}
	
	
	public Rule findAllRule(String ruleName) throws RuleNotFoundException {
		try {
			return this.findRule(ruleName);
		} catch (RuleNotFoundException e) {
		}
		for(Grammar pg: this.getExtends()) {
			try {
				return pg.findRule(ruleName);
			} catch (RuleNotFoundException e) {
			}	
		}
		throw new RuleNotFoundException(ruleName + " in Grammar("+this.getName()+").findAllRule");
	}
	
	public Rule findRule(String ruleName) throws RuleNotFoundException {
		ArrayList<Rule> rules = new ArrayList<>();
		for(Rule r : this.getRule()) {
			if (r.getName().equals(ruleName)) {
				rules.add(r);
			}
		}
		if (rules.isEmpty()) {
			throw new RuleNotFoundException(ruleName + " in Grammar("+this.getName()+").findRule");
		} else if (rules.size() == 1) {
			return  rules.get(0);
		} else {
			throw new RuleNotFoundException(ruleName + "too many rules in Grammar("+this.getName()+").findRule with name "+ruleName);
		}
	}
	
	Set<Terminal> allTerminal;
	public Set<Terminal> getAllTerminal() {
		if (null==this.allTerminal) {
			this.allTerminal = this.findAllTerminal();
		}
		return this.allTerminal;
	}
	
	Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		for (Rule rule : this.getAllRule()) {
			RuleItem ri = rule.getRhs();
			result.addAll(this.findAllTerminal(0, rule, ri ) );
		}
		return result;
	}
	
	Set<Terminal> findAllTerminal(final int totalItems, final Rule rule, RuleItem item) {
		Set<Terminal> result = new HashSet<>();
		if (item instanceof Terminal) {
			Terminal t = (Terminal) item;
			result.add(t);
		} else if (item instanceof Multi) {
			result.addAll( this.findAllTerminal( totalItems, rule, ((Multi)item).getItem() ) );
		} else if (item instanceof AbstractChoice) {
			for(Concatenation ti : ((AbstractChoice)item).getAlternative()) {
				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
			}
		} else if (item instanceof Concatenation) {
			for(ConcatenationItem ti : ((Concatenation)item).getItem()) {
				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
			}
		} else if (item instanceof SeparatedList) {
			result.addAll(this.findAllTerminal(totalItems, rule,((SeparatedList)item).getSeparator()));
			result.addAll( this.findAllTerminal(totalItems, rule, ((SeparatedList)item).getItem() ) );
		}
		return result;
	}

	public List<ITokenType> findTokenTypes() {
		List<ITokenType> result = new ArrayList<ITokenType>();
		for(Terminal t: this.getAllTerminal()){
			String pattern = t.getValue();
			String identity = t.getOwningRule().getName();
			TokenType tt = new TokenType(identity, pattern, (t instanceof TerminalPattern));
			if (!result.contains(tt)) {
				result.add(tt);
			}			
		}
		return result;
	}
	
	public INodeType findNodeType(String ruleName) throws RuleNotFoundException {
		for(Rule r: this.getAllRule()) {
			if (r.getName().equals(ruleName)) {
				return new RuleNodeType(r);
			}
		}
		throw new RuleNotFoundException(ruleName);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = this.getNamespace() + System.lineSeparator();
		String extendStr = "";
		if (! this.getExtends().isEmpty()) {
			extendStr += " extends ";
			for(Grammar pg : this.getExtends()) {
				extendStr += pg.getName() + ", ";
			}
		}
		r += "grammar "+this.getName() + extendStr +" {" + System.lineSeparator();
		for(Rule i : this.getRule()) {
			r += i.toString() + System.lineSeparator();
		}
		r+="}";
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Grammar) {
			Grammar other = (Grammar)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}

}
