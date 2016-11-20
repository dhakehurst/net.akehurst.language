package net.akehurst.language.grammar.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet.SuperRuleInfo;
import net.akehurst.language.parse.graph.IGraphNode;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parse.graph.ParseGraph;

public class Forrest3 {

	public Forrest3(IParseGraph graph, RuntimeRuleSet runtimeRuleSet) {
		this.graph = graph;
		this.runtimeRuleSet = runtimeRuleSet;
		this.goals = new ArrayList<>();
	}

	IParseGraph graph;

	// ForrestFactory2 ffactory;

	protected RuntimeRuleSet runtimeRuleSet;

	protected Forrest3 newForrest() {
		Forrest3 f2 = new Forrest3(this.graph, this.runtimeRuleSet);
		f2.goals = this.goals;
		return f2;
	}

	/**
	 * For debug purposes
	 */
	public Forrest3 shallowClone() {
		Forrest3 f2 = new Forrest3(this.graph.shallowClone(), this.runtimeRuleSet);
		f2.goals = this.goals;
		return f2;
	}

	ArrayList<IGraphNode> goals;

	// public void newSeeds(List<AbstractParseTree2_> newTrees) {
	// for (AbstractParseTree2_ t : newTrees) {
	// gss.newBottom(t.getIdentifier(), t);
	// }
	// }

	public boolean getCanGrow() {
		return !this.graph.getGrowable().isEmpty();
	}

	public IGraphNode getLongestMatch(CharSequence text) throws ParseFailedException {
		if (!this.goals.isEmpty() && this.goals.size() >= 1) {
			IGraphNode lt = this.goals.iterator().next();
			for (IGraphNode gt : this.goals) {
				if (gt.getMatchedTextLength() > lt.getMatchedTextLength()) {
					lt = gt;
				}
			}
			if (lt.getMatchedTextLength() < text.length()) {
				throw new ParseFailedException("Goal does not match full text", null);// this.extractLongestMatch());
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal", null);// this.extractLongestMatch());
		}
	}

	public Forrest3 grow() throws RuleNotFoundException, ParseTreeException {
		Forrest3 newForrest = this.newForrest();

		List<IGraphNode> toGrow = new ArrayList<>(this.graph.getGrowable());
		this.graph.getGrowable().clear();
		for (IGraphNode gn : toGrow) {

			List<IGraphNode> newNodes = this.growTreeWidthAndHeight(gn);
			// newForrest.addAll(newBranches);
			int c = newNodes.size();
			if (newNodes.isEmpty()) {
				// this.graph.getGrowable().remove(gn);
			} else {
				for (IGraphNode nn : newNodes) {
					// this.gss.getTops().add(nn);
					// TODO: what if there is a duplicate already on top!
					if (this.getIsGoal(nn)) {
						this.goals.add(nn);
					}
				}
			}

		}

		return newForrest;
	}

	public List<IGraphNode> growTreeWidthAndHeight(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGraphNode> result = new ArrayList<IGraphNode>();

		ArrayList<IGraphNode> newSkipBranches = this.growWidthWithSkipRules(gn);
		if (!newSkipBranches.isEmpty()) {
			result.addAll(newSkipBranches);
		} else {
			if (gn.getIsSkip()) {
				ArrayList<IGraphNode> nts = this.tryGraftBack(gn);
				for (IGraphNode nt : nts) {
					result.add(nt);
				}
			} else {
				if (gn.getIsComplete()) {
					ArrayList<IGraphNode> nts = this.growHeight(gn);
					for (IGraphNode nt : nts) {
						result.add(nt);
					}
				}

				// reduce
				if (gn.getCanGraftBack()) {
					ArrayList<IGraphNode> nts = this.tryGraftBack(gn);
					for (IGraphNode nt : nts) {
						result.add(nt);
					}
				}

				// shift
				if (gn.getCanGrowWidth()) {
					int i = 1;
					if ((gn.getIsComplete() && !gn.getCanGrowWidth())) {
						// don't grow width
						// this never happens!
					} else {
						ArrayList<IGraphNode> newBranches = this.growWidth(gn);
						for (IGraphNode nt : newBranches) {
							result.add(nt);
						}
					}
				}
			}
		}

		return result;
	}

	ArrayList<IGraphNode> growWidth(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGraphNode> result = new ArrayList<>();
		if (gn.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			RuntimeRule nextExpectedRule = gn.getNextExpectedItem();
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstTerminals(nextExpectedRule);
			for (RuntimeRule rr : expectedNextTerminal) {
				IGraphNode bud = this.graph.createLeaf(rr, gn.getNextInputPosition());
				if (null != bud) {
					IGraphNode nn = this.pushStackNewRoot(gn, bud);
					if (null == nn) {
						// has been dropped
					} else {
						if (nn.getIsEmpty()) {
							RuntimeRule ruleThatIsEmpty = nn.getRuntimeRule().getRuleThatIsEmpty();
							SuperRuleInfo info = new SuperRuleInfo(ruleThatIsEmpty, 0);
							IGraphNode pt = this.growHeightTree(nn, info);
							if (null!=pt) {
							this.graph.getGrowable().remove(nn);
							nn = pt;
							}
						}
						result.add(nn);

					}
				}
			}
			// doing this causes non termination of parser
			// ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
			// buds.add(empty);

		}
		return result;
	}

	protected ArrayList<IGraphNode> growWidthWithSkipRules(IGraphNode gn) throws RuleNotFoundException {
		ArrayList<IGraphNode> result = new ArrayList<>();

		if (gn.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstSkipTerminals();
			for (RuntimeRule rr : expectedNextTerminal) {
				IGraphNode bud = this.graph.createLeaf(rr, gn.getNextInputPosition());
				if (null != bud) {
					IGraphNode nn = this.pushStackNewRoot(gn, bud);
					if (null == nn) {
						// has been dropped
					} else {
						if (nn.getIsEmpty()) {
							RuntimeRule ruleThatIsEmpty = nn.getRuntimeRule().getRuleThatIsEmpty();
							SuperRuleInfo info = new SuperRuleInfo(ruleThatIsEmpty, nn.getNextItemIndex());
							IGraphNode pt = this.growHeightTree(nn, info);
							if (null!=pt)result.add(pt);
						} else { // bud is not empty, so progress has been made already
							// if (nt.getHasPotential(runtimeRuleSet)) {
							result.add(nn);
							// }
						}
					}
				}
			}

			// TODO: maybe could use smaller subset of terminals here! getTerminalAt(nextExpectedPosition)
			// TODO: maybe don't need this.but maybe we do!

		}
		return result;
	}

	protected boolean getIsGoal(IGraphNode gn) {
		// TODO: this use of constant is not reliable / appropriate

		return gn.getIsComplete() && !gn.getIsStacked() && (runtimeRuleSet.getRuleNumber("$goal$") == gn.getRuntimeRule().getRuleNumber());
	}

	// protected boolean getIsGoal(AbstractParseTree2_ tree) {
	// // TODO: this use of constant is not reliable / appropriate
	// return tree.getIsComplete() && (runtimeRuleSet.getRuleNumber("$goal$") == tree.getRuntimeRule().getRuleNumber());
	// }

	protected ArrayList<IGraphNode> tryGraftBack(IGraphNode gn) throws RuleNotFoundException {
		ArrayList<IGraphNode> result = new ArrayList<>();
		ArrayList<IGraphNode.PreviousInfo> prev = new ArrayList<>(gn.getPrevious());
		for (IGraphNode.PreviousInfo info : prev) {
			// IGssNode<NodeIdentifier, AbstractParseTree2_> parentNode = prev.get(0);
			if (info.node.hasNextExpectedItem()) {
				ArrayList<IGraphNode> pts = this.tryGraftInto(gn, info.node, info.atPosition);
				result.addAll(pts);
			} else {
				// can't push back
			}
		}
		return result;
	}

	ArrayList<IGraphNode> tryGraftInto(IGraphNode gn, IGraphNode parentNode, int atPosition) throws RuleNotFoundException {
		// try {
		ArrayList<IGraphNode> result = new ArrayList<>();
		if (parentNode.getExpectedItemAt(atPosition).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()) {
			// FIXME:problem here, if we later find that the parent needs a different child !, i.e. when child is a list (we want a longer list or recursive
			// node with more in it)
			IGraphNode extended = parentNode.duplicateWithNextChild(gn);
//			IGraphNode extended = newParent.addNextChild(gn);
			if (null != extended) {
				result.add(extended);
			}
		} else if (gn.getIsSkip()) {
			IGraphNode extended = parentNode.duplicateWithNextSkipChild(gn);
//			IGraphNode extended = parentNode.addSkipChild(gn);
			if (null != extended) {
				result.add(extended);
			}
		} else {
			// drop
		}
		return result;
		// } catch (ParseTreeException e) {
		// throw new RuntimeException("Internal Error: Should not happen", e);
		// }
	}

	// protected IGraphNode extendWith(IGraphNode parentNode, IGraphNode extension) throws ParseTreeException {
	// AbstractParseTree2_ parent = parentNode.getValue();
	// INode[] nc = this.addChild((Branch) parent.getRoot(), extension.getRoot());
	// // priority doesn't change
	// int priority = parent.getPriority();
	// if (extension.getIsSkip()) {
	// ParseTreeBranch2 newBranch = this.ffactory.fetchOrCreateBranch(parent.getRuntimeRule(), priority, nc, parent.nextItemIndex);
	// // this.gss.pop(extension.getIdentifier());
	// IGssNode<NodeIdentifier, AbstractParseTree2_> nn = parentNode.duplicate(newBranch.getIdentifier(), newBranch);
	// return nn;
	// } else {
	// ParseTreeBranch2 newBranch = this.ffactory.fetchOrCreateBranch(parent.getRuntimeRule(), priority, nc, parent.nextItemIndex + 1);
	// // this.gss.pop(extension.getIdentifier());
	// // if(newBranch.getIsComplete()) {
	// // n.replace(newBranch.getIdentifier(), newBranch);
	// // } else {
	// if (this.getHasPotential(newBranch, parentNode.previous())) {
	// IGssNode<NodeIdentifier, AbstractParseTree2_> nn = parentNode.duplicate(newBranch.getIdentifier(), newBranch);
	// // }
	// return nn;
	// } else {
	// return null;
	// }
	// }
	// }

	// protected INode[] addChild(Branch old, INode newChild) {
	// INode[] newChildren = Arrays.copyOf(old.children, old.children.length + 1);
	// newChildren[old.children.length] = newChild;
	// return newChildren;
	// }

	public ArrayList<IGraphNode> growHeight(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGraphNode> result = new ArrayList<>();
		if (gn.getIsComplete()) {
			// RuntimeRule[] rules = runtimeRuleSet.getPossibleSuperRule(gn.getRuntimeRule());
			SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
			for (SuperRuleInfo info : infos) {
				if (gn.getRuntimeRule().getRuleNumber() == info.getRuntimeRule().getRuleNumber()) {
					result.add(gn);
				}
				if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {

					ArrayList<IGraphNode> newTrees = this.growHeightByType(gn, info);
					for (IGraphNode nt : newTrees) {
						if (null != nt) {
							result.add(nt);
						}
					}
				}
			}
		} else {
			// result.add(this);
		}
		return result;
	}

	ArrayList<IGraphNode> growHeightByType(IGraphNode gn, SuperRuleInfo info) {
		switch (info.getRuntimeRule().getRhs().getKind()) {
			case CHOICE:
				return this.growHeightChoice(gn, info);
			case PRIORITY_CHOICE:
				return this.growHeightPriorityChoice(gn, info);
			case CONCATENATION:
				return this.growHeightConcatenation(gn, info);
			case MULTI:
				return this.growHeightMulti(gn, info);
			case SEPARATED_LIST:
				return this.growHeightSeparatedList(gn, info);
			case EMPTY:
				throw new RuntimeException(
						"Internal Error: Should never have called grow on an EMPTY Rule (growMe is called as there should only be one growth option)");
			default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	ArrayList<IGraphNode> growHeightChoice(IGraphNode gn, SuperRuleInfo info) {

		RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		ArrayList<IGraphNode> result = new ArrayList<>();
		for (int i = 0; i < rrs.length; ++i) {
			IGraphNode nn = this.growHeightTree(gn, info);
			if (null == nn) {
				// has been dropped
			} else {
				result.add(nn);
			}
		}
		return result;
	}

	ArrayList<IGraphNode> growHeightPriorityChoice(IGraphNode gn, SuperRuleInfo info) {
		RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		ArrayList<IGraphNode> result = new ArrayList<>();
		for (int i = 0; i < rrs.length; ++i) {
			IGraphNode nn = this.growHeightTree(gn, info);
			if (null == nn) {
				// has been dropped
			} else {
				result.add(nn);
			}
		}
		return result;
	}

	ArrayList<IGraphNode> growHeightConcatenation(IGraphNode gn, SuperRuleInfo info) {
		if (0 == info.getRuntimeRule().getRhs().getItems().length) {
			return new ArrayList<>();
		}
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()) {
			IGraphNode nn = this.growHeightTree(gn, info);
			if (null == nn) {
				// has been dropped
				return new ArrayList<>();
			} else {
				ArrayList<IGraphNode> res = new ArrayList<>();
				res.add(nn);
				return res;
			}
		} else {
			return new ArrayList<>();
		}
	}

	ArrayList<IGraphNode> growHeightMulti(IGraphNode gn, SuperRuleInfo info) {
		try {
			if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()
					|| (0 == info.getRuntimeRule().getRhs().getMultiMin() && gn.getIsLeaf())) {
				IGraphNode nn = this.growHeightTree(gn, info);
				if (null == nn) {
					// has been dropped
					return new ArrayList<>();
				} else {
					ArrayList<IGraphNode> res = new ArrayList<>();
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

	ArrayList<IGraphNode> growHeightSeparatedList(IGraphNode gn, SuperRuleInfo info) {
		try {
			if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()
					|| (0 == info.getRuntimeRule().getRhs().getMultiMin() && gn.getIsLeaf())) {
				IGraphNode nn = this.growHeightTree(gn, info);
				if (null == nn) {
					// has been dropped
					return new ArrayList<>();
				} else {
					ArrayList<IGraphNode> res = new ArrayList<>();
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

	IGraphNode growHeightTree(IGraphNode gn, SuperRuleInfo info) {
		// INode[] children = new INode[] { gn.getRoot() };
		int priority = info.getRuntimeRule().getRhsIndexOf(gn.getRuntimeRule());
		// ParseTreeBranch2 newTree = this.ffactory.fetchOrCreateBranch(target, priority, children, 1);
		IGraphNode newParent = null;
		// Check if new node already exists
		NodeIdentifier id = new NodeIdentifier(info.getRuntimeRule().getRuleNumber(), gn.getStartPosition(), gn.getMatchedTextLength());
		IGraphNode existing2 = this.graph.peek(id);
		if (null!=existing2 && gn.getMatchedTextLength() > existing2.getMatchedTextLength()) {
			return existing2;

		} else {

		if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {
			newParent = this.graph.createBranch(info.getRuntimeRule(), priority, gn.getStartPosition(), 0, 0);
			newParent.getPrevious().addAll(gn.getPrevious());
			newParent = newParent.duplicateWithNextChild(gn);
			// } else {
			// newParent = existing2;
			// }

			// if (this.getHasPotential(newParent, gn.getPrevious(), info.getIndex())) {
			if (info.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.PRIORITY_CHOICE) {
				NodeIdentifier id2 = new NodeIdentifier(newParent.getRuntimeRule().getRuleNumber(), newParent.getStartPosition(),
						newParent.getMatchedTextLength());
				IGraphNode existing = this.graph.peek(id2);
				// TODO:
				if (null == existing) {

					return newParent;
				} else {
					// lower value is higher priority
					// TODO: don't think this is correct
					if (newParent.getPriority() < existing.getPriority()) {
//						existing.replace(newParent);
//						IGraphNode nn = gn.replace(newParent);
						return null;
					} else {
						IGraphNode nn = existing;// gssnode.replace(newTree.getIdentifier(), newTree);
						return nn;
					}
				}
			} else {
				// IGraphNode nn = gn.replace(newParent);

				return newParent;
			}
		} else {
			return null;
		}
		}
	}

	boolean hasHeightPotential(RuntimeRule newParentRule, IGraphNode child) {
		if (newParentRule.couldHaveChild(child.getRuntimeRule(), 0)) {
			if (runtimeRuleSet.getAllSkipTerminals().contains(child.getRuntimeRule())) {
				return true;
			} else if (child.getIsStacked()) {
				RuntimeRule nextExpectedForStacked = child.getPrevious().get(0).node.getNextExpectedItem();
				if (nextExpectedForStacked.getRuleNumber() == newParentRule.getRuleNumber()) {
					return true;
				} else {
					if (nextExpectedForStacked.getKind() == RuntimeRuleKind.NON_TERMINAL) {
						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstSubRule(nextExpectedForStacked));
						boolean res = possibles.contains(newParentRule);
						return res;
					} else {
						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstTerminals(nextExpectedForStacked));
						boolean res = possibles.contains(newParentRule);
						return res;
					}
				}
				// SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(child.getRuntimeRule());
				// return this.hasStackedPotential(newParentRule, child.getPrevious().get(0).node.getRuntimeRule());
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	protected IGraphNode pushStackNewRoot(IGraphNode gn, IGraphNode bud) {
		// ParseTreeBud2 bud = this.ffactory.fetchOrCreateBud(leaf);
		// if (this.getHasPotential(bud, Arrays.asList(new IGraphNode.PreviousInfo(gn,gn.getNextItemIndex())), gn.getNextItemIndex())) {
		if (this.hasStackedPotential(bud.getRuntimeRule(), gn.getRuntimeRule())) {
			IGraphNode nn = gn.pushToStackOf(bud, gn.getNextItemIndex());
			return nn;
		} else {
			return null;
		}
	}

	boolean hasStackedPotential(RuntimeRule gnRule, RuntimeRule stackedRule) {
		if (gnRule.getIsSkipRule()) {
			return true;
		}

		if (gnRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
			List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubRule(stackedRule));
			boolean res = possibles.contains(gnRule);
			return res;
		} else if (runtimeRuleSet.getAllSkipTerminals().contains(gnRule)) {
			return true;
		} else {
			List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(stackedRule));
			boolean res = possibles.contains(gnRule);
			return res;
		}
	}

	// private AbstractParseTree2_ peekTopStackedRoot(AbstractParseTree2_ tree) {
	// IGssNode<NodeIdentifier, AbstractParseTree2_> n = this.gss.peek(tree.getIdentifier());
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
	public boolean getHasPotential(IGraphNode tree, List<IGraphNode.PreviousInfo> stackedTreeNodes, int atPosition) {
		if (this.getIsGoal(tree)) { // TODO: not sure we should do this test here
			return true;
		} else {
			if ((!tree.getCanGrowWidth() && stackedTreeNodes.isEmpty())) { // !this.getCanGrow(tree)) {
				return false;
			} else {
				if (stackedTreeNodes.isEmpty()) {
					return true; // only happens when tree is dealing with goal stuff
				} else {
					RuntimeRule thisRule = tree.getRuntimeRule();
					// IGssNode<NodeIdentifier, AbstractParseTree2_> n = this.gss.peek(tree.getIdentifier()); // TODO: what if it is already in the gss?

					// TODO: need to know the expected child index of tree relative the stackedTree

					IGraphNode.PreviousInfo stackedTree = stackedTreeNodes.get(0); // TODO: handle multiples
					RuntimeRule expectedRule = stackedTree.node.getExpectedItemAt(atPosition); // TODO: nextexpected from all the stacked trees
					if (tree.getRuntimeRule().getRuleNumber() == expectedRule.getRuleNumber()) {
						return true;
					}
					RuntimeRule stackedRule = stackedTree.node.getRuntimeRule();
					if (thisRule == stackedRule || thisRule.getIsSkipRule()) {
						return true;
					} else {
						if (thisRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
							// List<RuntimeRule> possibles =
							// Arrays.asList(runtimeRuleSet.getPossibleSubRule(nextExpectedRule));
							// List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstSubRule(expectedRule));
							List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubRule(stackedRule));
							boolean res = possibles.contains(thisRule);
							return res;
						} else if (runtimeRuleSet.getAllSkipTerminals().contains(thisRule)) {
							return true;
						} else {
							// List<RuntimeRule> possibles =
							// Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(nextExpectedRule));
							// List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstTerminals(expectedRule));
							List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(stackedRule));
							boolean res = possibles.contains(thisRule);
							return res;
						}
					}

				}
			}
		}
	}

	@Override
	public String toString() {
		return this.goals + System.lineSeparator() + this.graph;
	}
}
