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

import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.RuleNotFoundException;

public class NonTerminal extends TangibleItem {

	public NonTerminal(String referencedRuleName) {
		this.referencedRuleName = referencedRuleName;
	}
	
	String referencedRuleName;
	Rule referencedRule;
	public Rule getReferencedRule() throws RuleNotFoundException {
		if (null == this.referencedRule) {
			this.referencedRule = this.getOwningRule().getGrammar().findAllRule(this.referencedRuleName);
		}
		return this.referencedRule;
	}
	
	@Override
	public String getName() {
		try {
			return this.getNodeType().getIdentity().asPrimitive();
		} catch (RuleNotFoundException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
	}

	INodeType nodeType;
	public INodeType getNodeType() throws RuleNotFoundException {
		return new RuleNodeType(this.getReferencedRule());
	}

	
	@Override
	public <T, E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E {
		return visitor.visit(this, arg);
	}
	
//	public Set<TangibleItem> findFirstTangibleItem() {
//		Set<TangibleItem> result = new HashSet<>();
//		result.add( this );
//		return result;
//	}
//	
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		result.add(this);
		return result;
	}
	
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		return node.getNodeType().equals(this.getNodeType());
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.referencedRuleName;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof NonTerminal) {
			NonTerminal other = (NonTerminal)arg;
			return this.referencedRuleName.equals(other.referencedRuleName) && this.index.equals(other.index) && this.getOwningRule().equals(other.getOwningRule());
		} else {
			return false;
		}
	}
}
