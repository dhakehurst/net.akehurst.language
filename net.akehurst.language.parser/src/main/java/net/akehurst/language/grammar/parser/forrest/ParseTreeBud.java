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
package net.akehurst.language.grammar.parser.forrest;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class ParseTreeBud extends AbstractParseTree {

	ParseTreeBud(ForrestFactory factory, Leaf root, AbstractParseTree stackedTree) {
		super(factory, root, stackedTree);
	}

	@Override
	public boolean getCanGrow() {
		if (this.getIsStacked()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean getIsComplete() {
		return true;
	}

	@Override
	public boolean getCanGraftBack() {
		return this.getIsStacked();
	}
	
	@Override
	public boolean getCanGrowWidth() {
		return false;
	}
	
	@Override
	public Leaf getRoot() {
		return (Leaf) super.getRoot();
	}

	@Override
	public RuntimeRule getNextExpectedItem() {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	public ParseTreeBranch extendWith(INode extension)  {
		throw new RuntimeException("Should not happen, cannot extend a bud");
	}
	
//	public ParseTreeBud deepClone() {
//		Stack<AbstractParseTree> stack = new Stack<>();
//		stack.addAll(this.stackedRoots);
//		ParseTreeBud clone = new ParseTreeBud(this.input, this.getRoot(), stack);
//		return clone;
//	}

	// --- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}

	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}

	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof ParseTreeBud)) {
			return false;
		}
		ParseTreeBud other = (ParseTreeBud)arg;
		if ( this.getRoot().getStart() != other.getRoot().getStart() ) {
			return false;
		}
		if ( this.getRoot().getEnd() != other.getRoot().getEnd() ) {
			return false;
		}
		if ( this.getRoot().getRuntimeRule().getRuleNumber() != other.getRoot().getRuntimeRule().getRuleNumber() ) {
			return false;
		}
		if (!this.getIsStacked() && !other.getIsStacked()) {
			return true;
		}
		if (!this.peekTopStackedRoot().equals(other.peekTopStackedRoot())) {
			return false;
		}
		return true;

	}
}
