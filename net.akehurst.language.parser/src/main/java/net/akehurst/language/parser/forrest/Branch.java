package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.parser.ToStringVisitor;

public class Branch extends Node implements IBranch {

	public Branch(Factory factory, int nodeTypeNumber, INodeType nodeType, List<INode> children) {
		super(factory, nodeTypeNumber, nodeType);
		this.children = children;
		this.start = this.children.get(0).getStart();
		this.length = 0;
		for(INode n: this.children) {
			this.length += n.getMatchedTextLength();
		}
		this.hashCode_cache = this.nodeTypeNumber ^ this.start ^ this.length;
	}
	
	
	List<INode> children;
	int length;
	int start;
	
	@Override
	public boolean getIsEmpty() {
		boolean res = true;
		for(INode n: this.children) {
			res &= n.getIsEmpty();
		}
		return res;
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
		return this.children;
	}
	
	@Override
	public IBranch addChild(INode newChild) {
		List<INode> newChildren = new ArrayList<>();
		newChildren.addAll(this.getChildren());
		newChildren.add(newChild);
		IBranch newBranch = this.factory.createBranch(this.nodeType, newChildren);
		return newBranch;
	}
	
	@Override
	public Branch deepClone() {
		List<INode> clonedChildren = new ArrayList<>();
		for(INode n:this.getChildren()) {
			clonedChildren.add( n.deepClone() );
		}
		IBranch clone = this.factory.createBranch(this.nodeType, clonedChildren);
		return (Branch)clone;
	}
	
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
		if (this.nodeTypeNumber != other.nodeTypeNumber) {
			return false;
		}
		if (this.start!=other.start || this.length!=other.length) {
			return false;
		}
		return this.children.equals(other.children);
	}
	
}
