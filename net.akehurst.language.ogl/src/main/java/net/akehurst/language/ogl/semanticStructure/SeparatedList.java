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
import java.util.Set;



public class SeparatedList extends ConcatenationItem {

	public SeparatedList(int min, int max, TerminalLiteral separator, TangibleItem item) {
		this.min = min;
		this.separator = separator;
		this.item = item;
	}
	
	ArrayList<Integer> index;
	public ArrayList<Integer> getIndex() {
		return this.index;
	}
	@Override
	public void setOwningRule(Rule value, ArrayList<Integer> index) {
		this.owningRule = value;
		this.index = index;
		ArrayList<Integer> nextIndex0 = new ArrayList<>(index);
		nextIndex0.add(0);
		ArrayList<Integer> nextIndex1 = new ArrayList<>(index);
		nextIndex1.add(1);
		this.getItem().setOwningRule(value, nextIndex0);
		this.getSeparator().setOwningRule(value, nextIndex1);
	}
	
	int min;
	public int getMin() {
		return this.min;
	}
	
	int max;
	public int getMax() {
		return this.max;
	}
	
	TerminalLiteral separator;
	public TerminalLiteral getSeparator() {
		return this.separator;
	}
	
	TangibleItem item;
	public TangibleItem getItem() {
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
//		result.addAll( this.getConcatination().findFirstTangibleItem() );
//		return result;
//	}
//	
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		result.add(this.getSeparator());
		result.addAll( this.getItem().findAllTerminal() );
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		result.addAll( this.getItem().findAllNonTerminal() );
		return result;
	}
	
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		// TODO Auto-generated method stub
//		return false;
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "( "+this.getItem()+" / "+this.getSeparator()+" )"+(this.min==0?"*":"+");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof SeparatedList) {
			SeparatedList other = (SeparatedList)arg;
			return this.getOwningRule().equals(other.getOwningRule()) && this.index.equals(other.index);
		} else {
			return false;
		}
	}
	
}
