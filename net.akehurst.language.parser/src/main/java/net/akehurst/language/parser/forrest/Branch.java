package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.parser.ToStringVisitor;

public class Branch implements IBranch {

	public Branch(INodeType nodeType, List<INode> children) {
		this.nodeType = nodeType;
		this.children = children;
		this.length = 0;
		for(INode n: this.children) {
			this.length += n.getLength();
		}
	}
	
	INodeType nodeType;
	List<INode> children;
	int length;
	
	@Override
	public INodeType getNodeType() throws ParseTreeException {
		return this.nodeType;
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
	public int getLength() {
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
		IBranch newBranch = new Branch(this.nodeType, newChildren);
		return newBranch;
	}
	
	@Override
	public Branch deepClone() {
		List<INode> clonedChildren = new ArrayList<>();
		for(INode n:this.getChildren()) {
			clonedChildren.add( n.deepClone() );
		}
		Branch clone = new Branch(this.nodeType, clonedChildren);
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Branch) {
			Branch other = (Branch)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
	
}
