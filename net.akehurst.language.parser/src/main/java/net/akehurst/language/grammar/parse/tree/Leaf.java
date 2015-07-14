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
package net.akehurst.language.grammar.parse.tree;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.Input;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.ogl.semanticStructure.LeafNodeType;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;

public class Leaf extends Node implements ILeaf {

	Leaf(Input input, int start, int end, RuntimeRule terminalRule) {
		super(terminalRule);
		this.input = input;
		this.start = start;
		this.end = end;
		this.terminalRule = terminalRule;
	}
	
	Input input;
	Input getInput() {
		return this.input;
	}
	
	int start;
	int end;
	RuntimeRule terminalRule;

	@Override
	public boolean getIsEmpty() {
		return false;
	}
	
	@Override
	public INodeType getNodeType() throws ParseTreeException {
		try {
			return this.terminalRule.getTerminal().getNodeType();
		} catch (RuleNotFoundException e) {
			throw new ParseTreeException("Rule Not Found",e);
		}
	}

	@Override
	public String getName() {
		return this.terminalRule.getName();
	}

	@Override
	public int getStart() {
		return this.start;
	}
	
	@Override
	public int getEnd() {
		return this.end;
	}
	
	@Override
	public int getMatchedTextLength() {
		return end - start;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public String getMatchedText() {
		return this.input.get(this.start, this.end).toString();
	}
	
//	public Leaf deepClone() {
//		Leaf clone = new Leaf(this.input, this.start, this.end, this.terminalRule);
//		return clone;
//	}
	
	//--- Object ---
	static ToStringVisitor v = new ToStringVisitor();
	@Override
	public String toString() {
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.start ^ this.end;
	}
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof Leaf) ) {
			return false;
		}
		Leaf other = (Leaf)arg;
		if (this.start!=other.start || this.end!=other.end) {
			return false;
		}
		return this.terminalRule.getRuleNumber() == other.terminalRule.getRuleNumber();
	}

}
