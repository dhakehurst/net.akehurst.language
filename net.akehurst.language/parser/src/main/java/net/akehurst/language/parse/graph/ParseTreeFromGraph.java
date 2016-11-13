package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parse.tree.Branch;

public class ParseTreeFromGraph implements IParseTree {

	public ParseTreeFromGraph(GraphNodeRoot gr) {
		this.gr = gr;
	}
	
	GraphNodeRoot gr;
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public INode getRoot() {
		return new Branch(gr.goalRule, createChildNodes(gr.children));
	}

	INode[] createChildNodes(List<IGraphNode> children) {
		ArrayList<INode> nodes = new ArrayList<>();
		for(IGraphNode gn:children){
			if (gn.getIsLeaf()) {
				INode l = ((GraphNodeLeaf)gn).leaf;
				nodes.add(l);
			} else {
				INode n = new Branch(gn.getRuntimeRule(), createChildNodes(gn.getChildren()));
				nodes.add(n);
			}
		}
		return nodes.toArray(new INode[nodes.size()]);
	}
	
	@Override
	public boolean getIsComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getCanGrowWidth() {
		// TODO Auto-generated method stub
		return false;
	}

}
