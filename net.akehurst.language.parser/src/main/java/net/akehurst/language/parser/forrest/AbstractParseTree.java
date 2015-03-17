package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.CannotGrowTreeException;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleSet;

public abstract class AbstractParseTree implements IParseTree {

	public AbstractParseTree(Factory factory, Input input, Node root, AbstractParseTree stackedTree) {
		this.factory = factory;
		this.input = input;
		this.root = root;
		this.stackedTree = stackedTree;
	}

	Factory factory;
	Input input;
	Node root;
	
	AbstractParseTree stackedTree;
	public AbstractParseTree getStackedTree() {
		return this.stackedTree;
	}
	
	@Override
	public Node getRoot() {
		return this.root;
	}
	
	public boolean getIsEmpty() {
		return this.getRoot().getIsEmpty();
	}

	public AbstractParseTree peekTopStackedRoot() {
		return this.stackedTree;
	}

	abstract public boolean getIsComplete();

	public abstract boolean getCanGrow();

	abstract boolean getCanGraftBack();

	abstract boolean getCanGrowWidth();
	
	public abstract RuntimeRule getNextExpectedItem() ;

//	public abstract AbstractParseTree deepClone();

	public boolean getIsSkip() throws ParseTreeException {
		return this.getRoot().getNodeType() instanceof SkipNodeType;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	/**
	 * shift
	 * 
	 * look for next possible tokens for this tree - create buds if there are none then this tree can no longer grow for all possible buds clone a new tree with
	 * current root on the pushed stack, and new current root is the bud.
	 * 
	 * @throws RuleNotFoundException
	 * @throws ParseTreeException
	 * 
	 */
	public Set<AbstractParseTree> growWidth(RuntimeRule[] terminalRules, RuntimeRuleSet ruleSet) throws RuleNotFoundException, ParseTreeException {
		Set<AbstractParseTree> result = new HashSet<>();
		if ( this.getCanGrowWidth() ) { //don't grow width if its complete...cant graft back
			List<ParseTreeBud> buds = this.input.createNewBuds(terminalRules, this.getRoot().getEnd());
	// doing this causes non termination of parser
	//		ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
	//		buds.add(empty);
			for (ParseTreeBud bud : buds) {
				AbstractParseTree nt = this.pushStackNewRoot(bud.getRoot());
				ArrayList<AbstractParseTree> nts = nt.growHeight(ruleSet);
				//if (nts.isEmpty()) {
					result.add(nt);
				//} else {
					result.addAll(nts);
				//}
			}
		}
		return result;
	}

	AbstractParseTree pushStackNewRoot(Leaf n) {
		return new ParseTreeBud(this.factory, this.input, n, this);
	}
	
	/**
	 * reduce for this tree, see if the current root will expand (fit into the top stacked root)
	 * 
	 * 
	 * @throws ParseTreeException
	 * @throws RuleNotFoundException
	 * @throws CannotGraftBackException
	 * 
	 **/
	public AbstractParseTree tryGraftBack() throws RuleNotFoundException {//, CannotGraftBackException {
//		try {
//			if (this.getCanGraftBack()) {
				AbstractParseTree parent = this.peekTopStackedRoot();
//				if (parent.getCanGrow()) {
					return this.tryGraftInto(parent);
//				} else {
//					throw new CannotExtendTreeException("parent cannot grow");
//				}
//			} else {
//				throw new CannotExtendTreeException("tree not complete");
//			}
//		} catch (CannotExtendTreeException e) {
//			throw new CannotGraftBackException(e.getMessage(), e);
//		} catch (ParseTreeException e) { // StackEmpty
//			throw new CannotGraftBackException(e.getMessage(), null);
//		}
	}

	public ArrayList<AbstractParseTree> growHeight(RuntimeRuleSet ruleSet) {
		ArrayList<AbstractParseTree> result = new ArrayList<>();
		//result.add((AbstractParseTree) this);
		if (this.getIsComplete()) {
			RuntimeRule[] rules = ruleSet.getPossibleSuperRule(this.getRoot().getRuntimeRule());
			for (RuntimeRule rule : rules) {
				if (this.root.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
					result.add(this);
				}
				ArrayList<ParseTreeBranch> newTrees = this.grow(rule);
				for (ParseTreeBranch nt : newTrees) {
					if (nt.getIsComplete()) {
						ArrayList<AbstractParseTree> newTree2 = nt.growHeight(ruleSet);
						//if (newTree2.isEmpty()) {
							result.add(nt);
						//} else {
							result.addAll(newTree2);
						//}
					} else {
						result.add(nt);
					}
				}
			}
		} else {
			result.add(this);
		}
		return result;
	}

	AbstractParseTree tryGraftInto(AbstractParseTree parent) throws RuleNotFoundException {
		try {
//			if (parent instanceof ParseTreeBud) {
//				throw new CannotExtendTreeException("parent is a bud, cannot extend it");
//			} else {
				if (parent.getNextExpectedItem().getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber()) {
					return parent.extendWith(this.getRoot());
				} else if (this.getIsSkip()) {
					return parent.extendWith(this.getRoot());
				} else {
//					throw new CannotExtendTreeException("node is not next expected item or a skip node");
					return null;
				}
//			}
//		} catch (CannotExtendTreeException e) {
//			throw e;
//		} catch (NoNextExpectedItemException e) {
//			throw new CannotExtendTreeException(e.getMessage());
//		} catch (RuntimeException e) {
//			e.printStackTrace();
//			throw new CannotExtendTreeException(e.getMessage());
//		}
		} catch (ParseTreeException e) {
			throw new RuntimeException("Internal Error: Should not happen",e);
		}
	}

	public abstract ParseTreeBranch extendWith(INode extension) throws ParseTreeException;

	ArrayList<ParseTreeBranch> grow(RuntimeRule runtimeRule) {
		switch(runtimeRule.getRhs().getKind()) {
		case CHOICE:
			return this.growChoice(runtimeRule);
		case CONCATENATION:
			return this.growConcatenation(runtimeRule);
		case MULTI:
			return this.growMulti(runtimeRule);
		case SEPARATED_LIST:
			return this.growSeparatedList(runtimeRule);
		default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	ParseTreeBranch growMe(RuntimeRule target) {
		//INodeType nodeType = target.getOwningRule().getNodeType();
		List<INode> children = Arrays.asList(new INode[] {this.getRoot()});
		Branch newBranch = this.factory.createBranch(target, children);
		ParseTreeBranch newTree = new ParseTreeBranch(this.factory, this.input, newBranch, this.stackedTree, target, 1);
		return newTree;
	}
	
	ArrayList<ParseTreeBranch> growConcatenation(RuntimeRule target) {

		ArrayList<ParseTreeBranch> result = new ArrayList<>();
		if (0 == target.getRhs().getItems().length) {
			//do nothing
		} else if (target.getRhsItem(0).getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber() ) {
			ParseTreeBranch newTree = this.growMe(target);
			result.add(newTree);
		} else {
			//do nothing
		}
		return result;

	}

	ArrayList<ParseTreeBranch> growChoice(RuntimeRule target) {
		ArrayList<ParseTreeBranch> result = new ArrayList<>();
		for (RuntimeRule alt : target.getRhs().getItems()) {
			if (this.getRoot().getRuntimeRule().getRuleNumber() == alt.getRuleNumber() ) {
				ParseTreeBranch newTree = this.growMe(target);
				result.add(newTree);
			}
		}
		return result;
	}

	ArrayList<ParseTreeBranch> growMulti(RuntimeRule target) {
		try {
			ArrayList<ParseTreeBranch> result = new ArrayList<>();
			if (target.getRhsItem(0).getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber() 
			|| (0 == target.getRhs().getMultiMin() && this.getRoot() instanceof Leaf) ) {
				
				//need to create a 'complete' version and a 'non-complete' version of the tree
				ParseTreeBranch newTree = this.growMe(target);
				ParseTreeBranch ntB = (ParseTreeBranch)newTree;
				ParseTreeBranch newTree2 = new ParseTreeBranch(this.factory, ntB.input, (Branch)ntB.root, ntB.stackedTree, ntB.rule, ntB.nextItemIndex);
				newTree2.complete = false;
				result.add(newTree);
				//result.add(newTree2);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	ArrayList<ParseTreeBranch> growSeparatedList(RuntimeRule target) {
		try {
			ArrayList<ParseTreeBranch> result = new ArrayList<>();
			if (target.getRhsItem(0).getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber() ) {
				ParseTreeBranch newTree = this.growMe(target);
//				ParseTreeBranch ntB = (ParseTreeBranch)newTree;
//				ParseTreeBranch newTree2 = new ParseTreeBranch(this.factory, ntB.input, (Branch)ntB.root, ntB.stackedTree, ntB.rule, ntB.nextItemIndex);
//				newTree2.complete = false;
				result.add(newTree);
				//result.add(newTree2);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

}
