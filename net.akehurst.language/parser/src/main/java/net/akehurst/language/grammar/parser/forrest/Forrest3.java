package net.akehurst.language.grammar.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.sppt.FixedList;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
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
import net.akehurst.language.parser.sppf.IInput;
import net.akehurst.language.parser.sppf.Leaf;
import net.akehurst.language.parser.sppf.SharedPackedParseTree;

public final class Forrest3 {

    public Forrest3(final IParseGraph graph, final RuntimeRuleSet runtimeRuleSet, final IInput input, final RuntimeRule goalRule) {
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
    IInput input;
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
                final ISharedPackedParseTree last = this.extractLastGrown();
                final Map<String, Integer> location = this.getLineAndColumn(this.input, ((ICompleteNode) last.getRoot()).getNextInputPosition());
                throw new ParseFailedException("Goal does not match full text", last, location);
            } else {
                return lt;
            }
        } else {
            final ISharedPackedParseTree last = this.extractLastGrown();
            final Map<String, Integer> location = this.getLineAndColumn(this.input, ((ICompleteNode) last.getRoot()).getNextInputPosition());
            throw new ParseFailedException("Could not match goal", last, location);
        }
    }

    Map<String, Integer> getLineAndColumn(final IInput input, final int position) {
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

    private ISharedPackedParseTree extractLastGrown() {
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
        complete.getChildrenAlternatives().add((FixedList<ISPNode>) (FixedList<?>) longest.getGrowingChildren());
        return new SharedPackedParseTree((ISPNode) complete);
    }

    private ISharedPackedParseTree extractLongestMatch() {
        if (this.graph.getCompleteNodes().isEmpty()) {
            return null;
        }
        ICompleteNode longest = null;
        for (final ICompleteNode n : this.graph.getCompleteNodes()) {
            if (null == longest || n.getMatchedTextLength() > longest.getMatchedTextLength()) {
                longest = n;
            }
        }
        return new SharedPackedParseTree((ISPNode) longest);
    }

    private ISharedPackedParseTree extractLongestMatchFromStart() {
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
            return new SharedPackedParseTree((ISPNode) longest);
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

    public void start(final IParseGraph graph, final RuntimeRule goalRule, final IInput input) {

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
            this.graph.growNextSkipChild(info.node, complete);
            // info.node.duplicateWithNextSkipChild(gn);
            // this.graftInto(gn, info);
            result |= true;
        } else if (info.node.getExpectsItemAt(gn.getRuntimeRule(), info.atPosition)) {
            final ICompleteNode complete = this.graph.getCompleteNode(gn);
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
            case PRIORITY_CHOICE:
                this.graph.growNextChild(info.node, complete, info.atPosition);
            // info.node.duplicateWithNextChild(gn);
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

    public boolean growHeight(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) throws GrammarRuleNotFoundException, ParseTreeException {
        boolean result = false;
        // TODO: should have already done this test?
        if (gn.getHasCompleteChildren()) {
            final ICompleteNode complete = this.graph.getCompleteNode(gn);
            // TODO: include position
            final SuperRuleInfo[] infos = this.runtimeRuleSet.getPossibleSuperRuleInfo(gn.getRuntimeRule());
            for (final SuperRuleInfo info : infos) {
                if (this.hasHeightPotential(info.getRuntimeRule(), gn, previous)) {
                    // check if already grown into this parent ?
                    final IGraphNode alreadyGrown = null;

                    if (null == alreadyGrown) {
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

    boolean hasHeightPotential(final RuntimeRule newParentRule, final IGrowingNode child, final Set<IGrowingNode.PreviousInfo> previous) {
        // if (newParentRule.couldHaveChild(child.getRuntimeRule(), 0)) {
        // if (this.runtimeRuleSet.getAllSkipTerminals().contains(child.getRuntimeRule())) {
        if (this.runtimeRuleSet.isSkipTerminal(child.getRuntimeRule())) {
            return true;
        } else if (!previous.isEmpty()) {
            for (final IGrowingNode.PreviousInfo prev : previous) {
                if (prev.node.hasNextExpectedItem()) {
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
                for (final RuntimeRule expectedRule : stack.getNextExpectedItem()) {

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
