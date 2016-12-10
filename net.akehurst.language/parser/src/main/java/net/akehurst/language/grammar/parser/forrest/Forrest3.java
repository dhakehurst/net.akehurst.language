package net.akehurst.language.grammar.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
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
import net.akehurst.language.parse.graph.ParseTreeFromGraph;

public final class Forrest3 {

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
			return new ParseTreeFromGraph(longest);
		}
		return new ParseTreeFromGraph(longest);
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
			return new ParseTreeFromGraph(longest);
		}
	}

	public void start(IParseGraph graph, RuntimeRule goalRule, Input3 input) {

		IGraphNode gn = graph.createBranch(goalRule, 0, 0, 0, 0, 0);
		// if (this.getIsGoal(gn)) {
		// this.goals.add(gn);
		// }

	}

	public void grow() throws RuleNotFoundException, ParseTreeException {

		List<IGraphNode> toGrow = new ArrayList<>(this.graph.getGrowable());
		this.graph.getGrowable().clear();
		for (IGraphNode gn : toGrow) {

			this.growTreeWidthAndHeight(gn);

		}

	}

	public void growTreeWidthAndHeight(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {

		boolean didSkipNode = this.growWidthWithSkipRules(gn);
		if (didSkipNode) {
			return;
		} else {
			if (gn.getIsSkip()) {
				this.tryGraftBackSkipNode(gn);
			} else {
				if (gn.getIsComplete()) {
					this.growHeight(gn);
				}

				// reduce
				if (gn.getCanGraftBack()) {
					this.tryGraftBack(gn);
				}

				// shift
				if (gn.getCanGrowWidth()) {
					int i = 1;
					if ((gn.getIsComplete() && !gn.getCanGrowWidth())) {
						// don't grow width
						// this never happens!
					} else {
						this.growWidth(gn);
					}
				}
			}
		}
	}

	// public void growWidthSkipOrNormal(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
	// ArrayList<IGraphNode> newBranches = this.growWidthWithSkipRules(gn);
	// if (newBranches.isEmpty()) {
	// newBranches = this.growWidth(gn);
	// }
	// }

	void growWidth(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {

		if (gn.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			// List<RuntimeRule> nextExpectedRule = gn.getNextExpectedItem();
			// for(RuntimeRule err: nextExpectedRule) {
			List<RuntimeRule> expectedNextTerminal = gn.getNextExpectedTerminals();
			for (RuntimeRule rr : expectedNextTerminal) {
				Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());
				if (null != l) {
					IGraphNode bud = this.graph.createLeaf(l, rr, gn.getNextInputPosition(), l.getMatchedTextLength());
//					if (bud.getRuntimeRule().getIsEmptyRule()) {
//						RuntimeRule ruleThatIsEmpty = bud.getRuntimeRule().getRuleThatIsEmpty();
//						IGraphNode pt = this.graph.createWithFirstChild(ruleThatIsEmpty, bud.getPriority(), bud);
//						// if (this.getIsGoal(pt)) {
//						// this.goals.add(pt);
//						// }
//						IGraphNode nn = this.pushStackNewRoot(gn, pt);
//
//					} else {
						// what if bud exists and already has stacked nodes?
						IGraphNode nn = this.pushStackNewRoot(gn, bud);

//					}
				}
			}
			// }
			// doing this causes non termination of parser
			// ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
			// buds.add(empty);

		}

	}

	protected boolean growWidthWithSkipRules(IGraphNode gn) throws RuleNotFoundException {
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
		// TODO: get rid of the arraylist
		return !result.isEmpty();
	}

	protected void tryGraftBack(IGraphNode gn) throws RuleNotFoundException {

		ArrayList<IGraphNode.PreviousInfo> prev = new ArrayList<>(gn.getPrevious());
		for (IGraphNode.PreviousInfo info : prev) {
			if (info.node.hasNextExpectedItem()) {
				this.tryGraftInto(gn, info);
			} else {
				// can't push back
			}
		}

	}

	protected void tryGraftBackSkipNode(IGraphNode gn) throws RuleNotFoundException {

		ArrayList<IGraphNode.PreviousInfo> prev = new ArrayList<>(gn.getPrevious());
		for (IGraphNode.PreviousInfo info : prev) {
			this.tryGraftInto(gn, info);
		}

	}

	void tryGraftInto(IGraphNode gn, IGraphNode.PreviousInfo info) throws RuleNotFoundException {

		if (gn.getIsSkip()) {
			IGraphNode extended = info.node.duplicateWithNextSkipChild(gn);

		} else if (info.node.getExpectsItemAt(gn.getRuntimeRule(), info.atPosition)) {
			IGraphNode extended = info.node.duplicateWithNextChild(gn);

		} else {
			// drop
		}

	}

	public void growHeight(IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		ArrayList<IGraphNode> result = new ArrayList<>();
		// TODO: should have already done this test?
		if (gn.getIsComplete()) {
			if (gn.getRuntimeRule().getIsEmptyRule()) {
				RuntimeRule ruleThatIsEmpty = gn.getRuntimeRule().getRuleThatIsEmpty();
				IGraphNode pt = this.graph.createWithFirstChild(ruleThatIsEmpty, gn.getPriority(), gn);
				// if (this.getIsGoal(pt)) {
				// this.goals.add(pt);
				// }
//				IGraphNode nn = this.pushStackNewRoot(gn, pt);
				
			} else {
			// RuntimeRule[] rules = runtimeRuleSet.getPossibleSuperRule(gn.getRuntimeRule());
			SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
			for (SuperRuleInfo info : infos) {
				if (gn.getRuntimeRule().getRuleNumber() == info.getRuntimeRule().getRuleNumber()) {
					// TODO: do we need to make this growable?
					result.add(gn);
				}
				if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {

					this.growHeightByType(gn, info);

				}
			}
			}
		} else {
			// result.add(this);
		}

	}

	void growHeightByType(IGraphNode gn, SuperRuleInfo info) {
		switch (info.getRuntimeRule().getRhs().getKind()) {
			case CHOICE:
				this.growHeightChoice(gn, info);
			return;
			case PRIORITY_CHOICE:
				this.growHeightPriorityChoice(gn, info);
				return;
			case CONCATENATION:
				this.growHeightConcatenation(gn, info);
				return;
			case MULTI:
				this.growHeightMulti(gn, info);
				return;
			case SEPARATED_LIST:
				this.growHeightSeparatedList(gn, info);
				return;
			case EMPTY:
				throw new RuntimeException(
						"Internal Error: Should never have called grow on an EMPTY Rule (growMe is called as there should only be one growth option)");
			default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	void growHeightChoice(IGraphNode gn, SuperRuleInfo info) {

		RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		for (int i = 0; i < rrs.length; ++i) {
			this.growHeightTree(gn, info);
		}
	}

	void growHeightPriorityChoice(IGraphNode gn, SuperRuleInfo info) {
		RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		for (int i = 0; i < rrs.length; ++i) {
			this.growHeightTree(gn, info);
		}
	}

	void growHeightConcatenation(IGraphNode gn, SuperRuleInfo info) {
		if (0 == info.getRuntimeRule().getRhs().getItems().length) {
			// return new ArrayList<>();
		}
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()) {
			this.growHeightTree(gn, info);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightMulti(IGraphNode gn, SuperRuleInfo info) {
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()
				|| (0 == info.getRuntimeRule().getRhs().getMultiMin() && gn.getIsLeaf())) {
			this.growHeightTree(gn, info);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightSeparatedList(IGraphNode gn, SuperRuleInfo info) {
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()
				|| (0 == info.getRuntimeRule().getRhs().getMultiMin() && gn.getIsLeaf())) {
			this.growHeightTree(gn, info);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightTree(IGraphNode gn, SuperRuleInfo info) {
		int priority = info.getRuntimeRule().getRhsIndexOf(gn.getRuntimeRule());

		// should have already done this test
		if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {

			if (info.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.PRIORITY_CHOICE) {
				IGraphNode existing = this.graph.findCompleteNode(info.getRuntimeRule().getRuleNumber(), gn.getStartPosition(), gn.getMatchedTextLength());
				if (null == existing) {
					// use new
					IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

				} else {
					// higher priority has a lower number
					// existing must have only one child, because the rule is a prioritychoice
					// existing must be complete or we wouldn't know about it
					// when we created it, it should have got the priority of its child
					int existingPriority = existing.getPriority();// .getChildren().get(0).getPriority();
					if (existingPriority == priority) {
						if (existing.getMatchedTextLength() > gn.getMatchedTextLength()) {
							// use existing
							IGraphNode newParent = existing.duplicateWithOtherStack(existingPriority, gn.getPrevious());

						} else {
							// use new
							IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

						}
					} else if (existingPriority > priority) {
						// use new
						IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

					} else {
						if (existing.getMatchedTextLength() > gn.getMatchedTextLength()) {
							// use existing
							IGraphNode newParent = existing.duplicateWithOtherStack(existingPriority, gn.getPrevious());

						} else {
							// use new
							IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

						}

					}
				}
			} else {

				IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

			}
		} else {
//			return null;
		}

	}

//	int getHeight(IGraphNode n) {
//		int i = 0;
//		while (!n.getChildren().isEmpty()) {
//			i++;
//			n = n.getChildren().get(0);
//		}
//		return i;
//	}

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

	@Override
	public String toString() {
		return this.graph.toString();
	}
}
