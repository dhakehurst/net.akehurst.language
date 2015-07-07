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



public class SeparatedList extends RuleItem {

	public SeparatedList(int min, int max, TerminalLiteral separator, TangibleItem concatination) {
		this.min = min;
		this.separator = separator;
		this.concatination = concatination;
	}
	
	@Override
	public void setOwningRule(Rule value) {
		this.owningRule = value;
		this.getConcatination().setOwningRule(value);
		this.getSeparator().setOwningRule(value);
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
	
	TangibleItem concatination;
	public TangibleItem getConcatination() {
		return this.concatination;
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
		result.addAll( this.getConcatination().findAllTerminal() );
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		result.addAll( this.getConcatination().findAllNonTerminal() );
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
		return "( "+this.getConcatination()+" / "+this.getSeparator()+" )"+(this.min==0?"*":"+");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof SeparatedList) {
			SeparatedList other = (SeparatedList)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
	
}
