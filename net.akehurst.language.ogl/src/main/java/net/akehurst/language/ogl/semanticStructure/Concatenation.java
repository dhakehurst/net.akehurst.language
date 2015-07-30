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

public class Concatenation extends RuleItem {

	public Concatenation(ConcatenationItem... item) {
		if (item.length < 1) {
			throw new RuntimeException("A concatentation must have 1 or more items in it.");
		}
		this.item = Arrays.asList(item);
	}

	ArrayList<Integer> index;
	public ArrayList<Integer> getIndex() {
		return this.index;
	}
	public void setOwningRule(Rule value, ArrayList<Integer> index) {
		this.owningRule = value;
		this.index = index;
		int i=0;
		for(ConcatenationItem c: this.getItem()) {
			ArrayList<Integer> nextIndex = new ArrayList<>(index);
			nextIndex.add(i++);
			c.setOwningRule(value, nextIndex);
		}
	}
	
	List<ConcatenationItem> item;
	public List<ConcatenationItem> getItem() {
		return this.item;
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
//		result.addAll( this.getItem().get(0).findFirstTangibleItem() );
//		return result;
//	}
//	
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		for(ConcatenationItem ti: this.getItem()) {
			result.addAll( ti.findAllTerminal() );
		}
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		for(ConcatenationItem ti: this.getItem()) {
			result.addAll( ti.findAllNonTerminal() );
		}
		return result;
	}
	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		if (node instanceof IBranch) {
//			IBranch branch = (IBranch)node;
//			boolean isMatched = branch.getChildren().size() == this.getItem().size();
//			if (isMatched) {
//				for(int i=0; i < branch.getChildren().size(); ++i) {
//					INode cn = branch.getChildren().get(i);
//					ConcatinationItem item = this.getItem().get(i);
//					if ( ! item.isMatchedBy(cn) ) {
//						return false;
//					}
//				}
//			}
//			return isMatched;
//		}
//		return false;
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = "";
		for(RuleItem i : this.getItem()) {
			r += i.toString() + " ";
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Concatenation) {
			Concatenation other = (Concatenation)arg;
			return this.getOwningRule().equals(other.getOwningRule()) && this.index.equals(other.index);
		} else {
			return false;
		}
	}
}
