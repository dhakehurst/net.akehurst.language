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
package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.Leaf;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class ParseTreeBud extends AbstractParseTree {

	ParseTreeBud(ForrestFactory factory, Leaf root, AbstractParseTree stackedTree) {
		super(factory, root, stackedTree);
	}

	@Override
	public boolean getCanGrow() {
		if (null!=this.stackedTree) {
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
		return this.stackedTree!=null;
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
		if (arg instanceof ParseTreeBud) {
			ParseTreeBud other = (ParseTreeBud) arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
