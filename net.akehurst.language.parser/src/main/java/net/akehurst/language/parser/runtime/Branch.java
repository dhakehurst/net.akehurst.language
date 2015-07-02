package net.akehurst.language.parser.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.parser.ToStringVisitor;

public class Branch extends Node implements IBranch {

	public Branch(final RuntimeRule runtimeRule, final INode[] children) {
		super(runtimeRule);
		this.children = children;
		this.start = this.children[0].getStart();
		this.length = 0;
		this.isEmpty = true;
		for(INode n: this.children) {
			this.isEmpty &= n.getIsEmpty();
			this.length += n.getMatchedTextLength();
		}
		this.hashCode_cache = this.runtimeRule.getRuleNumber() ^ this.start ^ this.length;
	}
	
	public INode[] children;
	int length;
	int start;
	
	boolean isEmpty;
	@Override
	public boolean getIsEmpty() {
		return isEmpty;
	}
	
	@Override
	public String getName() {
		try {
			return this.getNodeType().getIdentity().asPrimitive();
		} catch (ParseTreeException e) {
			throw new RuntimeException("", e);
		}
	}

	@Override
	public int getStart() {
		return this.start;
	}
	
	@Override
	public int getEnd() {
		return this.start + this.length;
	}
	
	@Override
	public int getMatchedTextLength() {
		return this.length;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public List<INode> getChildren() {
		return Arrays.asList(this.children);
	}

	@Override
	public INode getChild(int index) {
		List<INode> children = this.getChildren();

		//get first non skip child
		int child=0;
		INode n = children.get(child);
		while(n.getIsSkip()) {
			++child;
			n = children.get(child);
		}
		
		int count=0;

		while(count < index) {
			++child;
			n = children.get(child);
			while(n.getIsSkip()) {
				++child;
				n = children.get(child);
			}
			++count;
		}

		return n;
	}
	
//	@Override
//	public Branch deepClone() {
//		INode[] clonedChildren = Arrays.copyOf(this.children, this.children.length);
//		IBranch clone = this.factory.createBranch(this.getRuntimeRule(), clonedChildren);
//		return (Branch)clone;
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
	
	int hashCode_cache;
	@Override
	public int hashCode() {
		return this.hashCode_cache;
	}
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof Branch)) {
			return false;
		}
		Branch other = (Branch)arg;
		if (this.runtimeRule.getRuleNumber() != other.runtimeRule.getRuleNumber()) {
			return false;
		}
		if (this.start!=other.start || this.length!=other.length) {
			return false;
		}
		return Arrays.equals(this.children,other.children);
	}
	
}
