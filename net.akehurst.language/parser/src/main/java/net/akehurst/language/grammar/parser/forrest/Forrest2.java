package net.akehurst.language.grammar.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parse.tree.Node;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.graphStructuredStack.IGraphStructuredStack;
import net.akehurst.language.graphStructuredStack.IGssNode;
import net.akehurst.language.graphStructuredStack.impl.GraphStructuredStack;

public class Forrest2 {

	public Forrest2(ForrestFactory2 ffactory, RuntimeRuleSet runtimeRuleSet) {
		this.ffactory = ffactory;
		this.runtimeRuleSet = runtimeRuleSet;
		this.gss = new GraphStructuredStack<>();
		this.goals = new ArrayList<>();
	}

	ForrestFactory2 ffactory;

	protected RuntimeRuleSet runtimeRuleSet;

	protected Forrest2 newForrest() {
		Forrest2 f2 = new Forrest2(this.ffactory, this.runtimeRuleSet);
		f2.gss = this.gss;
		f2.goals = this.goals;
		return f2;
	}

	/**
	 * For debug purposes
	 */
	public Forrest2 shallowClone() {
		// really should clone the gss here!
		Forrest2 f2 = new Forrest2(this.ffactory, this.runtimeRuleSet);
		f2.gss = this.gss.shallowClone();
		f2.goals = this.goals;
		return f2;
	}

	ArrayList<AbstractParseTree2> goals;

	IGraphStructuredStack<NodeIdentifier, AbstractParseTree2> gss;

	public void newSeeds(List<AbstractParseTree2> newTrees) {
		for (AbstractParseTree2 t : newTrees) {
			gss.newBottom(t.getIdentifier(), t);
		}
	}

	public boolean getCanGrow() {
//		boolean b = false;
//		for (IGssNode<NodeIdentifier, AbstractParseTree2> n : this.gss.getTops()) {
//			b = b || this.getCanGrow(n);
//		}
//		return b;
		return !this.gss.getTops().isEmpty();
	}

	boolean getCanGrow(IGssNode<NodeIdentifier, AbstractParseTree2> n) {
		if (this.getIsStacked(n))
			return true;
		return n.getValue().getCanGrowWidth();
	}

	public IParseTree getLongestMatch(CharSequence text) throws ParseFailedException {
		if (!this.goals.isEmpty() && this.goals.size() >= 1) {
			IParseTree lt = this.goals.iterator().next();
			for (AbstractParseTree2 gt : this.goals) {
				if (gt.getRoot().getMatchedTextLength() > lt.getRoot().getMatchedTextLength()) {
					lt = gt;
				}
			}
			if (lt.getRoot().getMatchedTextLength() < text.length()) {
				throw new ParseFailedException("Goal does not match full text", null);// this.extractLongestMatch());
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal", null);// this.extractLongestMatch());
		}
	}

	public Forrest2 grow() throws RuleNotFoundException, ParseTreeException {
		Forrest2 newForrest = this.newForrest();

		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> toGrow = new ArrayList<>(gss.getTops());
		gss.getTops().clear();
		for (IGssNode<NodeIdentifier, AbstractParseTree2> gn : toGrow) {
			if (this.getIsGoal(gn)) {
				AbstractParseTree2 g = gn.getValue();
				this.goals.add(g);
			} else {
				ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> newNodes = this.growTreeWidthAndHeight(gn);
				// newForrest.addAll(newBranches);
				int c = newNodes.size();
				if (newNodes.isEmpty()) {
					// this.gss.getTops().remove(gn);
				} else {
					for (IGssNode<NodeIdentifier, AbstractParseTree2> nn : newNodes) {
						this.gss.getTops().add(nn);
						// TODO: what if there is a duplicate already on top!
					}
				}
			}
		}

		return newForrest;
	}

	public ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growTreeWidthAndHeight(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode)
			throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>>();
		AbstractParseTree2 tree = gssnode.getValue();
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> newSkipBranches = this.growWidthWithSkipRules(gssnode);
		if (!newSkipBranches.isEmpty()) {
			result.addAll(newSkipBranches);
		} else {
			if (tree.getIsSkip()) {
				ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> nts = this.tryGraftBack(gssnode);
				for (IGssNode<NodeIdentifier, AbstractParseTree2> nt : nts) {
					// if (nt.getHasPotential(this.runtimeRuleSet)) {
					result.add(nt);
					// } else {
					// // drop it
					// }
				}
			} else {
				if (tree.getIsComplete()) {
					ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> nts = this.growHeight(gssnode);
					for (IGssNode<NodeIdentifier, AbstractParseTree2> nt : nts) {
						// if (nt.getHasPotential(this.runtimeRuleSet)) {
						result.add(nt);
						// } else {
						// // drop it
						// }
					}
				}

				// reduce
				if (this.getCanGraftBack(gssnode)) {
					ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> nts = this.tryGraftBack(gssnode);
					for (IGssNode<NodeIdentifier, AbstractParseTree2> nt : nts) {
						// if (nt.getHasPotential(this.runtimeRuleSet) || this.getIsGoal(nt)) {
						result.add(nt);
						// } else {
						// // drop it
						// }
					}
				}

				// shift
				if (tree.getCanGrowWidth()) {
					int i = 1;
					if ((tree.getIsComplete() && !tree.getCanGrowWidth())) {
						// don't grow width
						// this never happens!
					} else {
						ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> newBranches = this.growWidth(gssnode);
						for (IGssNode<NodeIdentifier, AbstractParseTree2> nt : newBranches) {
							// if (nt.getHasPotential(runtimeRuleSet)) {
							result.add(nt);
							// } else {
							// // drop it
							// }
						}
					}
				}
			}
		}

		return result;
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growWidth(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode)
			throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<>();
		AbstractParseTree2 tree = gssnode.getValue();
		if (tree.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			RuntimeRule nextExpectedRule = tree.getNextExpectedItem();
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstTerminals(nextExpectedRule);
			List<ParseTreeBud2> buds = this.ffactory.createNewBuds(expectedNextTerminal, tree.getRoot().getEnd());
			// doing this causes non termination of parser
			// ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
			// buds.add(empty);
			for (ParseTreeBud2 bud : buds) {
				IGssNode<NodeIdentifier, AbstractParseTree2> nn = this.pushStackNewRoot(gssnode, bud);
				if (null == nn) {
					// has been dropped
				} else {
					if (nn.getValue().getRoot().getIsEmpty()) {
						RuntimeRule ruleThatIsEmpty = nn.getValue().getRuntimeRule().getRuleThatIsEmpty();
						IGssNode<NodeIdentifier, AbstractParseTree2> pt = this.growHeightTree(nn, ruleThatIsEmpty);
						result.add(pt);
					} else { // bud is not empty, so progress has been made already
						// if (nt.getHasPotential(runtimeRuleSet)) {
						result.add(nn);
						// }
					}
				}
			}
		}
		return result;
	}

	protected ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growWidthWithSkipRules(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode)
			throws RuleNotFoundException {
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<>();
		AbstractParseTree2 tree = gssnode.getValue();
		if (tree.getCanGrowWidth()) { // don't grow width if its complete...cant
										// graft back
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstSkipTerminals();

			// TODO: maybe could use smaller subset of terminals here! getTerminalAt(nextExpectedPosition)
			List<ParseTreeBud2> buds = this.ffactory.createNewBuds(expectedNextTerminal, tree.getRoot().getEnd());
			for (ParseTreeBud2 bud : buds) {
				IGssNode<NodeIdentifier, AbstractParseTree2> nn = this.pushStackNewRoot(gssnode, bud);
				if (null == nn) {
					// has been dropped
				} else {
					if (nn.getValue().getRoot().getIsEmpty()) {
						RuntimeRule ruleThatIsEmpty = nn.getValue().getRuntimeRule().getRuleThatIsEmpty();
						IGssNode<NodeIdentifier, AbstractParseTree2> pt = this.growHeightTree(nn, ruleThatIsEmpty);
						// if (pt.getHasPotential(runtimeRuleSet)) {
						result.add(pt);
						// } else {
						// int i = 0;
						// }
					} else {
						result.add(nn);
					}
				}
			}
		}
		return result;
	}

	protected boolean getCanGraftBack(IGssNode<NodeIdentifier, AbstractParseTree2> n) {
		return n.getValue().getIsComplete() && this.getIsStacked(n);
	}

	protected boolean getIsGoal(IGssNode<NodeIdentifier, AbstractParseTree2> n) {
		// TODO: this use of constant is not reliable / appropriate
		AbstractParseTree2 tree = n.getValue();
		return tree.getIsComplete() && !this.getIsStacked(n) && (runtimeRuleSet.getRuleNumber("$goal$") == tree.getRuntimeRule().getRuleNumber());
	}

	protected boolean getIsStacked(IGssNode<NodeIdentifier, AbstractParseTree2> n) {
		return null == n ? false : n.hasPrevious();
	}

	protected ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> tryGraftBack(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode)
			throws RuleNotFoundException {
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<>();
		AbstractParseTree2 tree = gssnode.getValue();
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> prev = new ArrayList<>(gssnode.previous());
		for (IGssNode<NodeIdentifier, AbstractParseTree2> parentNode : prev) {
			// IGssNode<NodeIdentifier, AbstractParseTree2> parentNode = prev.get(0);
			if (parentNode.getValue().hasNextExpectedItem()) {
				ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> pts = this.tryGraftInto(tree, parentNode);
				result.addAll(pts);
			} else {
				//can't push back
			}
		}
		return result;
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> tryGraftInto(AbstractParseTree2 tree, IGssNode<NodeIdentifier, AbstractParseTree2> parentNode)
			throws RuleNotFoundException {
		try {
			ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<>();
			AbstractParseTree2 parent = parentNode.getValue();
			if (parent.getNextExpectedItem().getRuleNumber() == tree.getRuntimeRule().getRuleNumber()) {
				IGssNode<NodeIdentifier, AbstractParseTree2> extended = this.extendWith(parentNode, tree);
				result.add(extended);
			} else if (tree.getIsSkip()) {
				IGssNode<NodeIdentifier, AbstractParseTree2> extended = this.extendWith(parentNode, tree);
				result.add(extended);
			} else {
				//
			}
			return result;
		} catch (ParseTreeException e) {
			throw new RuntimeException("Internal Error: Should not happen", e);
		}
	}

	protected IGssNode<NodeIdentifier, AbstractParseTree2> extendWith(IGssNode<NodeIdentifier, AbstractParseTree2> parentNode, AbstractParseTree2 extension)
			throws ParseTreeException {
		AbstractParseTree2 parent = parentNode.getValue();
		INode[] nc = this.addChild((Branch) parent.getRoot(), extension.getRoot());
		if (extension.getIsSkip()) {
			ParseTreeBranch2 newBranch = this.ffactory.fetchOrCreateBranch(parent.getRuntimeRule(), nc, parent.nextItemIndex);
			// this.gss.pop(extension.getIdentifier());
			IGssNode<NodeIdentifier, AbstractParseTree2> nn = parentNode.duplicate(newBranch.getIdentifier(), newBranch);
			return nn;
		} else {
			ParseTreeBranch2 newBranch = this.ffactory.fetchOrCreateBranch(parent.getRuntimeRule(), nc, parent.nextItemIndex + 1);
			// this.gss.pop(extension.getIdentifier());
			// if(newBranch.getIsComplete()) {
			// n.replace(newBranch.getIdentifier(), newBranch);
			// } else {
			IGssNode<NodeIdentifier, AbstractParseTree2> nn = parentNode.duplicate(newBranch.getIdentifier(), newBranch);
			// }
			return nn;
		}
	}

	protected INode[] addChild(Branch old, INode newChild) {
		INode[] newChildren = Arrays.copyOf(old.children, old.children.length + 1);
		newChildren[old.children.length] = newChild;
		return newChildren;
	}

	public ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growHeight(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode)
			throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<>();
		AbstractParseTree2 tree = gssnode.getValue();
		if (tree.getIsComplete()) {
			RuntimeRule[] rules = runtimeRuleSet.getPossibleSuperRule(tree.getRuntimeRule());
			for (RuntimeRule rule : rules) {
				if (tree.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
					result.add(gssnode);
				}
				ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> newTrees = this.growHeightByType(gssnode, rule);
				for (IGssNode<NodeIdentifier, AbstractParseTree2> nt : newTrees) {
					if (null != nt) {
						result.add(nt);
					}
				}
			}
		} else {
			// result.add(this);
		}
		return result;
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growHeightByType(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, RuntimeRule runtimeRule) {
		switch (runtimeRule.getRhs().getKind()) {
			case CHOICE:
				return this.growHeightChoice(gssnode, runtimeRule);
			case PRIORITY_CHOICE:
				return this.growHeightChoice(gssnode, runtimeRule);
			case CONCATENATION:
				return this.growHeightConcatenation(gssnode, runtimeRule);
			case MULTI:
				return this.growHeightMulti(gssnode, runtimeRule);
			case SEPARATED_LIST:
				return this.growHeightSeparatedList(gssnode, runtimeRule);
			case EMPTY:
				throw new RuntimeException(
						"Internal Error: Should never have called grow on an EMPTY Rule (growMe is called as there should only be one growth option)");
			default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growHeightChoice(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, RuntimeRule target) {
		AbstractParseTree2 tree = gssnode.getValue();
		RuntimeRule[] rrs = target.getRhs().getItems(tree.getRuntimeRule().getRuleNumber());
		ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> result = new ArrayList<>();
		for (int i = 0; i < rrs.length; ++i) {
			IGssNode<NodeIdentifier, AbstractParseTree2> nn = this.growHeightTree(gssnode, target);
			if (null == nn) {
				// has been dropped
			} else {
				result.add(nn);
			}
		}
		return result;
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growHeightConcatenation(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, RuntimeRule target) {
		AbstractParseTree2 tree = gssnode.getValue();
		if (0 == target.getRhs().getItems().length) {
			return new ArrayList<>();
		}
		if (target.getRhsItem(0).getRuleNumber() == tree.getRuntimeRule().getRuleNumber()) {
			IGssNode<NodeIdentifier, AbstractParseTree2> nn = this.growHeightTree(gssnode, target);
			if (null == nn) {
				// has been dropped
				return new ArrayList<>();
			} else {
				ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> res = new ArrayList<>();
				res.add(nn);
				return res;
			}
		} else {
			return new ArrayList<>();
		}
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growHeightMulti(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, RuntimeRule target) {
		try {
			AbstractParseTree2 tree = gssnode.getValue();
			if (target.getRhsItem(0).getRuleNumber() == tree.getRuntimeRule().getRuleNumber()
					|| (0 == target.getRhs().getMultiMin() && tree.getRoot() instanceof Leaf)) {
				IGssNode<NodeIdentifier, AbstractParseTree2> nn = this.growHeightTree(gssnode, target);
				if (null == nn) {
					// has been dropped
					return new ArrayList<>();
				} else {
					ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> res = new ArrayList<>();
					res.add(nn);
					return res;
				}
			} else {
				return new ArrayList<>();
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> growHeightSeparatedList(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, RuntimeRule target) {
		try {
			AbstractParseTree2 tree = gssnode.getValue();
			if (target.getRhsItem(0).getRuleNumber() == tree.getRuntimeRule().getRuleNumber()
					|| (0 == target.getRhs().getMultiMin() && tree.getRoot() instanceof Leaf)) {
				IGssNode<NodeIdentifier, AbstractParseTree2> nn = this.growHeightTree(gssnode, target);
				if (null == nn) {
					// has been dropped
					return new ArrayList<>();
				} else {
					ArrayList<IGssNode<NodeIdentifier, AbstractParseTree2>> res = new ArrayList<>();
					res.add(nn);
					return res;
				}
			} else {
				return new ArrayList<>();
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	IGssNode<NodeIdentifier, AbstractParseTree2> growHeightTree(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, RuntimeRule target) {
		AbstractParseTree2 tree = gssnode.getValue();
		INode[] children = new INode[] { tree.getRoot() };
		ParseTreeBranch2 newTree = this.ffactory.fetchOrCreateBranch(target, children, 1);
		if (this.getHasPotential(newTree, gssnode.previous())) {
			IGssNode<NodeIdentifier, AbstractParseTree2> nn = gssnode.replace(newTree.getIdentifier(), newTree);
			return nn;
		} else {
			return null;
		}
	}

	protected IGssNode<NodeIdentifier, AbstractParseTree2> pushStackNewRoot(IGssNode<NodeIdentifier, AbstractParseTree2> gssnode, AbstractParseTree2 bud) {
		// ParseTreeBud2 bud = this.ffactory.fetchOrCreateBud(leaf);
		if (this.getHasPotential(bud, Arrays.asList(gssnode))) {
			IGssNode<NodeIdentifier, AbstractParseTree2> nn = gssnode.push(bud.getIdentifier(), bud);
			return nn;
		} else {
			return null;
		}
	}

	// private AbstractParseTree2 peekTopStackedRoot(AbstractParseTree2 tree) {
	// IGssNode<NodeIdentifier, AbstractParseTree2> n = this.gss.peek(tree.getIdentifier());
	// if (n.hasPrevious()) {
	// // TODO: handle multiples
	// return n.previous().get(0).getValue();
	// } else {
	// return null;
	// }
	// }

	/**
	 * Filters out trees that won't grow
	 * 
	 * @param runtimeRuleSet
	 * @return
	 */
	public boolean getHasPotential(AbstractParseTree2 tree, List<IGssNode<NodeIdentifier, AbstractParseTree2>> stackedTreeNodes) {
		if ((!tree.getCanGrowWidth() && stackedTreeNodes.isEmpty())) { // !this.getCanGrow(tree)) {
			return false;
		} else {
			if (stackedTreeNodes.isEmpty()) {
				return true; // only happens when tree is dealing with goal stuff
			} else {
				RuntimeRule thisRule = tree.getRuntimeRule();
				// IGssNode<NodeIdentifier, AbstractParseTree2> n = this.gss.peek(tree.getIdentifier()); // TODO: what if it is already in the gss?

				AbstractParseTree2 stackedTree = stackedTreeNodes.get(0).getValue(); // TODO: handle multiples
				RuntimeRule nextExpectedRule = stackedTree.getNextExpectedItem(); // TODO: nextexpected from all the stacked trees
				if (thisRule == nextExpectedRule || thisRule.getIsSkipRule()) {
					return true;
				} else {
					if (thisRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
						// List<RuntimeRule> possibles =
						// Arrays.asList(runtimeRuleSet.getPossibleSubRule(nextExpectedRule));
						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstSubRule(nextExpectedRule));
						boolean res = possibles.contains(thisRule);
						return res;
					} else if (runtimeRuleSet.getAllSkipTerminals().contains(thisRule)) {
						return true;
					} else {
						// List<RuntimeRule> possibles =
						// Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(nextExpectedRule));
						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstTerminals(nextExpectedRule));
						boolean res = possibles.contains(thisRule);
						return res;
					}
				}

			}
		}
	}
}
