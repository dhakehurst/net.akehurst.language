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
					this.graph.getGrowable().remove(gn);
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
				IGraphNode bud = this.graph.createLeaf(rr, gn.getEndPosition());
				if (null!=bud) {
				IGraphNode nn = this.pushStackNewRoot(gn, bud);
				if (null == nn) {
					// has been dropped
				} else {
					if (nn.getIsEmpty()) {
						RuntimeRule ruleThatIsEmpty = nn.getRuntimeRule().getRuleThatIsEmpty();
						IGraphNode pt = this.growHeightTree(nn, ruleThatIsEmpty);
						result.add(pt);
					} else { // bud is not empty, so progress has been made already
						// if (nt.getHasPotential(runtimeRuleSet)) {
						result.add(nn);
						// }
					}
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
				this.graph.createLeaf(rr, gn.getEndPosition());
			}

			// TODO: maybe could use smaller subset of terminals here! getTerminalAt(nextExpectedPosition)
			// TODO: maybe don't need this.but maybe we do!
			/**
			 * <code>
			for (ParseTreeBud2 bud : buds) {
				IGraphNode nn = this.pushStackNewRoot(gn, bud);
				if (null == nn) {
					// has been dropped
				} else {
					if (nn.getValue().getRoot().getIsEmpty()) {
						RuntimeRule ruleThatIsEmpty = nn.getValue().getRuntimeRule().getRuleThatIsEmpty();
						IGraphNode pt = this.growHeightTree(nn, ruleThatIsEmpty);
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
			 </code>
			 */
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
		ArrayList<IGraphNode> prev = new ArrayList<>(gn.getPrevious());
		for (IGraphNode parentNode : prev) {
			// IGssNode<NodeIdentifier, AbstractParseTree2_> parentNode = prev.get(0);
			if (parentNode.hasNextExpectedItem()) {
				ArrayList<IGraphNode> pts = this.tryGraftInto(gn, parentNode);
				result.addAll(pts);
			} else {
				// can't push back
			}
		}
		return result;
	}

	ArrayList<IGraphNode> tryGraftInto(IGraphNode gn, IGraphNode parentNode) throws RuleNotFoundException {
		// try {
		ArrayList<IGraphNode> result = new ArrayList<>();
		if (parentNode.getNextExpectedItem().getRuleNumber() == gn.getRuntimeRule().getRuleNumber()) {
			IGraphNode extended = parentNode.addChild(gn);
			if (null != extended) {
				result.add(extended);
			}
		} else if (gn.getIsSkip()) {
			IGraphNode extended = parentNode.addSkipChild(gn);
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
			RuntimeRule[] rules = runtimeRuleSet.getPossibleSuperRule(gn.getRuntimeRule());
			for (RuntimeRule rule : rules) {
				if (gn.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
					result.add(gn);
				}
				ArrayList<IGraphNode> newTrees = this.growHeightByType(gn, rule);
				for (IGraphNode nt : newTrees) {
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

	ArrayList<IGraphNode> growHeightByType(IGraphNode gn, RuntimeRule runtimeRule) {
		switch (runtimeRule.getRhs().getKind()) {
			case CHOICE:
				return this.growHeightChoice(gn, runtimeRule);
			case PRIORITY_CHOICE:
				return this.growHeightPriorityChoice(gn, runtimeRule);
			case CONCATENATION:
				return this.growHeightConcatenation(gn, runtimeRule);
			case MULTI:
				return this.growHeightMulti(gn, runtimeRule);
			case SEPARATED_LIST:
				return this.growHeightSeparatedList(gn, runtimeRule);
			case EMPTY:
				throw new RuntimeException(
						"Internal Error: Should never have called grow on an EMPTY Rule (growMe is called as there should only be one growth option)");
			default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	ArrayList<IGraphNode> growHeightChoice(IGraphNode gn, RuntimeRule target) {

		RuntimeRule[] rrs = target.getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		ArrayList<IGraphNode> result = new ArrayList<>();
		for (int i = 0; i < rrs.length; ++i) {
			IGraphNode nn = this.growHeightTree(gn, target);
			if (null == nn) {
				// has been dropped
			} else {
				result.add(nn);
			}
		}
		return result;
	}

	ArrayList<IGraphNode> growHeightPriorityChoice(IGraphNode gn, RuntimeRule target) {
		RuntimeRule[] rrs = target.getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		ArrayList<IGraphNode> result = new ArrayList<>();
		for (int i = 0; i < rrs.length; ++i) {
			IGraphNode nn = this.growHeightTree(gn, target);
			if (null == nn) {
				// has been dropped
			} else {
				result.add(nn);
			}
		}
		return result;
	}

	ArrayList<IGraphNode> growHeightConcatenation(IGraphNode gn, RuntimeRule target) {
		if (0 == target.getRhs().getItems().length) {
			return new ArrayList<>();
		}
		if (target.getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()) {
			IGraphNode nn = this.growHeightTree(gn, target);
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

	ArrayList<IGraphNode> growHeightMulti(IGraphNode gn, RuntimeRule target) {
		try {
			if (target.getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber() || (0 == target.getRhs().getMultiMin() && gn.getIsLeaf())) {
				IGraphNode nn = this.growHeightTree(gn, target);
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

	ArrayList<IGraphNode> growHeightSeparatedList(IGraphNode gn, RuntimeRule target) {
		try {
			if (target.getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber() || (0 == target.getRhs().getMultiMin() && gn.getIsLeaf())) {
				IGraphNode nn = this.growHeightTree(gn, target);
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

	IGraphNode growHeightTree(IGraphNode gn, RuntimeRule target) {
		// INode[] children = new INode[] { gn.getRoot() };
		int priority = target.getRhsIndexOf(gn.getRuntimeRule());
		// ParseTreeBranch2 newTree = this.ffactory.fetchOrCreateBranch(target, priority, children, 1);
		IGraphNode newParent = this.graph.createBranch(target, priority, gn, 1);

		if (this.getHasPotential(newParent, gn.getPrevious())) {
			if (target.getRhs().getKind() == RuntimeRuleItemKind.PRIORITY_CHOICE) {
				IGraphNode existing = this.graph.peek(newParent.getIdentifier());
				// TODO:
				if (null == existing) {
					IGraphNode nn = gn.replace(newParent);
					return nn;
				} else {
					// lower value is higher priority
					// TODO: don't think this is correct
					if (newParent.getPriority() < existing.getPriority()) {
						existing.replace(newParent);
						IGraphNode nn = gn.replace(newParent);
						return nn;
					} else {
						IGraphNode nn = existing;// gssnode.replace(newTree.getIdentifier(), newTree);
						return nn;
					}
				}
			} else {
				//IGraphNode nn = gn.replace(newParent);
				newParent.getPrevious().addAll(gn.getPrevious());
				return newParent;
			}
		} else {
			return null;
		}
	}

	protected IGraphNode pushStackNewRoot(IGraphNode gn, IGraphNode bud) {
		// ParseTreeBud2 bud = this.ffactory.fetchOrCreateBud(leaf);
		if (this.getHasPotential(bud, Arrays.asList(gn))) {
			IGraphNode nn = gn.pushToStackOf(bud);
			return nn;
		} else {
			return null;
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
	public boolean getHasPotential(IGraphNode tree, List<IGraphNode> stackedTreeNodes) {
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

					IGraphNode stackedTree = stackedTreeNodes.get(0); // TODO: handle multiples
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

	@Override
	public String toString() {
		return this.goals + System.lineSeparator() + this.graph;
	}
}
