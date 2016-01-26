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

import java.util.Arrays;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class ParseTreeBranch extends AbstractParseTree {
	
	public ParseTreeBranch(ForrestFactory factory, Branch root, AbstractParseTree stackedTree, RuntimeRule rule, int nextItemIndex) {
		super(factory, root, stackedTree);
		this.rule = rule;
		this.nextItemIndex = nextItemIndex;
		this.canGrow = this.calculateCanGrow();
		this.complete = this.calculateIsComplete();
		this.canGrowWidth = this.calculateCanGrowWidth();
//		this.hashCode_cache = this.getRoot().hashCode();
	}
	
	RuntimeRule rule;
	int nextItemIndex;
	boolean canGrow;
	boolean complete;
	boolean canGrowWidth;
	
	@Override
	public boolean getCanGrow() {
		return this.canGrow;
	}
	
	@Override
	public boolean getIsComplete() {
		return this.complete;
	}
	
	@Override
	public boolean getCanGraftBack() {
		return this.getIsComplete() && this.getIsStacked();
	}
	
	@Override
	public boolean getCanGrowWidth() {
		return this.canGrowWidth;
	}
	
	@Override
	public Branch getRoot() {
		return (Branch)super.getRoot();
	}
	
	public ParseTreeBranch extendWith(INode extension) throws ParseTreeException {
		INode[] nc = this.addChild(this.getRoot(), extension);
//		Stack<AbstractParseTree> stack = new Stack<>();
//		stack.addAll(this.stackedRoots);
		if (extension.getIsSkip()) {// getNodeType() instanceof SkipNodeType) {
//			ParseTreeBranch newBranch = new ParseTreeBranch(this.factory, this.input, nb, this.stackedTree, this.rule, this.nextItemIndex);
			ParseTreeBranch newBranch = this.ffactory.fetchOrCreateBranch(this.rule, nc, this.stackedTree, this.nextItemIndex);
			return newBranch;			
		} else {
	//		ParseTreeBranch newBranch = new ParseTreeBranch(this.factory, this.input, nc, this.stackedTree, this.rule, this.nextItemIndex+1);
			ParseTreeBranch newBranch = this.ffactory.fetchOrCreateBranch(this.rule, nc, this.stackedTree, this.nextItemIndex+1);
			return newBranch;
		}
	}
	
	public INode[] addChild(Branch old, INode newChild) {
		INode[] newChildren = Arrays.copyOf(old.children, old.children.length+1);
		newChildren[old.children.length] = newChild;
		return newChildren;
	}
	
	@Override
	public RuntimeRule getNextExpectedItem() {
		switch(this.rule.getRhs().getKind()) {
		case CHOICE: {
			throw new RuntimeException("Internal Error: item is choice");
		}
		case CONCATENATION: {
			if (this.nextItemIndex >= this.rule.getRhs().getItems().length) {
				throw new RuntimeException("Internal Error: No NextExpectedItem");
			} else {
				return this.rule.getRhsItem(this.nextItemIndex);
			}
		}
		case MULTI: {
			return this.rule.getRhsItem(0);
		}
		case SEPARATED_LIST: {
			if ( (this.nextItemIndex % 2) == 1 ) {
				return this.rule.getSeparator();
			} else {
				return this.rule.getRhsItem(0);
			}
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	boolean calculateIsComplete() {
		switch(this.rule.getRhs().getKind()) {
		case CHOICE:
			return true;

		case CONCATENATION: {
			return this.rule.getRhs().getItems().length <= this.nextItemIndex;
		}
		case MULTI: {
			int size = this.nextItemIndex;
			return size >= this.rule.getRhs().getMultiMin();
		}
		case SEPARATED_LIST: {
			int size = this.nextItemIndex;
			return (size % 2) == 1;
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}
	boolean calculateCanGrow() {
		if (this.getIsStacked()) return true;
		return this.calculateCanGrowWidth();
	}
	
	boolean calculateCanGrowWidth() {
		boolean reachedEnd = this.getRoot().getMatchedTextLength() >= this.ffactory.input.getLength();
		if (reachedEnd)
			return false;
		if (this.complete && this.getIsEmpty()) {
			return false;
		}
		switch(this.rule.getRhs().getKind()) {
		case EMPTY : {
			return false;
		}
		case CHOICE: {
			return false;
		}
		case CONCATENATION: {
			if ( this.nextItemIndex < this.rule.getRhs().getItems().length ) {
				return true;
			} else {
				return false; //!reachedEnd;
			}
		}
		case MULTI: {
			int size = this.nextItemIndex;
			int max = this.rule.getRhs().getMultiMax();
			return -1==max || size < max;
		}
		case SEPARATED_LIST: {
			return true;
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}
	
//	public ParseTreeBranch deepClone() {
//		Stack<AbstractParseTree> stack = new Stack<>();
//		stack.addAll(this.stackedRoots);
//		ParseTreeBranch clone = new ParseTreeBranch(this.input, this.getRoot(), stack, this.rule, this.nextItemIndex);
//		return clone;
//	}
	
	//--- Object ---
	static ToStringVisitor v = new ToStringVisitor();
	String toString_cache;
	@Override
	public String toString() {
		if (null==this.toString_cache) {
			this.toString_cache = this.accept(v, "");
		}
		return this.toString_cache;
	}
	
//	int hashCode_cache;
//	@Override
//	public int hashCode() {
//		return hashCode_cache;
//	}
//	
//	@Override
//	public boolean equals(Object arg) {
//		if (!(arg instanceof ParseTreeBranch)) {
//			return false;
//		}
//		ParseTreeBranch other = (ParseTreeBranch)arg;
//		if ( this.getRoot().getStart() != other.getRoot().getStart() ) {
//			return false;
//		}
//		if ( this.getRoot().getEnd() != other.getRoot().getEnd() ) {
//			return false;
//		}
//		if ( this.getRoot().getRuntimeRule().getRuleNumber() != other.getRoot().getRuntimeRule().getRuleNumber() ) {
//			return false;
//		}
//		if (this.complete != other.complete) {
//			return false;
//		}
//		if (!this.getIsStacked() && !other.getIsStacked()) {
//			return this.getRoot().equals(other.getRoot());
//		}
//		if (!this.peekTopStackedRoot().equals(other.peekTopStackedRoot())) {
//			return false;
//		}
//		return this.getRoot().equals(other.getRoot());
//
//	}
}
