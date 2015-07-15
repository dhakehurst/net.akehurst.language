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

import java.util.Set;

public class Group extends SimpleItem {

	public Group(AbstractChoice choice) {
		this.choice = choice;
	}
	
	AbstractChoice choice;
	public AbstractChoice getChoice() {
		return this.choice;
	}
	
	@Override
	public String getName() {
		return "$group";
	}
	
	@Override
	public void setOwningRule(Rule value) {
		this.owningRule = value;
		this.getChoice().setOwningRule(value);
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
		Set<Terminal> result = this.choice.findAllTerminal();
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = this.choice.findAllNonTerminal();
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
		return "("+this.choice+")";
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Group) {
			Group other = (Group)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
