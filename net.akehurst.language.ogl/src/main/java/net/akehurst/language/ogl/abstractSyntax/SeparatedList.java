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
package net.akehurst.language.ogl.abstractSyntax;


public class SeparatedList extends Item {

	public SeparatedList(int min, TerminalLiteral separator, Concatination concatination) {
		this.min = min;
		this.separator = separator;
		this.concatination = concatination;
	}
	
	int min;
	public int getMin() {
		return this.min;
	}
	
	TerminalLiteral separator;
	public TerminalLiteral getSeparator() {
		return this.separator;
	}
	
	Concatination concatination;
	public Concatination getConcatination() {
		return this.concatination;
	}
	
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
