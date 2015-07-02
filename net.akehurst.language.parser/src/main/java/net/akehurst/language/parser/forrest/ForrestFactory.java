package net.akehurst.language.parser.forrest;

import java.util.List;
import java.util.Map;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.parser.runtime.Branch;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.Leaf;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class ForrestFactory {
	
	public ForrestFactory(Factory runtimeFactory, CharSequence text) {
		this.runtimeFactory = runtimeFactory;
		this.input = new Input(this, text);
	}
	
	Factory runtimeFactory;
	Input input;

	Map<BranchIdentifier, Branch> branch_cache;
	
	public List<ParseTreeBud> createNewBuds(RuntimeRule[] possibleNextTerminals, int pos) throws RuleNotFoundException {
		return this.input.createNewBuds(possibleNextTerminals, pos);
	}
	
	public ParseTreeBud fetchOrCreateBud(Leaf leaf, AbstractParseTree stackedTree) {
		ParseTreeBud bud = new ParseTreeBud(this, leaf, stackedTree);
		return bud;
	}
	
	public ParseTreeBranch fetchOrCreateBranch(RuntimeRule target, INode[] children, AbstractParseTree stackedTree, int nextItemIndex) {
//		BranchIdentifier bid = new BranchIdentifier(target, );
//		Branch newBranch = this.branch_cache.get(bid);
//		if (null==newBranch) {
//			newBranch = this.runtimeFactory.createBranch(target, children);
//		}
		Branch newBranch = this.runtimeFactory.createBranch(target, children);
		ParseTreeBranch newTree = new ParseTreeBranch(this, newBranch, stackedTree, target, nextItemIndex);

		return newTree;
	}
	
}
