package net.akehurst.language.grammar.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parse.tree.Node;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.graphStructuredStack.IGraphStructuredStack;
import net.akehurst.language.graphStructuredStack.IGssNode;
import net.akehurst.language.graphStructuredStack.impl.GraphStructuredStack;

public class Forrest2 {

	public Forrest2(ForrestFactory2 ffactory, RuntimeRuleSet runtimeRuleSet) {
		this.ffactory = ffactory;
		this.runtimeRuleSet = runtimeRuleSet;
		this.gss = new GraphStructuredStack<>();

	}

	ForrestFactory2 ffactory;

	protected RuntimeRuleSet runtimeRuleSet;

	protected Forrest2 newForrest() {
		Forrest2 f2 = new Forrest2(this.ffactory, this.runtimeRuleSet);
		f2.gss = this.gss;
		return f2;
	}

	/**
	 * For debug purposes
	 */
	public Forrest2 shallowClone() {
		Forrest2 f2 = new Forrest2(this.ffactory, this.runtimeRuleSet);
		f2.gss = this.gss.shallowClone();
		return f2;
	}

	IGraphStructuredStack<NodeIdentifier, AbstractParseTree2> gss;

	public void newSeeds(List<AbstractParseTree2> newTrees) {
		for (AbstractParseTree2 t : newTrees) {
			gss.newBottom(t.getIdentifier(), t);
		}
	}

	public boolean getCanGrow() {
		boolean b = false;
		for (IGssNode<NodeIdentifier, AbstractParseTree2> n : this.gss.getTops()) {
			b = b || this.getCanGrow(n.getValue());
		}
		return b;
	}

	boolean getCanGrow(AbstractParseTree2 tree) {
		if (this.getIsStacked(tree))
			return true;
		return tree.getCanGrowWidth();
	}

	public Forrest2 grow() throws RuleNotFoundException, ParseTreeException {
		Forrest2 newForrest = this.newForrest();

		for (IGssNode<NodeIdentifier, AbstractParseTree2> gn : gss.getTops()) {
			ArrayList<AbstractParseTree2> newBranches = this.growTreeWidthAndHeight(gn.getValue());
			// newForrest.addAll(newBranches);
			int c = newBranches.size();
		}

		return newForrest;
	}

	public ArrayList<AbstractParseTree2> growTreeWidthAndHeight(AbstractParseTree2 tree) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree2> result = new ArrayList<AbstractParseTree2>();

		ArrayList<AbstractParseTree2> newSkipBranches = this.growWidthWithSkipRules(tree);
		if (!newSkipBranches.isEmpty()) {
			result.addAll(newSkipBranches);
		} else {
			if (tree.getIsSkip()) {
				ArrayList<AbstractParseTree2> nts = this.tryGraftBack(tree);
				for (AbstractParseTree2 nt : nts) {
					if (nt.getHasPotential(this.runtimeRuleSet)) {
						result.add(nt);
					} else {
						// drop it
					}
				}
			} else {
				if (tree.getIsComplete()) {
					ArrayList<AbstractParseTree2> nts = this.growHeight(tree);
					for (AbstractParseTree2 nt : nts) {
						if (nt.getHasPotential(this.runtimeRuleSet)) {
							result.add(nt);
						} else {
							// drop it
						}
					}
				}

				// reduce
				if (this.getCanGraftBack(tree)) {
					ArrayList<AbstractParseTree2> nts = this.tryGraftBack(tree);
					for (AbstractParseTree2 nt : nts) {
						if (nt.getHasPotential(this.runtimeRuleSet) || this.getIsGoal(nt)) {
							result.add(nt);
						} else {
							// drop it
						}
					}
				}

				// shift
				if (tree.getCanGrowWidth()) {
					int i = 1;
					if ((tree.getIsComplete() && !tree.getCanGrowWidth())) {
						// don't grow width
						// this never happens!
					} else {
						ArrayList<AbstractParseTree2> newBranches = this.growWidth(tree);
						for (AbstractParseTree2 nt : newBranches) {
							if (nt.getHasPotential(runtimeRuleSet)) {
								result.add(nt);
							} else {
								// drop it
							}
						}
					}
				}
			}
		}

		return result;
	}

	ArrayList<AbstractParseTree2> growWidth(AbstractParseTree2 tree) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree2> result = new ArrayList<>();
		if (tree.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			RuntimeRule nextExpectedRule = tree.getNextExpectedItem();
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstTerminals(nextExpectedRule);
			List<ParseTreeBud2> buds = this.ffactory.createNewBuds(expectedNextTerminal, tree.getRoot().getEnd());
			// doing this causes non termination of parser
			// ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
			// buds.add(empty);
			for (ParseTreeBud2 bud : buds) {
				AbstractParseTree2 nt = this.pushStackNewRoot(tree, bud.getRoot());
				if (nt.getRoot().getIsEmpty()) {
					RuntimeRule ruleThatIsEmpty = nt.getRuntimeRule().getRuleThatIsEmpty();
					ParseTreeBranch2 pt = this.growHeightTree(nt, ruleThatIsEmpty);
					result.add(pt);
				} else { // bud is not empty, so progress has been made already
					if (nt.getHasPotential(runtimeRuleSet)) {
						result.add(nt);
					}
				}
			}
		}
		return result;
	}

	protected boolean getCanGraftBack(AbstractParseTree2 tree) {
		return tree.getIsComplete() && this.getIsStacked(tree);
	}

	protected boolean getIsGoal(AbstractParseTree2 tree) {
		// TODO: this use of constant is not reliable / appropriate
		return tree.getIsComplete() && !this.getIsStacked(tree) && (runtimeRuleSet.getRuleNumber("$goal$") == tree.getRuntimeRule().getRuleNumber());
	}

	protected boolean getIsStacked(AbstractParseTree2 tree) {
		IGssNode<NodeIdentifier, AbstractParseTree2> n = this.gss.peek(tree.getIdentifier());
		return null == n ? false : n.hasPrevious();
	}

	protected ArrayList<AbstractParseTree2> tryGraftBack(AbstractParseTree2 tree) throws RuleNotFoundException {
		ArrayList<AbstractParseTree2> result = new ArrayList<>();
		AbstractParseTree2 parent = this.peekTopStackedRoot(tree);
		ArrayList<AbstractParseTree2> pts = this.tryGraftInto(tree, parent);
		result.addAll(pts);
		return result;
	}

	ArrayList<AbstractParseTree2> tryGraftInto(AbstractParseTree2 tree, AbstractParseTree2 parent) throws RuleNotFoundException {
		try {
			ArrayList<AbstractParseTree2> result = new ArrayList<>();
			if (parent.getNextExpectedItem().getRuleNumber() == tree.getRuntimeRule().getRuleNumber()) {
				result.add(this.extendWith(parent, tree.getRoot()));
			} else if (tree.getIsSkip()) {
				result.add(this.extendWith(parent, tree.getRoot()));
			} else {
				//
			}
			return result;
		} catch (ParseTreeException e) {
			throw new RuntimeException("Internal Error: Should not happen", e);
		}
	}

	protected ParseTreeBranch2 extendWith(AbstractParseTree2 parent, INode extension) throws ParseTreeException {
		// TODO: need to modify GSS here!
		INode[] nc = this.addChild((Branch) parent.getRoot(), extension);
		if (extension.getIsSkip()) {
			ParseTreeBranch2 newBranch = this.ffactory.fetchOrCreateBranch(parent.getRuntimeRule(), nc, parent.nextItemIndex);
			return newBranch;
		} else {
			ParseTreeBranch2 newBranch = this.ffactory.fetchOrCreateBranch(parent.getRuntimeRule(), nc, parent.nextItemIndex + 1);
			return newBranch;
		}
	}

	protected INode[] addChild(Branch old, INode newChild) {
		INode[] newChildren = Arrays.copyOf(old.children, old.children.length + 1);
		newChildren[old.children.length] = newChild;
		return newChildren;
	}

	protected ArrayList<AbstractParseTree2> growWidthWithSkipRules(AbstractParseTree2 tree) throws RuleNotFoundException {
		ArrayList<AbstractParseTree2> result = new ArrayList<>();
		if (tree.getCanGrowWidth()) { // don't grow width if its complete...cant
										// graft back
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstSkipTerminals();

			// TODO: maybe could use smaller subset of terminals here! getTerminalAt(nextExpectedPosition)
			List<ParseTreeBud2> buds = this.ffactory.createNewBuds(expectedNextTerminal, tree.getRoot().getEnd());
			for (ParseTreeBud2 bud : buds) {
				AbstractParseTree2 nt = this.pushStackNewRoot(tree, bud.getRoot());
				if (nt.getRoot().getIsEmpty()) {
					RuntimeRule ruleThatIsEmpty = nt.getRuntimeRule().getRuleThatIsEmpty();
					ParseTreeBranch2 pt = this.growHeightTree(nt, ruleThatIsEmpty);

					if (pt.getHasPotential(runtimeRuleSet)) {
						result.add(pt);
					} else {
						int i = 0;
					}
				} else {
					result.add(nt);
				}
			}
		}
		return result;
	}

	public ArrayList<AbstractParseTree2> growHeight(AbstractParseTree2 tree) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree2> result = new ArrayList<>();
		if (tree.getIsComplete()) {
			RuntimeRule[] rules = runtimeRuleSet.getPossibleSuperRule(tree.getRuntimeRule());
			for (RuntimeRule rule : rules) {
				if (tree.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
					result.add(tree);
				}
				ParseTreeBranch2[] newTrees = this.growHeightByType(tree, rule);
				for (ParseTreeBranch2 nt : newTrees) {
					result.add(nt);
				}
			}
		} else {
			// result.add(this);
		}
		return result;
	}

	ParseTreeBranch2[] growHeightByType(AbstractParseTree2 tree, RuntimeRule runtimeRule) {
		switch (runtimeRule.getRhs().getKind()) {
			case CHOICE:
				return this.growHeightChoice(tree, runtimeRule);
			case CONCATENATION:
				return this.growHeightConcatenation(tree, runtimeRule);
			case MULTI:
				return this.growHeightMulti(tree, runtimeRule);
			case SEPARATED_LIST:
				return this.growHeightSeparatedList(tree, runtimeRule);
			case EMPTY:
				throw new RuntimeException(
						"Internal Error: Should never have called grow on an EMPTY Rule (growMe is called as there should only be one growth option)");
			default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	ParseTreeBranch2[] growHeightChoice(AbstractParseTree2 tree, RuntimeRule target) {
		RuntimeRule[] rrs = target.getRhs().getItems(tree.getRuntimeRule().getRuleNumber());
		ParseTreeBranch2[] result = new ParseTreeBranch2[rrs.length];
		for (int i = 0; i < rrs.length; ++i) {
			ParseTreeBranch2 newTree = this.growHeightTree(tree, target);
			result[i] = newTree;
		}
		return result;
	}

	ParseTreeBranch2[] growHeightConcatenation(AbstractParseTree2 tree, RuntimeRule target) {
		if (0 == target.getRhs().getItems().length) {
			return new ParseTreeBranch2[0];
		} else if (target.getRhsItem(0).getRuleNumber() == tree.getRuntimeRule().getRuleNumber()) {
			ParseTreeBranch2 newTree = this.growHeightTree(tree, target);
			return new ParseTreeBranch2[] { newTree };
		} else {
			return new ParseTreeBranch2[0];
		}
	}

	ParseTreeBranch2[] growHeightMulti(AbstractParseTree2 tree, RuntimeRule target) {
		try {
			if (target.getRhsItem(0).getRuleNumber() == tree.getRuntimeRule().getRuleNumber()
					|| (0 == target.getRhs().getMultiMin() && tree.getRoot() instanceof Leaf)) {
				ParseTreeBranch2 newTree = this.growHeightTree(tree, target);
				return new ParseTreeBranch2[] { newTree };
			} else {
				return new ParseTreeBranch2[0];
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	ParseTreeBranch2[] growHeightSeparatedList(AbstractParseTree2 tree, RuntimeRule target) {
		try {
			if (target.getRhsItem(0).getRuleNumber() == tree.getRuntimeRule().getRuleNumber()
					|| (0 == target.getRhs().getMultiMin() && tree.getRoot() instanceof Leaf)) {
				ParseTreeBranch2 newTree = this.growHeightTree(tree, target);
				return new ParseTreeBranch2[] { newTree };
			} else {
				return new ParseTreeBranch2[0];
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	ParseTreeBranch2 growHeightTree(AbstractParseTree2 tree, RuntimeRule target) {
		INode[] children = new INode[] { tree.getRoot() };
		ParseTreeBranch2 newTree = this.ffactory.fetchOrCreateBranch(target, children, 1);
		return newTree;
	}

	protected AbstractParseTree2 pushStackNewRoot(AbstractParseTree2 tree, Leaf leaf) {
		IGssNode<NodeIdentifier, AbstractParseTree2> n = this.gss.peek(tree.getIdentifier());
		ParseTreeBud2 bud = this.ffactory.fetchOrCreateBud(leaf);
		n.push(bud.getIdentifier(), bud);
		return bud;
	}

	private AbstractParseTree2 peekTopStackedRoot(AbstractParseTree2 tree) {
		IGssNode<NodeIdentifier, AbstractParseTree2> n = this.gss.peek(tree.getIdentifier());
		if (n.hasPrevious()) {
			// TODO: handle multiples
			return n.previous().get(0).getValue();
		} else {
			return null;
		}
	}
}
