package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.grammar.parser.log.Log;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.parser.sppf.FixedLists;
import net.akehurst.language.parser.sppf.IInput;
import net.akehurst.language.parser.sppf.Leaf;

public class ParseGraph implements IParseGraph {

    // TODO: remove, this is for test
    static boolean with = true;

    public ParseGraph(final RuntimeRule goalRule, final IInput input) {
        this.goalRule = goalRule;
        // this.inputLength = inputLength;
        this.input = input;
        this.nodes = new HashMap<>();
        this.leaves = new HashMap<>();
        this.growingHead = new HashMap<>();
        this.growing = new HashMap<>();
        this.goals = new ArrayList<>();
    }

    private final RuntimeRule goalRule;
    // private final int inputLength;
    private final IInput input;
    private final List<ICompleteNode> goals;

    public static final class NodeIndex {
        public NodeIndex(final int ruleNumber, final int startPosition, final int nextInputPosition) {
            this.ruleNumber = ruleNumber;
            this.startPosition = startPosition;
            this.nextInputPosition = nextInputPosition;
            this.hashCode_cache = Objects.hash(ruleNumber, startPosition, nextInputPosition);
        }

        int ruleNumber;
        int startPosition;
        int nextInputPosition;

        int hashCode_cache;

        @Override
        public int hashCode() {
            return this.hashCode_cache;
        }

        @Override
        public boolean equals(final Object arg) {
            if (!(arg instanceof NodeIndex)) {
                return false;
            }
            final NodeIndex other = (NodeIndex) arg;
            return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.nextInputPosition == other.nextInputPosition;
        }

        @Override
        public String toString() {
            return "(".concat(Integer.toString(this.ruleNumber)).concat(",").concat(Integer.toString(this.startPosition)).concat(",")
                    .concat(Integer.toString(this.nextInputPosition)).concat(")");
        }
    }

    private final Map<NodeIndex, ICompleteNode> nodes;

    public static final class GrowingNodeIndex {
        public GrowingNodeIndex(final int ruleNumber, final int startPosition, final int nextInputPosition, final int nextItemIndex) {
            this.ruleNumber = ruleNumber;
            this.startPosition = startPosition;
            this.nextInputPosition = nextInputPosition;
            this.nextItemIndex = nextItemIndex;
            this.hashCode_cache = Objects.hash(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        }

        private final int ruleNumber;
        private final int startPosition;
        private final int nextInputPosition;
        private final int nextItemIndex;
        private final int hashCode_cache;

        @Override
        public int hashCode() {
            return this.hashCode_cache;
        }

        @Override
        public boolean equals(final Object arg) {
            if (!(arg instanceof GrowingNodeIndex)) {
                return false;
            }
            final GrowingNodeIndex other = (GrowingNodeIndex) arg;
            return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.nextInputPosition == other.nextInputPosition
                    && this.nextItemIndex == other.nextItemIndex;
        }

        @Override
        public String toString() {
            final StringBuilder b = new StringBuilder();
            b.append('(');
            b.append(this.ruleNumber);
            b.append(',');
            b.append(this.startPosition);
            b.append(',');
            b.append(this.nextInputPosition);
            b.append(',');
            b.append(this.nextItemIndex);
            // b.append(',');
            // b.append(this.hasStack);
            // b.append(',');
            // b.append(this.stackHash);
            b.append(')');
            return b.toString();
        }
    }

    private final Map<GrowingNodeIndex, IGrowingNode> growingHead;
    private final Map<GrowingNodeIndex, IGrowingNode> growing;

    private final Map<NodeIndex, ICompleteNode> leaves;

    @Override
    public List<ICompleteNode> getGoals() {
        return this.goals;
    }

    @Override
    public Collection<ICompleteNode> getCompleteNodes() {
        return this.nodes.values();
    }

    @Override
    public ICompleteNode getCompleteNode(final IGrowingNode gn) {
        final RuntimeRule runtimeRule = gn.getRuntimeRule();
        final int priority = gn.getPriority();
        final int startPosition = gn.getStartPosition();
        final int nextInputPosition = gn.getNextInputPosition();
        final ICompleteNode cn = this.findNode(runtimeRule.getRuleNumber(), startPosition, nextInputPosition);
        return cn;
    }

    @Override
    public ICompleteNode findNode(final int ruleNumber, final int start, final int nextInputPosition) {
        final NodeIndex id = new NodeIndex(ruleNumber, start, nextInputPosition);// , l.getEnd(), -1);
        final ICompleteNode gn = this.nodes.get(id);
        return gn;
    }

    @Override
    public Collection<IGrowingNode> getGrowingHead() {
        return this.growingHead.values();
    }

    private void checkForGoal(final ICompleteNode node) {
        if (this.getIsGoal(node)) {
            // TODO: maybe need to not have duplicates!
            this.goals.add(node);
        }
    }

    private void findOrCreateGrowingLeaf(final ICompleteNode leafNode, final IGrowingNode stack, final Set<IGrowingNode.PreviousInfo> previous) {
        this.addGrowing(stack, previous);
        // TODO: remove, this is for test
        if (ParseGraph.with) {
            for (final IGrowingNode.PreviousInfo info : previous) {
                this.addGrowing(info.node);
            }
        }
        final int ruleNumber = leafNode.getRuntimeRuleNumber();
        final int startPosition = leafNode.getStartPosition();
        final int nextInputPosition = leafNode.getNextInputPosition();
        final int nextItemIndex = -1;
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        final IGrowingNode existing = this.growing.get(gnindex);
        IGrowingNode actual = null;
        if (null == existing) {
            final RuntimeRule runtimeRule = leafNode.getRuntimeRule();
            final IGrowingNode nn = new GrowingNode(runtimeRule, startPosition, nextInputPosition, nextItemIndex, 0, FixedLists.emptyList());
            nn.addPrevious(stack, stack.getNextItemIndex());
            // this.growing.put(gnindex, nn);
            actual = this.addGrowingHead(gnindex, nn);
        } else {
            existing.addPrevious(stack, stack.getNextItemIndex());
            actual = this.addGrowingHead(gnindex, existing);
        }
        if (Log.on) {
            String m = "";
            if (actual.getPrevious().size() > 1) {
                m = "" + actual.getPrevious().size();
            }
            Log.traceln("%10s %3s %s =%s=> %s", "width", "", leafNode, m, stack.toStringTree(true, true));
        }
    }

    private IGrowingNode findOrCreateGrowingNode(final RuntimeRule runtimeRule, final int startPosition, final int nextInputPosition, final int nextItemIndex,
            final int priority, final FixedList<ICompleteNode> children, final Set<IGrowingNode.PreviousInfo> previous) {
        final int ruleNumber = runtimeRule.getRuleNumber();
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        final IGrowingNode existing = this.growing.get(gnindex);
        IGrowingNode result = null;
        if (null == existing) {
            final IGrowingNode nn = new GrowingNode(runtimeRule, startPosition, nextInputPosition, nextItemIndex, priority, children);
            for (final IGrowingNode.PreviousInfo info : previous) {
                nn.addPrevious(info.node, info.atPosition);
                // TODO: remove, this is for test
                if (ParseGraph.with) {
                    this.addGrowing(info.node);
                }
            }
            this.addGrowingHead(gnindex, nn);
            if (nn.getHasCompleteChildren()) {
                this.complete(nn);
            }
            result = nn;
        } else {
            for (final IGrowingNode.PreviousInfo info : previous) {
                existing.addPrevious(info.node, info.atPosition);
                // TODO: remove, this is for test
                if (ParseGraph.with) {
                    this.addGrowing(info.node);
                }
            }
            this.addGrowingHead(gnindex, existing);
            result = existing;
        }
        return result;
    }

    // used to provide error
    @Override
    public Collection<IGrowingNode> getGrowing() {
        return this.growing.values();
    }

    private void addGrowing(final IGrowingNode gn) {
        final int ruleNumber = gn.getRuntimeRuleNumber();
        final int startPosition = gn.getStartPosition();
        final int nextInputPosition = gn.getNextInputPosition();
        final int nextItemIndex = gn.getNextItemIndex();
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        final IGrowingNode existing = this.growing.get(gnindex);
        if (null == existing) {
            this.growing.put(gnindex, gn);
        } else {
            // merge
            for (final IGrowingNode.PreviousInfo info : gn.getPrevious()) {
                existing.addPrevious(info.node, info.atPosition);
            }
        }
    }

    private void addGrowing(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) {
        final int ruleNumber = gn.getRuntimeRuleNumber();
        final int startPosition = gn.getStartPosition();
        final int nextInputPosition = gn.getNextInputPosition();
        final int nextItemIndex = gn.getNextItemIndex();
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        final IGrowingNode existing = this.growing.get(gnindex);
        if (null == existing) {
            for (final IGrowingNode.PreviousInfo info : previous) {
                gn.addPrevious(info.node, info.atPosition);
            }
            this.growing.put(gnindex, gn);
        } else {
            // merge
            for (final IGrowingNode.PreviousInfo info : previous) {
                existing.addPrevious(info.node, info.atPosition);
            }
        }
    }

    private void removeGrowing(final IGrowingNode gn) {
        final int ruleNumber = gn.getRuntimeRuleNumber();
        final int startPosition = gn.getStartPosition();
        final int nextInputPosition = gn.getNextInputPosition();
        final int nextItemIndex = gn.getNextItemIndex();
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        this.growing.remove(gnindex);
    }

    private IGrowingNode addGrowingHead(final GrowingNodeIndex gnindex, final IGrowingNode gn) {
        final IGrowingNode existingGrowing = this.growing.get(gnindex);
        if (null != existingGrowing) {
            // don't add the head, previous should already have been merged
            if (Log.on) {
                Log.traceln("not head, already growing ", gn);
            }
            return null;
        } else {
            final IGrowingNode existing = this.growingHead.get(gnindex);
            if (null == existing) {
                this.growingHead.put(gnindex, gn);
                return gn;
            } else {
                // merge
                if (Log.on) {
                    Log.traceln("merge %s into %s", gn, existing);
                }
                for (final IGrowingNode.PreviousInfo info : gn.getPrevious()) {
                    existing.addPrevious(info.node, info.atPosition);
                }
                return existing;
            }
        }
    }

    private void removeGrowingHead(final IGrowingNode gn) {
        final int ruleNumber = gn.getRuntimeRuleNumber();
        final int startPosition = gn.getStartPosition();
        final int nextInputPosition = gn.getNextInputPosition();
        final int nextItemIndex = gn.getNextItemIndex();
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        this.growingHead.remove(gnindex);
    }

    private boolean getIsGoal(final ICompleteNode gn) {
        final boolean isStart = this.input.getIsStart(gn.getStartPosition());
        final boolean isEnd = this.input.getIsEnd(gn.getNextInputPosition());
        final boolean isGoalRule = this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber();
        return isStart && isEnd && isGoalRule;
    }

    @Override
    public ICompleteNode complete(final IGrowingNode gn) {
        if (gn.getHasCompleteChildren()) {
            final RuntimeRule runtimeRule = gn.getRuntimeRule();
            final int priority = gn.getPriority();
            final int startPosition = gn.getStartPosition();
            final int nextInputPosition = gn.getNextInputPosition();
            ICompleteNode cn = this.findNode(runtimeRule.getRuleNumber(), startPosition, nextInputPosition);
            if (null == cn) {
                cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, nextInputPosition);
                if (gn.isLeaf()) {
                    // dont try and add children...can't for a leaf
                } else {
                    cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());
                }
            } else {
                if (gn.isLeaf()) {
                    // dont try and add children...can't for a leaf
                } else {
                    // final ICompleteNode.ChildrenOption opt = new ICompleteNode.ChildrenOption();
                    // opt.matchedLength = gn.getMatchedTextLength();
                    // opt.nodes = gn.getGrowingChildren();

                    // TODO: don't add duplicate children
                    // somewhere resolve priorities!
                    final int existingPriority = cn.getPriority();
                    final int newPriority = gn.getPriority();
                    if (existingPriority == newPriority) {
                        // TODO: record/log ambiguity!
                        cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());
                        if (gn.isEmptyRuleMatch() && cn.isEmptyRuleMatch()) {
                            if (cn.getChildrenAlternatives().isEmpty()) {
                                cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());
                            } else {
                                if (cn.getChildrenAlternatives().iterator().next().get(0).isEmptyLeaf()) {
                                    // leave it, no need to add empty alternatives
                                } else {
                                    if (gn.getGrowingChildren().get(0).isEmptyLeaf()) {
                                        // use just the empty leaf
                                        cn.getChildrenAlternatives().clear();
                                        cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());
                                    } else {
                                        // add the alternatives
                                        cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());
                                    }
                                }
                            }

                        } else {
                            cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());

                        }
                    } else if (existingPriority < newPriority) {
                        // do nothing, drop new one
                        final int i = 0;
                    } else if (newPriority < existingPriority) {
                        // replace existing with new
                        cn.getChildrenAlternatives().clear();
                        cn.getChildrenAlternatives().add((FixedList<SPPTNode>) (FixedList<?>) gn.getGrowingChildren());
                    }
                }
            }

            this.checkForGoal(cn);
            return cn;
        } else {
            return null;
        }
    }

    @Override
    public void createStart(final RuntimeRule goalRule) {
        final IGrowingNode gn = this.findOrCreateGrowingNode(goalRule, 0, 0, 0, 0, FixedLists.emptyList(), Collections.emptySet());// this.createBranch(goalRule,
        if (Log.on) {
            Log.traceln("%10s %3s %s", "create", "", gn.toStringTree(true, true));
        }
    }

    // @Override
    private ICompleteNode createLeaf(final Leaf leaf) {
        final ICompleteNode gn = new GraphNodeLeaf(this, leaf);
        final NodeIndex nindex = new NodeIndex(leaf.getRuntimeRuleNumber(), leaf.getStartPosition(), leaf.getNextInputPosition());
        // this.registerCompleteNode(gn);
        this.leaves.put(nindex, gn);
        this.nodes.put(nindex, gn);
        this.checkForGoal(gn);
        return gn;
    }

    private ICompleteNode createBranchNoChildren(final RuntimeRule runtimeRule, final int priority, final int startPosition, final int nextInputPosition) {
        final ICompleteNode gn = new GraphNodeBranch(this, runtimeRule, priority, startPosition, nextInputPosition);
        final NodeIndex nindex = new NodeIndex(runtimeRule.getRuleNumber(), startPosition, nextInputPosition);
        this.nodes.put(nindex, gn);
        return gn;
    }

    @Override
    public ICompleteNode findOrCreateLeaf(final Leaf leaf) {
        final NodeIndex nindex = new NodeIndex(leaf.getRuntimeRuleNumber(), leaf.getStartPosition(), leaf.getNextInputPosition());
        ICompleteNode gn = this.leaves.get(nindex);
        if (null == gn) {
            gn = this.createLeaf(leaf);
        }

        return gn;
    }

    @Override
    public void createWithFirstChild(final RuntimeRule runtimeRule, final int priority, final ICompleteNode firstChild,
            final Set<IGrowingNode.PreviousInfo> previous) {
        final int startPosition = firstChild.getStartPosition();
        final int nextInputPosition = firstChild.getNextInputPosition();
        int nextItemIndex = 0;
        switch (runtimeRule.getRhs().getKind()) {
            case CHOICE:
                nextItemIndex = -1;
            break;
            case CONCATENATION:
                nextItemIndex = runtimeRule.getRhs().getItems().length == 1 ? -1 : 1;
            break;
            case EMPTY:
                nextItemIndex = -1;
            break;
            case MULTI:
                nextItemIndex = firstChild.isEmptyLeaf() ? -1 : 1 == runtimeRule.getRhs().getMultiMax() ? -1 : 1;
            break;
            case PRIORITY_CHOICE:
                nextItemIndex = -1;
            break;
            case SEPARATED_LIST:
                nextItemIndex = firstChild.isEmptyLeaf() ? -1 : 1 == runtimeRule.getRhs().getMultiMax() ? -1 : 1;
            break;
            default:
                throw new RuntimeException("Internal Error: Unknown RuleKind " + runtimeRule.getRhs().getKind());
        }
        final FixedList<ICompleteNode> children = FixedLists.of(firstChild);

        final IGrowingNode gn = this.findOrCreateGrowingNode(runtimeRule, startPosition, nextInputPosition, nextItemIndex, priority, children, previous);
        if (Log.on) {
            Log.traceln("%10s %3s %s", "height", "", gn.toStringTree(true, true));
        }
    }

    private void growNextChildAt(final IGrowingNode parent, final ICompleteNode nextChild, final int nextItemIndex) {
        final RuntimeRule runtimeRule = parent.getRuntimeRule();
        final int priority = parent.getPriority();
        final int startPosition = parent.getStartPosition();
        final int nextInputPosition = nextChild.getNextInputPosition();
        final FixedList<ICompleteNode> children = parent.getGrowingChildren().append(nextChild);
        final Set<IGrowingNode.PreviousInfo> previous = parent.getPrevious();
        for (final IGrowingNode.PreviousInfo pi : previous) {
            pi.node.removeNext(parent);
        }
        final IGrowingNode gn = this.findOrCreateGrowingNode(runtimeRule, startPosition, nextInputPosition, nextItemIndex, priority, children, previous);
        if (Log.on) {
            Log.traceln("%10s %3s %s", "graft", "", gn.toStringTree(true, true));
        }
        if (parent.getNext().isEmpty()) {
            this.removeGrowing(parent);
        }
    }

    @Override
    public void growNextChild(final IGrowingNode parent, final ICompleteNode nextChild, final int position) {
        if (0 != position && parent.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.MULTI) {
            final ICompleteNode prev = parent.getGrowingChildren().get(position - 1);
            if (prev == nextChild) {
                // dont add same child twice to a multi
                return;
            }
        }
        int newNextItemIndex = 0;
        switch (parent.getRuntimeRule().getRhs().getKind()) {
            case CHOICE:
                newNextItemIndex = -1;
            break;
            case CONCATENATION:
                newNextItemIndex = parent.getRuntimeRule().getRhs().getItems().length == parent.getNextItemIndex() + 1 ? -1 : parent.getNextItemIndex() + 1;
            break;
            case EMPTY:
                newNextItemIndex = -1;
            break;
            case MULTI:
                newNextItemIndex = parent.getNextItemIndex() + 1;
            break;
            case PRIORITY_CHOICE:
                newNextItemIndex = -1;
            break;
            case SEPARATED_LIST:
                newNextItemIndex = parent.getNextItemIndex() + 1;
            break;
            default:
                throw new RuntimeException("Internal Error: Unknown RuleKind " + parent.getRuntimeRule().getRhs().getKind());
        }

        this.growNextChildAt(parent, nextChild, newNextItemIndex);

        // final RuntimeRule runtimeRule = parent.getRuntimeRule();
        // final int priority = parent.getPriority();
        // final int startPosition = parent.getStartPosition();
        // final int nextInputPosition = nextChild.getNextInputPosition();
        // final int nextItemIndex = newNextItemIndex;
        // final List<ICompleteNode> children = new ArrayList<>(parent.getGrowingChildren());
        // children.add(nextChild);
        //
        // final Set<IGrowingNode.PreviousInfo> previous = parent.getPrevious();
        // final IGrowingNode gn = this.findOrCreateGrowingNode(runtimeRule, startPosition, nextInputPosition, nextItemIndex, priority, children, previous);
        // if (Log.on) {
        // Log.traceln("%10s %3s %s", "graft", "", gn.toStringTree(true, true));
        // }
        //
        // // maybe?
        // if (parent.getNext().isEmpty()) {
        // this.removeGrowing(parent);
        // }

    }

    @Override
    public void growNextSkipChild(final IGrowingNode parent, final ICompleteNode nextChild) {
        final int nextItemIndex = parent.getNextItemIndex();
        this.growNextChildAt(parent, nextChild, nextItemIndex);
    }

    @Override
    public void pushToStackOf(final ICompleteNode leafNode, final IGrowingNode stack, final Set<IGrowingNode.PreviousInfo> previous) {
        this.findOrCreateGrowingLeaf(leafNode, stack, previous);
    }

    @Override
    public Set<IGrowingNode.PreviousInfo> pop(final IGrowingNode gn) {
        for (final IGrowingNode.PreviousInfo pi : gn.getPrevious()) {
            pi.node.removeNext(gn);
            // TODO: remove, this is for test
            if (ParseGraph.with) {
                this.removeGrowing(pi.node);
            }
        }
        final Set<IGrowingNode.PreviousInfo> previous = gn.getPrevious();
        gn.newPrevious();
        return previous;
    }

    @Override
    public void makeHead(final IGrowingNode node) {
        final int ruleNumber = node.getRuntimeRuleNumber();
        final int startPosition = node.getStartPosition();
        final int nextInputPosition = node.getNextInputPosition();
        final int nextItemIndex = node.getNextItemIndex();
        final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex);
        // this.growing.remove(gnindex);
        // this.growingHead.put(gnindex, node);
        this.addGrowingHead(gnindex, node);
    }

    @Override
    public String toString() {
        return this.goals + System.lineSeparator() + this.growingHead.size() + "-" + Arrays.toString(this.growingHead.values().toArray());
    }

}
