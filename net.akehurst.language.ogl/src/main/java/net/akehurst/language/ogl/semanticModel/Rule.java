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
package net.akehurst.language.ogl.semanticModel;

import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.core.parser.INodeType;

public class Rule {

	public Rule(Grammar grammar, String name) {
		this.grammar = grammar;
		this.name = name;
	}

	
	Grammar grammar;
	public Grammar getGrammar() {
		return this.grammar;
	}
	
	String name;
	public String getName(){
		return name;
	}

	RuleItem rhs;
	public RuleItem getRhs(){
		return rhs;
	}
	public void setRhs(RuleItem value) {
		this.rhs = value;
		this.rhs.setOwningRule(this);
	}
	
	public Set<Terminal> findAllSubTerminal() throws RuleNotFoundException {
		Set<Terminal> result = new HashSet<>();
		result.addAll(this.getRhs().findAllTerminal());
		for(Rule r: this.findAllSubRule()) {
			result.addAll(r.getRhs().findAllTerminal());
		}	
		return result;
	}
	
	public Set<NonTerminal> findAllSubNonTerminal() throws RuleNotFoundException {
		Set<NonTerminal> result = this.getRhs().findAllNonTerminal();
		Set<NonTerminal> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for(NonTerminal nt: oldResult) {
				Set<NonTerminal> newNts = nt.getReferencedRule().getRhs().findAllNonTerminal();
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}
	
	public Set<Rule> findAllSubRule() throws RuleNotFoundException {
		Set<Rule> result = new HashSet<>();
		for(NonTerminal nt: this.findAllSubNonTerminal()) {
			result.add( nt.getReferencedRule() );
		}
		return result;
	}
	
	public INodeType getNodeType() {
		return new RuleNodeType(this);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getName()+" = "+this.getRhs();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Rule) {
			Rule other = (Rule)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
