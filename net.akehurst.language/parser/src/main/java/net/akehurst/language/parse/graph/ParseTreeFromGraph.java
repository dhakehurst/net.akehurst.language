package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parse.tree.Branch;

public class ParseTreeFromGraph implements IParseTree {

	public ParseTreeFromGraph(IGraphNode gr) {
		this.root = (IBranch)gr;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	IBranch root;

	@Override
	public INode getRoot() {
		return this.root;
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
