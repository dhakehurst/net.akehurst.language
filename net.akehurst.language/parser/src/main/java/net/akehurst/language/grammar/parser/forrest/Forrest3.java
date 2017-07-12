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
import net.akehurst.language.parse.graph.IGraphNode;
import net.akehurst.language.parse.graph.IGraphNode.PreviousInfo;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parse.graph.ParseTreeFromGraph;

public final class Forrest3 {

	public Forrest3(final IParseGraph graph, final RuntimeRuleSet runtimeRuleSet, final Input3 input, final RuntimeRule goalRule) {
		this.graph = graph;
		this.runtimeRuleSet = runtimeRuleSet;
		this.input = input;
		this.goalRule = goalRule;
		this.toGrow = new ArrayList<>();
	}

	RuntimeRule goalRule;
	IParseGraph graph;

	// ForrestFactory2 ffactory;

	protected RuntimeRuleSet runtimeRuleSet;
	Input3 input;
	List<IGraphNode> toGrow;

	public boolean getCanGrow() {
		return !this.graph.getGrowable().isEmpty();
	}

	public IGraphNode getLongestMatch(final CharSequence text) throws ParseFailedException {
		if (!this.graph.getGoals().isEmpty() && this.graph.getGoals().size() >= 1) {
			IGraphNode lt = this.graph.getGoals().iterator().next();
			for (final IGraphNode gt : this.graph.getGoals()) {
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
		for (final IGraphNode n : this.graph.getCompleteNodes()) {
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
		for (final IGraphNode n : this.graph.getCompleteNodes()) {
			if (n.getStartPosition() == 0) {
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

	public List<IGraphNode> getLastGrown() {
		// List<IGraphNode> longest = new ArrayList<>();
		// for (final IGraphNode n : this.graph.) {
		// if (n.getStartPosition() == 0) {
		// if (null == longest || n.getMatchedTextLength() > longest.getMatchedTextLength()) {
		// longest = n;
		// }
		// }
		// }
		return this.toGrow;
	}

	public void start(final IParseGraph graph, final RuntimeRule goalRule, final Input3 input) {

		final IGraphNode gn = graph.createBranch(goalRule, 0, 0, 0, 0, 0);
		// if (this.getIsGoal(gn)) {
		// this.goals.add(gn);
		// }

	}

	public void grow() throws RuleNotFoundException, ParseTreeException {

		this.toGrow = new ArrayList<>(this.graph.getGrowable());
		this.graph.getGrowable().clear();
		for (final IGraphNode gn : this.toGrow) {

			this.growTreeWidthAndHeight(gn);

		}

	}

	public void growTreeWidthAndHeight(final IGraphNode gn) throws RuleNotFoundException, ParseTreeException {

		final boolean didSkipNode = this.growWidthWithSkipRules(gn);
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
					final int i = 1;
					if (gn.getIsComplete() && !gn.getCanGrowWidth()) {
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

	void growWidth(final IGraphNode gn) throws RuleNotFoundException, ParseTreeException {

		if (gn.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			// List<RuntimeRule> nextExpectedRule = gn.getNextExpectedItem();
			// for(RuntimeRule err: nextExpectedRule) {
			final List<RuntimeRule> expectedNextTerminal = gn.getNextExpectedTerminals();
			for (final RuntimeRule rr : expectedNextTerminal) {
				final Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());
				if (null != l) {
					final IGraphNode bud = this.graph.createLeaf(l, rr, gn.getNextInputPosition(), l.getMatchedTextLength());
					// if (bud.getRuntimeRule().getIsEmptyRule()) {
					// final RuntimeRule ruleThatIsEmpty = bud.getRuntimeRule().getRuleThatIsEmpty();
					// final IGraphNode pt = this.graph.createWithFirstChildAndStack(ruleThatIsEmpty, bud.getPriority(), bud, gn);
					// // // if (this.getIsGoal(pt)) {
					// // // this.goals.add(pt);
					// // // }
					// // final IGraphNode nn = this.pushStackNewRoot(gn, pt);
					// //
					// } else {
					// what if bud exists and already has stacked nodes?
					final IGraphNode nn = this.pushStackNewRoot(gn, bud);

					// }
				}
			}
			// }
			// doing this causes non termination of parser
			// ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
			// buds.add(empty);

		}

	}

	protected boolean growWidthWithSkipRules(final IGraphNode gn) throws RuleNotFoundException {
		final ArrayList<IGraphNode> result = new ArrayList<>();

		if (gn.getCanGrowWidthWithSkip()) { // don't grow width if its complete...cant graft back
			final RuntimeRule[] expectedNextTerminal = this.runtimeRuleSet.getPossibleFirstSkipTerminals();
			for (final RuntimeRule rr : expectedNextTerminal) {
				// TODO: check if this is already growing!
				final Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());

				if (null != l) {
					final IGraphNode bud = this.graph.createLeaf(l, rr, gn.getNextInputPosition(), l.getMatchedTextLength());
					// if (bud.getRuntimeRule().getIsEmptyRule()) {
					// final RuntimeRule ruleThatIsEmpty = bud.getRuntimeRule().getRuleThatIsEmpty();
					// final IGraphNode pt = this.graph.createWithFirstChildAndStack(ruleThatIsEmpty, 0, bud, gn);
					// // final IGraphNode nn = this.pushStackNewRoot(gn, pt);
					// result.add(pt);
					// } else {
					final IGraphNode nn = this.pushStackNewRoot(gn, bud);
					result.add(nn);
					// }

				}
			}

			// TODO: maybe could use smaller subset of terminals here! getTerminalAt(nextExpectedPosition)
			// TODO: maybe don't need this.but maybe we do!

		}
		// TODO: get rid of the arraylist
		return !result.isEmpty();
	}

	protected void tryGraftBack(final IGraphNode gn) throws RuleNotFoundException {

		for (final IGraphNode.PreviousInfo info : gn.getPossibleParent()) {
			if (info.node.hasNextExpectedItem()) {
				this.tryGraftInto(gn, info);
			} else {
				// can't push back
			}
		}

	}

	protected void tryGraftBackSkipNode(final IGraphNode gn) throws RuleNotFoundException {
		for (final IGraphNode.PreviousInfo info : gn.getPossibleParent()) {
			this.tryGraftInto(gn, info);
		}

	}

	private void tryGraftInto(final IGraphNode gn, final IGraphNode.PreviousInfo info) throws RuleNotFoundException {

		if (gn.getIsSkip()) {
			info.node.duplicateWithNextSkipChild(gn);
			// this.graftInto(gn, info);
		} else if (info.node.getExpectsItemAt(gn.getRuntimeRule(), info.atPosition)) {

			this.graftInto(gn, info);

		} else {
			// drop
		}

	}

	private void graftInto(final IGraphNode gn, final IGraphNode.PreviousInfo info) {
		// if parent can have an unbounded number of children, then we can potentially have
		// an infinite number of 'empty' nodes added to it.
		// So check we are not adding the same child as the previous one.
		switch (info.node.getRuntimeRule().getRhs().getKind()) {
			case CHOICE:
				info.node.duplicateWithNextChild(gn);
			break;
			case CONCATENATION:
				info.node.duplicateWithNextChild(gn);
			break;
			case EMPTY:
				info.node.duplicateWithNextChild(gn);
			break;
			case MULTI:
				if (-1 == info.node.getRuntimeRule().getRhs().getMultiMax()) {
					if (0 == info.atPosition) {// info.node.getChildren().isEmpty()) {
						info.node.duplicateWithNextChild(gn);
					} else {
						final IGraphNode previousChild = (IGraphNode) info.node.getChildren().get(info.atPosition - 1);
						if (previousChild.getStartPosition() == gn.getStartPosition()) {
							// trying to add something at same position....don't add it, just drop?
						} else {
							info.node.duplicateWithNextChild(gn);
						}
					}
				} else {
					info.node.duplicateWithNextChild(gn);
				}
			break;
			case PRIORITY_CHOICE:
				info.node.duplicateWithNextChild(gn);
			break;
			case SEPARATED_LIST:
				// TODO: should be ok because we need a separator beteen each item
				info.node.duplicateWithNextChild(gn);
			break;
			default:
			break;

		}
	}

	public void growHeight(final IGraphNode gn) throws RuleNotFoundException, ParseTreeException {
		final ArrayList<IGraphNode> result = new ArrayList<>();
		// TODO: should have already done this test?
		if (gn.getIsComplete()) {
			// if (gn.getRuntimeRule().getIsEmptyRule()) {
			// TODO: I don't think we should be doing this!
			// empty rule should get grafted back into its parent!
			final RuntimeRule ruleThatIsEmpty = gn.getRuntimeRule().getRuleThatIsEmpty();
			// final IGraphNode pt = this.graph.createWithFirstChild(ruleThatIsEmpty, gn.getPriority(), gn);
			// if (this.getIsGoal(pt)) {
			// this.goals.add(pt);
			// }
			// IGraphNode nn = this.pushStackNewRoot(gn, pt);

			// } else {
			// RuntimeRule[] rules = runtimeRuleSet.getPossibleSuperRule(gn.getRuntimeRule());
			final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
			for (final SuperRuleInfo info : infos) {
				if (gn.getRuntimeRule().getRuleNumber() == info.getRuntimeRule().getRuleNumber()) {
					// TODO: do we need to make this growable?
					result.add(gn);
				}
				if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {

					this.growHeightByType(gn, info);

				}
			}
			// }
		} else {
			// result.add(this);
		}

	}

	void growHeightByType(final IGraphNode gn, final SuperRuleInfo info) {
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

	void growHeightChoice(final IGraphNode gn, final SuperRuleInfo info) {

		final RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		for (final RuntimeRule rr : rrs) {
			this.growHeightTree(gn, info);
		}
	}

	void growHeightPriorityChoice(final IGraphNode gn, final SuperRuleInfo info) {
		final RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(gn.getRuntimeRule().getRuleNumber());
		for (final RuntimeRule rr : rrs) {
			this.growHeightTree(gn, info);
		}
	}

	void growHeightConcatenation(final IGraphNode gn, final SuperRuleInfo info) {
		if (0 == info.getRuntimeRule().getRhs().getItems().length) {
			// return new ArrayList<>();
		}
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()) {
			this.growHeightTree(gn, info);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightMulti(final IGraphNode gn, final SuperRuleInfo info) {
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()
				|| 0 == info.getRuntimeRule().getRhs().getMultiMin() && gn.getIsLeaf()) {
			this.growHeightTree(gn, info);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightSeparatedList(final IGraphNode gn, final SuperRuleInfo info) {
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == gn.getRuntimeRule().getRuleNumber()
				|| 0 == info.getRuntimeRule().getRhs().getMultiMin() && gn.getIsLeaf()) {
			this.growHeightTree(gn, info);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightTree(final IGraphNode gn, final SuperRuleInfo info) {
		final int priority = info.getRuntimeRule().getRhsIndexOf(gn.getRuntimeRule());

		// should have already done this test
		if (this.hasHeightPotential(info.getRuntimeRule(), gn)) {

			if (info.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.PRIORITY_CHOICE) {
				final IGraphNode existing = this.graph.findCompleteNode(info.getRuntimeRule().getRuleNumber(), gn.getStartPosition(),
						gn.getMatchedTextLength());
				if (null == existing) {
					// use new
					final IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

				} else {
					// higher priority has a lower number
					// existing must have only one child, because the rule is a prioritychoice
					// existing must be complete or we wouldn't know about it
					// when we created it, it should have got the priority of its child
					final int existingPriority = existing.getPriority();// .getChildren().get(0).getPriority();
					if (existingPriority == priority) {
						if (existing.getMatchedTextLength() > gn.getMatchedTextLength()) {
							// use existing
							final IGraphNode newParent = existing.duplicateWithOtherStack(existingPriority, gn.getPossibleParent());

						} else {
							// use new
							final IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

						}
					} else if (existingPriority > priority) {
						// use new
						final IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

					} else {
						if (existing.getMatchedTextLength() > gn.getMatchedTextLength()) {
							// use existing
							final IGraphNode newParent = existing.duplicateWithOtherStack(existingPriority, gn.getPossibleParent());

						} else {
							// use new
							final IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

						}

					}
				}
			} else {

				final IGraphNode newParent = this.graph.createWithFirstChild(info.getRuntimeRule(), priority, gn);

			}
		} else {
			// return null;
		}

	}

	// int getHeight(IGraphNode n) {
	// int i = 0;
	// while (!n.getChildren().isEmpty()) {
	// i++;
	// n = n.getChildren().get(0);
	// }
	// return i;
	// }

	boolean hasHeightPotential(final RuntimeRule newParentRule, final IGraphNode child) {
		if (newParentRule.couldHaveChild(child.getRuntimeRule(), 0)) {
			if (this.runtimeRuleSet.getAllSkipTerminals().contains(child.getRuntimeRule())) {
				return true;
			} else if (child.getIsStacked()) {
				for (final PreviousInfo prev : child.getPossibleParent()) {
					final List<RuntimeRule> nextExpectedForStacked = prev.node.getNextExpectedItem();
					// if (nextExpectedForStacked.getRuleNumber() == newParentRule.getRuleNumber()) {
					if (nextExpectedForStacked.contains(newParentRule)) {
						return true;
					} else {
						for (final RuntimeRule rr : nextExpectedForStacked) {
							if (rr.getKind() == RuntimeRuleKind.NON_TERMINAL) {
								final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleFirstSubRule(rr));
								if (possibles.contains(newParentRule)) {
									return true;
								}
							} else {
								final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleFirstTerminals(rr));
								if (possibles.contains(newParentRule)) {
									return true;
								}
							}
						}
						return false;
					}
					// SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(child.getRuntimeRule());
					// return this.hasStackedPotential(newParentRule, child.getPrevious().get(0).node.getRuntimeRule());
				}
				return false;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	protected IGraphNode pushStackNewRoot(final IGraphNode gn, final IGraphNode bud) {
		// ParseTreeBud2 bud = this.ffactory.fetchOrCreateBud(leaf);
		// if (this.getHasPotential(bud, Arrays.asList(new IGraphNode.PreviousInfo(gn,gn.getNextItemIndex())), gn.getNextItemIndex())) {
		if (this.hasStackedPotential(bud.getRuntimeRule(), gn.getRuntimeRule())) {
			final IGraphNode nn = gn.pushToStackOf(bud, gn.getNextItemIndex());
			return nn;
		} else {
			return null;
		}
	}

	boolean hasStackedPotential(final RuntimeRule gnRule, final RuntimeRule stackedRule) {
		if (gnRule.getIsSkipRule()) {
			return true;
		}

		if (gnRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
			final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleSubRule(stackedRule));
			final boolean res = possibles.contains(gnRule);
			return res;
		} else if (this.runtimeRuleSet.getAllSkipTerminals().contains(gnRule)) {
			return true;
		} else {
			final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleSubTerminal(stackedRule));
			final boolean res = possibles.contains(gnRule);
			return res;
		}
	}

	@Override
	public String toString() {
		return this.graph.toString();
	}
}
