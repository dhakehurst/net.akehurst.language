package net.akehurst.language.grammar.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.log.Log;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet.SuperRuleInfo;
import net.akehurst.language.parse.graph.GraphNodeBranch;
import net.akehurst.language.parse.graph.ICompleteNode;
import net.akehurst.language.parse.graph.IGraphNode;
import net.akehurst.language.parse.graph.IGrowingNode;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parser.sppf.Input;
import net.akehurst.language.parser.sppf.Leaf;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public final class Forrest3 {

	public Forrest3(final IParseGraph graph, final RuntimeRuleSet runtimeRuleSet, final Input input, final RuntimeRule goalRule) {
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
	Input input;
	List<IGrowingNode> toGrow;

	public boolean getCanGrow() {
		return !this.graph.getGrowingHead().isEmpty();
	}

	public ICompleteNode getLongestMatch() throws ParseFailedException {
		if (!this.graph.getGoals().isEmpty() && this.graph.getGoals().size() >= 1) {
			ICompleteNode lt = this.graph.getGoals().iterator().next();
			for (final ICompleteNode gt : this.graph.getGoals()) {
				if (gt.getMatchedTextLength() > lt.getMatchedTextLength()) {
					lt = gt;
				}
			}
			if (!this.input.getIsEnd(lt.getNextInputPosition() + 1)) {
				final SharedPackedParseTree last = this.extractLastGrown();
				final Map<String, Integer> location = this.getLineAndColumn(this.input, ((ICompleteNode) last.getRoot()).getNextInputPosition());
				throw new ParseFailedException("Goal does not match full text", last, location);
			} else {
				return lt;
			}
		} else {
			final SharedPackedParseTree last = this.extractLastGrown();
			final Map<String, Integer> location = this.getLineAndColumn(this.input, ((ICompleteNode) last.getRoot()).getNextInputPosition());
			throw new ParseFailedException("Could not match goal", last, location);
		}
	}

	Map<String, Integer> getLineAndColumn(final Input input, final int position) {
		final Map<String, Integer> result = new HashMap<>();
		int line = 1;
		int column = 1;

		for (int count = 0; count < position; ++count) {
			if (input.getText().charAt(count) == '\n') {
				++line;
				column = 1;
			} else {
				++column;
			}
		}

		result.put("line", line);
		result.put("column", column);
		return result;
	}

	private SharedPackedParseTree extractLastGrown() {
		if (this.getLastGrown().isEmpty()) {
			return null;
		}
		IGrowingNode longest = null;
		for (final IGrowingNode n : this.getLastGrown()) {
			if (null == longest || n.getNextInputPosition() > longest.getNextInputPosition()) {
				longest = n;
			}
		}
		// TODO: gorwing node is not really complete
		final ICompleteNode complete = new GraphNodeBranch(this.graph, longest.getRuntimeRule(), longest.getPriority(), longest.getStartPosition(),
				longest.getNextInputPosition());
		complete.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) longest.getGrowingChildren());
		return new SharedPackedParseTreeSimple((SPPTNode) complete);
	}

	private SharedPackedParseTree extractLongestMatch() {
		if (this.graph.getCompleteNodes().isEmpty()) {
			return null;
		}
		ICompleteNode longest = null;
		for (final ICompleteNode n : this.graph.getCompleteNodes()) {
			if (null == longest || n.getMatchedTextLength() > longest.getMatchedTextLength()) {
				longest = n;
			}
		}
		return new SharedPackedParseTreeSimple((SPPTNode) longest);
	}

	private SharedPackedParseTree extractLongestMatchFromStart() {
		if (this.graph.getCompleteNodes().isEmpty()) {
			return null;
		}
		ICompleteNode longest = null;
		for (final ICompleteNode n : this.graph.getCompleteNodes()) {
			if (n.getStartPosition() == 0) {
				if (null == longest || n.getMatchedTextLength() > longest.getMatchedTextLength()) {
					longest = n;
				}
			}
		}
		if (null == longest) {
			return this.extractLongestMatch();
		} else {
			return new SharedPackedParseTreeSimple((SPPTNode) longest);
		}
	}

	public Collection<IGrowingNode> getLastGrown() {
		if (!this.graph.getGrowing().isEmpty()) {
			return this.graph.getGrowing();
		} else {
			return this.toGrow;
		}
		// return this.toGrow;
	}

	public void start(final IParseGraph graph, final RuntimeRule goalRule, final Input input) {

		graph.createStart(goalRule);

	}

	public void grow() throws GrammarRuleNotFoundException, ParseTreeException {

		this.toGrow = new ArrayList<>(this.graph.getGrowingHead());
		this.graph.getGrowingHead().clear();
		for (final IGrowingNode gn : this.toGrow) {
			if (Log.on) {
				Log.traceln("    %s", gn.toStringTree(true, false));
			}
			this.growTreeWidthAndHeight(gn);

		}

	}

	public void growTreeWidthAndHeight(final IGrowingNode gn) throws GrammarRuleNotFoundException, ParseTreeException {
		// gn.toString();

		final Set<IGrowingNode.PreviousInfo> previous = this.graph.pop(gn);

		final boolean didSkipNode = this.growWidthWithSkipRules(gn, previous);
		if (didSkipNode) {
			return;
		} else {
			if (gn.isSkip()) {
				this.tryGraftBackSkipNode(gn, previous);
				// this.graph.pop(gn);
			} else {
				// TODO: need to find a way to do either height or graft..not both, maybe!
				// problem is deciding which
				final boolean grownHeight = this.growHeight(gn, previous);

				boolean graftBack = false;
				// reduce
				for (final IGrowingNode.PreviousInfo prev : previous) {
					if (gn.getCanGraftBack(prev)) { // if hascompleteChildren && isStacked && prevInfo is valid
						graftBack = this.tryGraftBack(gn, prev);
					}
				}

				// maybe only shift if not done either of above!
				// tomitas original does that!
				// shift
				final boolean grownWidth = this.growWidth(gn, previous);

				// if (!grownWidth && !grownHeight && !graftBack) {
				// // if not done anything with the previous nodes, make them heads
				// for (final IGrowingNode.PreviousInfo info : previous) {
				// this.graph.makeHead(info.node);
				// }
				// }
				if (!grownHeight && !graftBack && !grownWidth) {
					if (Log.on) {
						Log.traceln("drop %s", gn);
					}
				}
			}
		}
	}

	boolean growWidth(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean modified = false;
		if (gn.getCanGrowWidth()) { // don't grow width if its complete...cant graft back
			final List<RuntimeRule> expectedNextTerminal = gn.getNextExpectedTerminals();
			final Set<RuntimeRule> setNextExpected = new HashSet<>(expectedNextTerminal);
			for (final RuntimeRule rr : setNextExpected) {
				final Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());
				if (null != l) {
					final ICompleteNode bud = this.graph.findOrCreateLeaf(l);
					modified = this.pushStackNewRoot(bud, gn, previous);
				}
			}
		}
		return modified;
	}

	protected boolean growWidthWithSkipRules(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException {
		boolean modified = false;
		if (gn.getCanGrowWidthWithSkip()) { // don't grow width if its complete...cant graft back
			final RuntimeRule[] expectedNextTerminal = this.runtimeRuleSet.getPossibleFirstSkipTerminals();
			for (final RuntimeRule rr : expectedNextTerminal) {
				final Leaf l = this.input.fetchOrCreateBud(rr, gn.getNextInputPosition());
				if (null != l) {
					final ICompleteNode bud = this.graph.findOrCreateLeaf(l);
					modified = this.pushStackNewRoot(bud, gn, previous);
				}
			}
		}
		return modified;
	}

	protected boolean tryGraftBack(final IGrowingNode gn, final IGrowingNode.PreviousInfo info) throws GrammarRuleNotFoundException {
		boolean result = false;
		// TODO: perhaps should return list of those who are not grafted!
		// for (final IGrowingNode.PreviousInfo info : previous) {
		if (info.node.hasNextExpectedItem()) {
			result |= this.tryGraftInto(gn, info);
		} else {
			// can't push back
			result |= false;
		}
		// }
		return result;
	}

	protected void tryGraftBackSkipNode(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException {
		for (final IGrowingNode.PreviousInfo info : previous) {
			this.tryGraftInto(gn, info);
		}

	}

	private boolean tryGraftInto(final IGrowingNode gn, final IGrowingNode.PreviousInfo info) throws GrammarRuleNotFoundException {
		boolean result = false;
		if (gn.isSkip()) {
			// TODO: why is this code so different to that in the next option?
			final ICompleteNode complete = this.graph.getCompleteNode(gn);
			// complete will not be null because we do not graftback unless gn has complete children
			// and graph will try to 'complete' a GraphNode when it is created.
			this.graph.growNextSkipChild(info.node, complete);
			// info.node.duplicateWithNextSkipChild(gn);
			// this.graftInto(gn, info);
			result |= true;
		} else if (info.node.getExpectsItemAt(gn.getRuntimeRule(), info.atPosition)) {
			final ICompleteNode complete = this.graph.getCompleteNode(gn);
			// complete will not be null because we do not graftback unless gn has complete children
			// and graph will try to 'complete' a GraphNode when it is created.
			this.graftInto(complete, info);
			result |= true;
		} else {
			// drop
			result |= false;
		}
		return result;
	}

	private void graftInto(final ICompleteNode complete, final IGrowingNode.PreviousInfo info) {
		// if parent can have an unbounded number of children, then we can potentially have
		// an infinite number of 'empty' nodes added to it.
		// So check we are not adding the same child as the previous one.
		switch (info.node.getRuntimeRule().getRhs().getKind()) {
			case CHOICE:
				this.graph.growNextChild(info.node, complete, info.atPosition);
			// info.node.duplicateWithNextChild(gn);
			break;
			case PRIORITY_CHOICE:
				this.graph.growNextChild(info.node, complete, info.atPosition);
			// info.node.duplicateWithNextChild(gn);
			break;
			case CONCATENATION:
				this.graph.growNextChild(info.node, complete, info.atPosition);
			// info.node.duplicateWithNextChild(gn);
			break;
			case EMPTY:
				this.graph.growNextChild(info.node, complete, info.atPosition);
			// info.node.duplicateWithNextChild(gn);
			break;
			case MULTI:
				if (-1 == info.node.getRuntimeRule().getRhs().getMultiMax()) {
					if (0 == info.atPosition) {// info.node.getChildren().isEmpty()) {
						this.graph.growNextChild(info.node, complete, info.atPosition);
						// info.node.duplicateWithNextChild(gn);
					} else {
						// final IGraphNode previousChild = (IGraphNode) info.node.getGrowingChildren().get(info.atPosition - 1);
						// if (previousChild.getStartPosition() == gn.getStartPosition()) {
						// // trying to add something at same position....don't add it, just drop?
						// } else {
						this.graph.growNextChild(info.node, complete, info.atPosition);
						// info.node.duplicateWithNextChild(gn);
						// }
					}
				} else {
					this.graph.growNextChild(info.node, complete, info.atPosition);
					// info.node.duplicateWithNextChild(gn);
				}
			break;

			case SEPARATED_LIST:
				// TODO: should be ok because we need a separator between each item
				this.graph.growNextChild(info.node, complete, info.atPosition);
			// info.node.duplicateWithNextChild(gn);
			break;
			default:
			break;

		}
	}

	// 1
	private boolean growHeight1(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean result = false;

		if (gn.getHasCompleteChildren()) {

			// TODO: include position
			final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
			for (final SuperRuleInfo info : infos) {
				if (this.hasHeightPotential(info.getRuntimeRule(), gn.getRuntimeRule(), previous)) {
					// check if already grown into this parent ?
					final IGraphNode alreadyGrown = null;

					if (null == alreadyGrown) {
						final ICompleteNode complete = this.graph.getCompleteNode(gn);
						this.growHeightByType(complete, info, previous);
						result |= true; // TODO: this should depend on if the growHeight does something
					} else {
					}
				}
			}

		} else {
			// do nothing
		}
		return result;
	}

	// 1b
	private boolean growHeight1b(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean result = false;

		if (gn.getHasCompleteChildren()) {

			// TODO: include position
			final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
			for (final SuperRuleInfo info : infos) {
				boolean hp;
				final RuntimeRule newParentRule = info.getRuntimeRule();
				final RuntimeRule childRule = gn.getRuntimeRule();
				hp = this.hasHeightPotential(newParentRule, childRule, previous);
				if (hp) {
					// check if already grown into this parent ?
					final IGraphNode alreadyGrown = null;

					if (null == alreadyGrown) {
						final ICompleteNode complete = this.graph.getCompleteNode(gn);
						this.growHeightByType(complete, info, previous);
						result |= true; // TODO: this should depend on if the growHeight does something
					} else {
					}
				}
			}

		} else {
			// do nothing
		}
		return result;
	}

	// 1 c
	// public boolean growHeight1c(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException
	// {
	// boolean result = false;
	//
	// if (gn.getHasCompleteChildren()) {
	// final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
	// for (final SuperRuleInfo info : infos) {
	// // boolean hp1 = false;
	// boolean hp2 = false;
	// final RuntimeRule newParentRule = info.getRuntimeRule();
	// final RuntimeRule childRule = gn.getRuntimeRule();
	//
	// if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
	// // hp1 = true;
	// hp2 = true;
	// } else if (!previous.isEmpty()) {
	// for (final IGrowingNode.PreviousInfo prev : previous) {
	// if (prev.node.hasNextExpectedItem()) {
	// // final List<RuntimeRule> nextExpectedForStacked = prev.node.getNextExpectedItem();
	// // if (nextExpectedForStacked.contains(newParentRule)) {
	// // hp1 = true;
	// // } else {
	// // for (final RuntimeRule rr : nextExpectedForStacked) {
	// // if (rr.getKind() == RuntimeRuleKind.NON_TERMINAL) {
	// // final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleFirstSubRule(rr));
	// // if (possibles.contains(newParentRule)) {
	// // hp1 = true;
	// // break;
	// // }
	// // } else {
	// // final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleFirstTerminals(rr));
	// // if (possibles.contains(newParentRule)) {
	// // hp1 = true;
	// // break;
	// // }
	// // }
	// // }
	// // }
	// // TODO: need to use nextInputPosition here, rather than nextItemIndex, which is always 0!!
	// final int nextItemIndex = prev.node.getNextItemIndex();
	// hp2 |= this.runtimeRuleSet.doHeight(newParentRule, prev.node.getRuntimeRule(), nextItemIndex);
	// } else {
	//
	// }
	// }
	// } else {
	// // hp1 = false;
	// hp2 = false;
	// }
	//
	// // if (hp1 != hp2) {
	// // System.out.println("different!!");
	// // }
	//
	// if (hp2) {
	// // check if already grown into this parent ?
	// final IGraphNode alreadyGrown = null;
	//
	// if (null == alreadyGrown) {
	// final ICompleteNode complete = this.graph.getCompleteNode(gn);
	// this.growHeightByType(complete, info, previous);
	// result |= true; // TODO: this should depend on if the growHeight does something
	// } else {
	// }
	// }
	// }
	//
	// } else {
	// // do nothing
	// }
	// return result;
	// }
	//
	// public boolean growHeight1d(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException
	// {
	// boolean result = false;
	// if (gn.getHasCompleteChildren()) {
	// final RuntimeRule childRule = gn.getRuntimeRule();
	// // what can gn growHeight directly into
	// final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
	// for (final SuperRuleInfo info : infos) {
	// boolean hp2 = false;
	// if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
	// hp2 = true;
	// } else if (!previous.isEmpty()) {
	// for (final IGrowingNode.PreviousInfo prev : previous) {
	// if (prev.node.hasNextExpectedItem()) {
	// // TODO: need to use nextRuleItemIndex! here, rather than nextItemIndex.
	// final int nextItemIndex = prev.node.getNextItemIndex();
	// final RuntimeRule newParentRule = info.getRuntimeRule();
	// hp2 |= this.runtimeRuleSet.doHeight(newParentRule, prev.node.getRuntimeRule(), nextItemIndex);
	// } else {
	//
	// }
	// }
	// } else {
	// hp2 = false;
	// }
	// if (hp2) {
	// final ICompleteNode complete = this.graph.getCompleteNode(gn);
	// this.growHeightByType(complete, info, previous);
	// result |= true; // TODO: this should depend on if the growHeight does something
	// }
	// }
	// } else {
	// // do nothing
	// }
	// return result;
	// }
	//
	// public boolean growHeight1e(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException
	// {
	// boolean result = false;
	// if (gn.getHasCompleteChildren()) {
	// final RuntimeRule childRule = gn.getRuntimeRule();
	// if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
	// final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(childRule);
	// for (final SuperRuleInfo info : infos) {
	// final ICompleteNode complete = this.graph.getCompleteNode(gn);
	// this.growHeightByType(complete, info, previous);
	// result |= true; // TODO: this should depend on if the growHeight does something
	// }
	// } else {
	// if (!previous.isEmpty()) {
	// final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(childRule);
	// for (final SuperRuleInfo info : infos) {
	// boolean hp2 = false;
	// for (final IGrowingNode.PreviousInfo prev : previous) {
	// if (prev.node.hasNextExpectedItem()) {
	// // TODO: need to use nextRuleItemIndex! here, rather than nextItemIndex.
	// final int nextItemIndex = prev.node.getNextItemIndex();
	// final RuntimeRule newParentRule = info.getRuntimeRule();
	// hp2 |= this.runtimeRuleSet.doHeight(newParentRule, prev.node.getRuntimeRule(), nextItemIndex);
	// } else {
	//
	// }
	// }
	//
	// if (hp2) {
	// final ICompleteNode complete = this.graph.getCompleteNode(gn);
	// this.growHeightByType(complete, info, previous);
	// result |= true; // TODO: this should depend on if the growHeight does something
	// }
	// }
	// } else {
	// // do nothing
	// }
	// }
	// } else {
	// // do nothing
	// }
	// return result;
	// }

	public boolean growHeight1f(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean result = false;
		if (gn.getHasCompleteChildren()) {
			final RuntimeRule childRule = gn.getRuntimeRule();
			if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
				final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(childRule);
				for (final SuperRuleInfo info : infos) {
					final ICompleteNode complete = this.graph.getCompleteNode(gn);
					this.growHeightByType(complete, info, previous);
					result |= true; // TODO: this should depend on if the growHeight does something
				}
			} else {
				if (previous.isEmpty()) {
					// do nothing
				} else {
					final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(childRule);
					final Set<SuperRuleInfo> toGrow = new HashSet<>();
					for (final IGrowingNode.PreviousInfo prev : previous) {
						if (prev.node.hasNextExpectedItem()) {
							final int prevItemIndex = prev.node.getNextItemIndex();
							final RuntimeRule prevRule = prev.node.getRuntimeRule();
							for (final SuperRuleInfo info : infos) {
								final RuntimeRule newParentRule = info.getRuntimeRule();
								final boolean hp2 = this.runtimeRuleSet.doHeight(newParentRule, prevRule, prevItemIndex);
								if (hp2) {
									toGrow.add(info);
								}
							}
						} else {

						}
					}

					for (final SuperRuleInfo info : toGrow) {
						final ICompleteNode complete = this.graph.getCompleteNode(gn);
						this.growHeightByType(complete, info, previous);
						result |= true; // TODO: this should depend on if the growHeight does something
					}
				}
			}
		} else {
			// do nothing
		}
		return result;
	}

	public boolean growHeight(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean result = false;
		if (gn.getHasCompleteChildren()) {
			final RuntimeRule childRule = gn.getRuntimeRule();
			if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
				final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(childRule);
				for (final SuperRuleInfo info : infos) {
					final ICompleteNode complete = this.graph.getCompleteNode(gn);
					this.growHeightByType(complete, info, previous);
					result |= true; // TODO: this should depend on if the growHeight does something
				}
			} else {
				if (previous.isEmpty()) {
					// do nothing
				} else {
					final Set<SuperRuleInfo> toGrow = new HashSet<>();
					for (final IGrowingNode.PreviousInfo prev : previous) {
						final int prevItemIndex = prev.node.getNextItemIndex();
						final RuntimeRule prevRule = prev.node.getRuntimeRule();
						toGrow.addAll(this.runtimeRuleSet.growsInto(childRule, prevRule, prevItemIndex));
					}
					for (final SuperRuleInfo info : toGrow) {
						final ICompleteNode complete = this.graph.getCompleteNode(gn);
						this.growHeightByType(complete, info, previous);
						result |= true; // TODO: this should depend on if the growHeight does something
					}
				}
			}
		} else {
			// do nothing
		}
		return result;
	}

	// 2
	public boolean growHeight2(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean result = false;

		if (gn.getHasCompleteChildren()) {
			for (final IGrowingNode.PreviousInfo prev : previous) {
				final int nextItemIndex = prev.node.getNextItemIndex();
				final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
				for (final SuperRuleInfo info : infos) {
					if (this.hasHeightPotential2(info.getRuntimeRule(), gn.getRuntimeRule(), prev)) {
						final ICompleteNode complete = this.graph.getCompleteNode(gn);
						this.growHeightByType(complete, info, previous);
						result |= true; // TODO: this should depend on if the growHeight does something
					}
				}
			}
		} else {
			// do nothing
		}
		return result;
	}

	// 3
	private boolean growHeight3(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
		boolean result = false;
		if (gn.getHasCompleteChildren()) {
			for (final IGrowingNode.PreviousInfo prev : previous) {
				final int nextItemIndex = prev.node.getNextItemIndex();
				final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
				for (final SuperRuleInfo info : infos) {
					boolean doIt = false;

					if (this.runtimeRuleSet.isSkipTerminal(gn.getRuntimeRule())) {
						doIt = true;
					} else {
						doIt = this.runtimeRuleSet.doHeight(info.getRuntimeRule(), prev.node.getRuntimeRule(), nextItemIndex);
					}
					if (doIt) {
						final ICompleteNode complete = this.graph.getCompleteNode(gn);
						this.growHeightByType(complete, info, previous);
						result |= true; // TODO: this should depend on if the growHeight does something
					}
				}
			}
		} else {
			// do nothing
		}
		return result;
	}

	void growHeightByType(final ICompleteNode gn, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous) {
		switch (info.getRuntimeRule().getRhs().getKind()) {
			case CHOICE:
				this.growHeightChoice(gn, info, previous);
				return;
			case PRIORITY_CHOICE:
				this.growHeightPriorityChoice(gn, info, previous);
				return;
			case CONCATENATION:
				this.growHeightConcatenation(gn, info, previous);
				return;
			case MULTI:
				this.growHeightMulti(gn, info, previous);
				return;
			case SEPARATED_LIST:
				this.growHeightSeparatedList(gn, info, previous);
				return;
			case EMPTY:
				throw new RuntimeException(
						"Internal Error: Should never have called grow on an EMPTY Rule (growMe is called as there should only be one growth option)");
			default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	void growHeightChoice(final ICompleteNode complete, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous) {

		final RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(complete.getRuntimeRule().getRuleNumber());
		for (final RuntimeRule rr : rrs) {
			this.growHeightTree(complete, info, previous, 0);
		}
	}

	void growHeightPriorityChoice(final ICompleteNode complete, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous) {
		final RuntimeRule[] rrs = info.getRuntimeRule().getRhs().getItems(complete.getRuntimeRule().getRuleNumber());
		final int priority = info.getRuntimeRule().getRhsIndexOf(complete.getRuntimeRule());
		for (final RuntimeRule rr : rrs) {
			this.growHeightTree(complete, info, previous, priority);
		}
	}

	void growHeightConcatenation(final ICompleteNode complete, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous) {
		if (0 == info.getRuntimeRule().getRhs().getItems().length) {
			// return new ArrayList<>();
		}
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == complete.getRuntimeRule().getRuleNumber()) {
			this.growHeightTree(complete, info, previous, 0);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightMulti(final ICompleteNode complete, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous) {
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == complete.getRuntimeRule().getRuleNumber()
				|| 0 == info.getRuntimeRule().getRhs().getMultiMin() && complete.isLeaf()) {
			this.growHeightTree(complete, info, previous, 0);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightSeparatedList(final ICompleteNode complete, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous) {
		if (info.getRuntimeRule().getRhsItem(0).getRuleNumber() == complete.getRuntimeRule().getRuleNumber()
				|| 0 == info.getRuntimeRule().getRhs().getMultiMin() && complete.isLeaf()) {
			this.growHeightTree(complete, info, previous, 0);
		} else {
			// return new ArrayList<>();
		}
	}

	void growHeightTree(final ICompleteNode complete, final SuperRuleInfo info, final Set<IGrowingNode.PreviousInfo> previous, final int priority) {
		this.graph.createWithFirstChild(info.getRuntimeRule(), priority, complete, previous);
	}

	boolean hasHeightPotential(final RuntimeRule newParentRule, final RuntimeRule childRule, final Set<IGrowingNode.PreviousInfo> previous) {
		// if (newParentRule.couldHaveChild(child.getRuntimeRule(), 0)) {
		// if (this.runtimeRuleSet.getAllSkipTerminals().contains(child.getRuntimeRule())) {
		if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
			return true;
		} else if (!previous.isEmpty()) {
			for (final IGrowingNode.PreviousInfo prev : previous) {
				if (prev.node.hasNextExpectedItem()) {
					final Set<RuntimeRule> nextExpectedForStacked = prev.node.getNextExpectedItems();
					// if (nextExpectedForStacked.getRuleNumber() == newParentRule.getRuleNumber()) {
					if (nextExpectedForStacked.contains(newParentRule)) {
						return true;
					} else {
						for (final RuntimeRule rr : nextExpectedForStacked) {
							if (rr.getKind() == RuntimeRuleKind.NON_TERMINAL) {
								final Set<RuntimeRule> possibles = this.runtimeRuleSet.getPossibleFirstSubRule(rr);
								if (possibles.contains(newParentRule)) {
									return true;
								}
							} else {
								final Set<RuntimeRule> possibles = this.runtimeRuleSet.getPossibleFirstTerminals(rr);
								if (possibles.contains(newParentRule)) {
									return true;
								}
							}
						}
						// return false;
					}
				} else {
					// do nothing
				}
				// SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(child.getRuntimeRule());
				// return this.hasStackedPotential(newParentRule, child.getPrevious().get(0).node.getRuntimeRule());
			}
			return false;
		} else {
			return false;
		}
		// } else {
		// return false;
		// }
	}

	boolean hasHeightPotential2(final RuntimeRule newParentRule, final RuntimeRule childRule, final IGrowingNode.PreviousInfo prev) {
		// if (newParentRule.couldHaveChild(child.getRuntimeRule(), 0)) {
		// if (this.runtimeRuleSet.getAllSkipTerminals().contains(child.getRuntimeRule())) {
		boolean hp;
		if (this.runtimeRuleSet.isSkipTerminal(childRule)) {
			hp = true;
		} else {
			if (prev.node.hasNextExpectedItem()) {
				final Set<RuntimeRule> nextExpectedForStacked = prev.node.getNextExpectedItems();
				// if (nextExpectedForStacked.getRuleNumber() == newParentRule.getRuleNumber()) {
				if (nextExpectedForStacked.contains(newParentRule)) {
					hp = true;
				} else {
					for (final RuntimeRule rr : nextExpectedForStacked) {
						if (rr.getKind() == RuntimeRuleKind.NON_TERMINAL) {
							final Set<RuntimeRule> possibles = this.runtimeRuleSet.getPossibleFirstSubRule(rr);
							if (possibles.contains(newParentRule)) {
								hp = true;
								break;
							}
						} else {
							final Set<RuntimeRule> possibles = this.runtimeRuleSet.getPossibleFirstTerminals(rr);
							if (possibles.contains(newParentRule)) {
								hp = true;
								break;
							}
						}
					}
					hp = false;
				}
			} else {
				hp = false;
			}
			// SuperRuleInfo[] infos = runtimeRuleSet.getPossibleSuperRuleInfo(child.getRuntimeRule());
			// return this.hasStackedPotential(newParentRule, child.getPrevious().get(0).node.getRuntimeRule());
		}
		// } else {
		// return false;
		// }
		return hp;
	}

	protected boolean pushStackNewRoot(final ICompleteNode leafNode, final IGrowingNode stack, final Set<IGrowingNode.PreviousInfo> previous) {
		// ParseTreeBud2 bud = this.ffactory.fetchOrCreateBud(leaf);
		// if (this.getHasPotential(bud, Arrays.asList(new IGraphNode.PreviousInfo(gn,gn.getNextItemIndex())), gn.getNextItemIndex())) {
		boolean modified = false;

		// for (final ICompleteNode ns : leafNode.getPossibleParent()) {
		// // TODO: this test could be more restrictive using the position
		//
		// if (this.hasStackedPotential(ns, stack)) {
		// this.graph.pushToStackOf(leafNode, stack);
		// // stack.pushToStackOf(ns, stack.getNextItemIndex());
		// modified = true;
		// } else {
		// // do nothing
		// }
		// }

		if (modified) {
			// do nothing we have pushed
		} else {
			// no existing parent was suitable, use newRoot
			if (this.hasStackedPotential(leafNode, stack)) {
				this.graph.pushToStackOf(leafNode, stack, previous);
				// stack.pushToStackOf(newRoot, stack.getNextItemIndex());
				modified = true;
			}
		}

		return modified;
	}

	boolean hasStackedPotential(final ICompleteNode node, final IGrowingNode stack) {
		if (node.isSkip()) {
			return true;
		} else {
			// if node is nextexpected item on stack, or could grow into nextexpected item
			if (stack.hasNextExpectedItem()) {
				for (final RuntimeRule expectedRule : stack.getNextExpectedItems()) {

					if (node.getRuntimeRuleNumber() == expectedRule.getRuleNumber()) {
						// if node is nextexpected item on stack
						return true;
					} else {
						// or node is a possible subrule of nextexpected item
						if (node.getRuntimeRule().getKind() == RuntimeRuleKind.NON_TERMINAL) {
							final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleSubRule(expectedRule));
							final boolean res = possibles.contains(node.getRuntimeRule());
							if (res) {
								return true;
							}
						} else {
							final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleSubTerminal(expectedRule));
							final boolean res = possibles.contains(node.getRuntimeRule());
							if (res) {
								return true;
							}
						}
					}
				}
				return false;
			} else if (this.runtimeRuleSet.getAllSkipTerminals().contains(node.getRuntimeRule())) {
				return true;
			} else {
				return false;
			}

			// return stack.getExpectsItemAt(newRoot.getRuntimeRule(), stack.getNextItemIndex());
		}

		// if (gnRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
		// final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleSubRule(stackedRule));
		// final boolean res = possibles.contains(gnRule);
		// return res;
		// } else if (this.runtimeRuleSet.getAllSkipTerminals().contains(gnRule)) {
		// return true;
		// } else {
		// final List<RuntimeRule> possibles = Arrays.asList(this.runtimeRuleSet.getPossibleSubTerminal(stackedRule));
		// final boolean res = possibles.contains(gnRule);
		// return res;
		// }
	}

	@Override
	public String toString() {
		return this.graph.toString();
	}
}
