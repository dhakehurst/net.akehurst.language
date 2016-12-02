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
import net.akehurst.language.parse.graph.GraphNodeRoot;
import net.akehurst.language.parse.graph.IGraphNode;
import net.akehurst.language.parse.graph.IGraphNode.PreviousInfo;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parse.graph.ParseGraph;
import net.akehurst.language.parse.graph.ParseTreeFromGraph;

public class Forrest3 {

	public Forrest3(IParseGraph graph, RuntimeRuleSet runtimeRuleSet, Input3 input, RuntimeRule goalRule) {
		this.graph = graph;
		this.runtimeRuleSet = runtimeRuleSet;
		this.input = input;
		this.goalRule = goalRule;

	}

	RuntimeRule goalRule;
	IParseGraph graph;

	// ForrestFactory2 ffactory;

	protected RuntimeRuleSet runtimeRuleSet;
	Input3 input;

	protected Forrest3 newForrest() {
		Forrest3 f2 = new Forrest3(this.graph, this.runtimeRuleSet, this.input, this.goalRule);
		return f2;
	}

	/**
	 * For debug purposes
	 */
	public Forrest3 shallowClone() {
		Forrest3 f2 = new Forrest3(this.graph, this.runtimeRuleSet, this.input, this.goalRule);
		return f2;
	}

	public boolean getCanGrow() {
		return !this.graph.getGrowable().isEmpty();
	}

	public IGraphNode getLongestMatch(CharSequence text) throws ParseFailedException {
		if (!this.graph.getGoals().isEmpty() && this.graph.getGoals().size() >= 1) {
			IGraphNode lt = this.graph.getGoals().iterator().next();
			for (IGraphNode gt : this.graph.getGoals()) {
				if (gt.getMatchedTextLength() > lt.getMatchedTextLength()) {
					lt = gt;
				}
			}
			if (lt.getMatchedTextLength() < text.length()) {
				throw new ParseFailedException("Goal does not match full text", this.extractLongestMatchFromStart());
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal", this.extractLongestMatchFromStart());
		}
	}

	private IParseTree extractLongestMatch() {
		if (this.graph.getCompleteNodes().isEmpty()) {
			return null;
		}
		IGraphNode longest = null;
		for (IGraphNode n : this.graph.getCompleteNodes()) {
			if (null == longest || n.getMatchedTextLength() > longest.getMatchedTextLength()) {
				longest = n;
			}
		}
		if (longest.getIsLeaf()) {
			return new ParseTreeFromGraph(new GraphNodeRoot(longest.getRuntimeRule(), Arrays.asList(longest)));
		}
		return new ParseTreeFromGraph(new GraphNodeRoot(longest.getRuntimeRule(), longest.getChildren()));
	}

	private IParseTree extractLongestMatchFromStart() {
		if (this.graph.getCompleteNodes().isEmpty()) {
			return null;
		}
		IGraphNode longest = null;
		for (IGraphNode n : this.graph.getCompleteNodes()) {
			if (n.getStartPosition() == 1) {
				if (null == longest || n.getMatchedTextLength() > longest.getMatchedTextLength()) {
					longest = n;
				}
			}
		}
		if (null == longest) {
			return this.extractLongestMatch();
		} else {
			return new ParseTreeFromGraph(new GraphNodeRoot(longest.getRuntimeRule(), longest.getChildren()));
		}
	}

	public void start(IParseGraph graph, RuntimeRule goalRule, Input3 input) {

		IGraphNode gn = graph.createBranch(goalRule, 0, 0, 0, 0, 0);
		// if (this.getIsGoal(gn)) {
		// this.goals.add(gn);
		// }

	}

	public Forrest3 grow() throws RuleNotFoundException, ParseTreeException {
		Forrest3 newForrest = this.newForrest();

		List<IGraphNode> toGrow = new ArrayList<>(this.graph.getGrowable());
		this.graph.getGrowable().clear();
		for (IGraphNode gn : toGrow) {

			List<IGraphNode> newNodes = this.growTreeWidthAndHeight(gn);
			// newForrest.addAll(newBranches);
			int c = newNodes.size();
//			if (newNodes.isEmpty()) {
				// this.graph.getGrowable().remove(gn);
//			} else {
				// for (IGraphNode nn : newNodes) {
				// // this.gss.getTops().add(nn);
				// // TODO: what if there is a duplicate already on top!
				//// if (this.getIsGoal(nn)) {
				//// this.goals.add(nn);
				//// }
				// }
//			}

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
				ArrayList<IGraphNode> nts = this.tryGraftBackSkipNode(gn);
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

	public void growWidthSkipOrNormal(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGraphNode> newBranches = this.growWidthWithSkipRules(gn);
		if (newBranches.isEmpty()) {
			newBranches = this.growWidth(gn);
		}
	}

	ArrayList<IGraphNode> growWidth(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGraphNode> result = new ArrayList<>();
		if (gn.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			// List<RuntimeRule> nextExpectedRule = gn.getNextExpectedItem();
			// for(RuntimeRule err: nextExpectedRule) {
			List<RuntimeRule> expectedNextTerminal = gn.getNextExpectedTerminals();
			for (RuntimeRule rr : expectedNextTerminal) {
				Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());
				if (null != l) {
					IGraphNode bud = this.graph.createLeaf(l, rr, gn.getNextInputPosition(), l.getMatchedTextLength());
					if (bud.getRuntimeRule().getIsEmptyRule()) {
						RuntimeRule ruleThatIsEmpty = bud.getRuntimeRule().getRuleThatIsEmpty();
						IGraphNode pt = this.graph.createWithFirstChild(ruleThatIsEmpty, bud.getPriority(), bud);
						// if (this.getIsGoal(pt)) {
						// this.goals.add(pt);
						// }
						IGraphNode nn = this.pushStackNewRoot(gn, pt);
						if (null != nn) {
							result.add(nn);
						}
					} else {
						// what if bud exists and already has stacked nodes?
						IGraphNode nn = this.pushStackNewRoot(gn, bud);
						// if (this.getIsGoal(nn)) {
						// this.goals.add(nn);
						// }
						if (null != nn) {
							result.add(nn);
						}
					}
				}
			}
			// }
			// doing this causes non termination of parser
			// ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
			// buds.add(empty);

		}
		return result;
	}

	protected ArrayList<IGraphNode> growWidthWithSkipRules(IGraphNode gn) throws RuleNotFoundException {
		ArrayList<IGraphNode> result = new ArrayList<>();

		if (gn.getCanGrowWidthWithSkip()) { // don't grow width if its complete...cant graft back
			RuntimeRule[] expectedNextTerminal = runtimeRuleSet.getPossibleFirstSkipTerminals();
			for (RuntimeRule rr : expectedNextTerminal) {
				// TODO: check if this is already growing!
				Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());

				if (null != l) {
					IGraphNode bud = this.graph.createLeaf(l, rr, gn.getNextInputPosition(), l.getMatchedTextLength());
					if (bud.getRuntimeRule().getIsEmptyRule()) {
						RuntimeRule ruleThatIsEmpty = bud.getRuntimeRule().getRuleThatIsEmpty();
						IGraphNode pt = this.graph.createWithFirstChild(ruleThatIsEmpty, 0, bud);
						IGraphNode nn = this.pushStackNewRoot(gn, pt);
						result.add(nn);
					} else {
						IGraphNode nn = this.pushStackNewRoot(gn, bud);
						result.add(nn);
					}

				}
			}

			// TODO: maybe could use smaller subset of terminals here! getTerminalAt(nextExpectedPosition)
			// TODO: maybe don't need this.but maybe we do!

		}
		return result;
	}

	// protected boolean getIsGoal(IGraphNode gn) {
	// // TODO: this use of constant is not reliable / appropriate
	//
	//// return gn.getIsComplete() && !gn.getIsStacked() && (runtimeRuleSet.getRuleNumber("$goal$") == gn.getRuntimeRule().getRuleNumber());
	// return gn.getIsComplete() && !gn.getIsStacked() && (this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber());
	// }

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
				ArrayList<IGraphNode> pts = this.tryGraftInto(gn, info);
				result.addAll(pts);
			} else {
				// can't push back
			}
		}
		return result;
	}

	protected ArrayList<IGraphNode> tryGraftBackSkipNode(IGraphNode gn) throws RuleNotFoundException {
		ArrayList<IGraphNode> result = new ArrayList<>();
		ArrayList<IGraphNode.PreviousInfo> prev = new ArrayList<>(gn.getPrevious());
		for (IGraphNode.PreviousInfo info : prev) {
			// IGssNode<NodeIdentifier, AbstractParseTree2_> parentNode = prev.get(0);
//			if (info.node.hasNextExpectedItem()) {
				ArrayList<IGraphNode> pts = this.tryGraftInto(gn, info);
				result.addAll(pts);
//			} else {
				// can't push back
//			}
		}
		return result;
	}
	
	ArrayList<IGraphNode> tryGraftInto(IGraphNode gn, IGraphNode.PreviousInfo info) throws RuleNotFoundException {
		// try {
		ArrayList<IGraphNode> result = new ArrayList<>();
		if (gn.getIsSkip()) {
			IGraphNode extended = info.node.duplicateWithNextSkipChild(gn);
			// gn.getPrevious().remove(info);
			// IGraphNode extended = parentNode.addSkipChild(gn);
			if (null != extended) {
				result.add(extended);
			}
		} else if (info.node.getExpectsItemAt(gn.getRuntimeRule(), info.atPosition)) {
			IGraphNode extended = info.node.duplicateWithNextChild(gn);
			// IGraphNode extended = newParent.addNextChild(gn);
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
		int priority = info.getRuntimeRule().getRhsIndexOf(gn.getRuntimeRule());

		if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {

			if (info.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.PRIORITY_CHOICE) {
				IGraphNode existing = this.graph.findCompleteNode(info.getRuntimeRule().getRuleNumber(), gn.getStartPosition(), gn.getMatchedTextLength());
				if (null == existing) {
					//use new
					IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);
					return newParent;
				} else {
					// higher priority has a lower number
					// existing must have only one child, because the rule is a prioritychoice
					// existing must be complete or we wouldn't know about it
					// when we created it, it should have got the priority of its child
					int existingPriority = existing.getPriority();//.getChildren().get(0).getPriority();
					if (existingPriority==priority) {
						if (existing.getMatchedTextLength() > gn.getMatchedTextLength()) {
							//use existing
							IGraphNode newParent = existing.duplicateWithOtherStack(existingPriority, gn.getPrevious());
							return newParent;
						} else {
							//use new
							IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);
							return newParent;
						}
					} else if (existingPriority > priority) {
						//use new
						IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);
						return newParent;
					} else {
						if (existing.getMatchedTextLength() > gn.getMatchedTextLength()) {
							//use existing
							IGraphNode newParent = existing.duplicateWithOtherStack(existingPriority, gn.getPrevious());
							return newParent;
						} else {
							//use new
							IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);
							return newParent;
						}

					}
				}
			} else {

				IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);
				return newParent;
			}
		} else {
			return null;
		}

	}

	int getHeight(IGraphNode n) {
		int i = 0;
		while (!n.getChildren().isEmpty()) {
			i++;
			n = n.getChildren().get(0);
		}
		return i;
	}

	boolean hasHeightPotential(RuntimeRule newParentRule, IGraphNode child) {
		if (newParentRule.couldHaveChild(child.getRuntimeRule(), 0)) {
			if (runtimeRuleSet.getAllSkipTerminals().contains(child.getRuntimeRule())) {
				return true;
			} else if (child.getIsStacked()) {
				PreviousInfo prev = child.getPrevious().get(0);
				List<RuntimeRule> nextExpectedForStacked = prev.node.getNextExpectedItem();
				// if (nextExpectedForStacked.getRuleNumber() == newParentRule.getRuleNumber()) {
				if (nextExpectedForStacked.contains(newParentRule)) {
					return true;
				} else {
					for (RuntimeRule rr : nextExpectedForStacked) {
						if (rr.getKind() == RuntimeRuleKind.NON_TERMINAL) {
							List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstSubRule(rr));
							if (possibles.contains(newParentRule)) {
								return true;
							}
						} else {
							List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstTerminals(rr));
							if (possibles.contains(newParentRule)) {
								return true;
							}
						}
					}
					return false;
				}
				// SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(child.getRuntimeRule());
				// return this.hasStackedPotential(newParentRule, child.getPrevious().get(0).node.getRuntimeRule());
			} else {
				return false;
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

	// /**
	// * Filters out trees that won't grow
	// *
	// * @param runtimeRuleSet
	// * @return
	// */
	// public boolean getHasPotential(IGraphNode tree, List<IGraphNode.PreviousInfo> stackedTreeNodes, int atPosition) {
	// if (this.getIsGoal(tree)) { // TODO: not sure we should do this test here
	// return true;
	// } else {
	// if ((!tree.getCanGrowWidth() && stackedTreeNodes.isEmpty())) { // !this.getCanGrow(tree)) {
	// return false;
	// } else {
	// if (stackedTreeNodes.isEmpty()) {
	// return true; // only happens when tree is dealing with goal stuff
	// } else {
	// RuntimeRule thisRule = tree.getRuntimeRule();
	// // IGssNode<NodeIdentifier, AbstractParseTree2_> n = this.gss.peek(tree.getIdentifier()); // TODO: what if it is already in the gss?
	//
	// // TODO: need to know the expected child index of tree relative the stackedTree
	//
	// IGraphNode.PreviousInfo stackedTree = stackedTreeNodes.get(0); // TODO: handle multiples
	// RuntimeRule expectedRule = stackedTree.node.getExpectedItemAt(atPosition); // TODO: nextexpected from all the stacked trees
	// if (tree.getRuntimeRule().getRuleNumber() == expectedRule.getRuleNumber()) {
	// return true;
	// }
	// RuntimeRule stackedRule = stackedTree.node.getRuntimeRule();
	// if (thisRule == stackedRule || thisRule.getIsSkipRule()) {
	// return true;
	// } else {
	// if (thisRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
	// // List<RuntimeRule> possibles =
	// // Arrays.asList(runtimeRuleSet.getPossibleSubRule(nextExpectedRule));
	// // List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstSubRule(expectedRule));
	// List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubRule(stackedRule));
	// boolean res = possibles.contains(thisRule);
	// return res;
	// } else if (runtimeRuleSet.getAllSkipTerminals().contains(thisRule)) {
	// return true;
	// } else {
	// // List<RuntimeRule> possibles =
	// // Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(nextExpectedRule));
	// // List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstTerminals(expectedRule));
	// List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(stackedRule));
	// boolean res = possibles.contains(thisRule);
	// return res;
	// }
	// }
	//
	// }
	// }
	// }
	// }

	@Override
	public String toString() {
		return this.graph.toString();
	}
}
