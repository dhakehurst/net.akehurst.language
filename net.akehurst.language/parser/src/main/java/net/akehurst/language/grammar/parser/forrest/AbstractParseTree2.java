package net.akehurst.language.grammar.parser.forrest;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parse.tree.Node;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;

abstract
public class AbstractParseTree2 implements IParseTree {

	public AbstractParseTree2(Node root, int nextItemIndex) {
		this.root = root;
		this.nextItemIndex = nextItemIndex;
		this.identifier = new NodeIdentifier(root, nextItemIndex);
	}

	public abstract RuntimeRule getNextExpectedItem();
	
	Node root;
	@Override
	public Node getRoot() {
		return this.root;
	}
	int nextItemIndex;
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	NodeIdentifier identifier;

	public NodeIdentifier getIdentifier() {
		return this.identifier;
	}

	public boolean getIsSkip() {
		return this.getRoot().getIsSkip();
	}
	
	public RuntimeRule getRuntimeRule() {
		return this.getRoot().getRuntimeRule();
	}
	
	public boolean getHasPotential(RuntimeRuleSet runtimeRuleSet) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String toString() {
		return this.identifier + this.getRoot().getName();
	}
	
}
