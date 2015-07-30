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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ChoicePriority extends AbstractChoice {
	
	public ChoicePriority(Concatenation... alternative) {
		this.alternative = Arrays.asList(alternative);
	}
	ArrayList<Integer> index;
	public ArrayList<Integer> getIndex() {
		return this.index;
	}
	public void setOwningRule(Rule value, ArrayList<Integer> index) {
		this.owningRule = value;
		this.index = index;
		int i=0;
		for(Concatenation c: this.getAlternative()) {
			ArrayList<Integer> nextIndex = new ArrayList<>(index);
			nextIndex.add(i++);
			c.setOwningRule(value, nextIndex);
		}
	}
	

	
//	@Override
//	public INodeType getNodeType() {
//		return new RuleNodeType(this.getOwningRule());
//	}
	
	@Override
	public <T, E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E {
		return visitor.visit(this, arg);
	}
	
//	public Set<TangibleItem> findFirstTangibleItem() {
//		Set<TangibleItem> result = new HashSet<>();
//		for(Concatination c : this.getAlternative()) {
//			Set<TangibleItem> ft = c.findFirstTangibleItem();
//			result.addAll(ft);
//		}		return result;
//	}
//	
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		for(Concatenation c : this.getAlternative()) {
			Set<Terminal> ft = c.findAllTerminal();
			result.addAll(ft);
		}
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		for(Concatenation c : this.getAlternative()) {
			Set<NonTerminal> ft = c.findAllNonTerminal();
			result.addAll(ft);
		}
		return result;
	}
	
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		for(Concatination c : this.getAlternative()) {
//			boolean isMatched = c.isMatchedBy(node);
//			if (isMatched) {
//				return true;
//			}
//		}
//		return false;
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = "";
		for(Concatenation a : this.getAlternative()) {
			r += a.toString() + " < ";
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof ChoicePriority) {
			ChoicePriority other = (ChoicePriority)arg;
			return this.getOwningRule().equals(other.getOwningRule()) && this.index.equals(other.index);
		} else {
			return false;
		}
	}
}
