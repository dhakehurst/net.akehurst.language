package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPNodeIdentity;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;
import net.akehurst.language.grammar.parser.ParseTreeToSingleLineTreeString;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.sppf.FixedLists;
import net.akehurst.language.parser.sppf.SPPTNodeIdentitySimple;

public class GraphNodeBranch implements ICompleteNode, SPPTBranch {

    private final RuntimeRule runtimeRule;
    private final int priority;
    private final int startPosition;
    private final int nextInputPosition;
    private final Set<FixedList<SPPTNode>> childrenAlternatives;
    private SPPTBranch parent;

    public GraphNodeBranch(final IParseGraph graph, final RuntimeRule runtimeRule, final int priority, final int startPosition, final int nextInputPosition) {
        this.runtimeRule = runtimeRule;
        this.priority = priority;
        this.startPosition = startPosition;
        this.nextInputPosition = nextInputPosition;
        this.childrenAlternatives = new HashSet<>();
    }

    // --- ICompleteNode ---
    @Override
    public RuntimeRule getRuntimeRule() {
        // TODO Auto-generated method stub
        return this.runtimeRule;
    }

    @Override
    public int getRuntimeRuleNumber() {
        return this.getRuntimeRule().getRuleNumber();
    }

    @Override
    public int getStartPosition() {
        return this.startPosition;
    }

    @Override
    public int getNextInputPosition() {
        return this.nextInputPosition;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public int getMatchedTextLength() {
        return this.getNextInputPosition() - this.getStartPosition();
    }

    @Override
    public boolean isEmptyLeaf() {
        return false;
    }

    // --- IBranch ---
    // @Override
    // public boolean getIsEmpty() {
    // if (this.getNonSkipChildren().isEmpty()) {
    // return true;
    // } else {
    // if (this.getNonSkipChildren().get(0).isEmptyLeaf()) {
    // return true;
    // } else {
    // return false;
    // }
    // }
    // }

    @Override
    public FixedList<SPPTNode> getChildren() {
        if (this.getChildrenAlternatives().isEmpty()) {
            return FixedLists.emptyList();
        } else {
            return this.getChildrenAlternatives().iterator().next();
        }
    }

    public IGraphNode getChildAt(final int index) {
        return (IGraphNode) this.getChildren().get(index);
    }

    @Override
    public SPPTNode getChild(final int index) {
        final FixedList<SPPTNode> children = this.getChildren();

        // get first non skip child
        int child = 0;
        SPPTNode n = children.get(child);
        while (n.isSkip() && child < children.size() - 1) {
            ++child;
            n = children.get(child);
        }
        if (child >= children.size()) {
            return null;
        }
        int count = 0;

        while (count < index && child < children.size() - 1) {
            ++child;
            n = children.get(child);
            while (n.isSkip()) {
                ++child;
                n = children.get(child);
            }
            ++count;
        }

        if (child < children.size()) {
            return n;
        } else {
            return null;
        }
    }

    @Override
    public SPPTBranch getBranchChild(final int i) {
        final SPPTNode n = this.getChild(i);
        return (SPPTBranch) n;
    }

    @Override
    public List<SPPTBranch> getBranchNonSkipChildren() {
        final List<SPPTBranch> res = this.getNonSkipChildren().stream().filter(SPPTBranch.class::isInstance).map(SPPTBranch.class::cast)
                .collect(Collectors.toList());
        return res;
    }

    List<SPPTNode> nonSkipChildren_cache;

    // --- IBranch ---
    @Override
    public List<SPPTNode> getNonSkipChildren() {
        if (null == this.nonSkipChildren_cache) {
            this.nonSkipChildren_cache = new ArrayList<>();
            for (final SPPTNode n : this.getChildren()) {
                if (n.isSkip()) {

                } else {
                    this.nonSkipChildren_cache.add(n);
                }
            }
        }
        return this.nonSkipChildren_cache;
    }

    // --- INode ---
    @Override
    public SPNodeIdentity getIdentity() {
        // TODO cache this
        return new SPPTNodeIdentitySimple(this.getRuntimeRuleNumber(), this.getStartPosition(), this.getMatchedTextLength());
    }

    @Override
    public SPPTBranch getParent() {
        return this.parent;
    }

    @Override
    public void setParent(final SPPTBranch value) {
        this.parent = value;
    }

    @Override
    public String getName() {
        return this.getRuntimeRule().getName();
    }

    @Override
    public String getMatchedText() {
        String str = "";
        for (final SPPTNode n : this.getChildren()) {
            str += n.getMatchedText();
        }
        return str;
    }

    @Override
    public int getNumberOfLines() {
        final String str = this.getMatchedText();
        final Pattern p = Pattern.compile(System.lineSeparator());
        final Matcher m = p.matcher(str);
        int count = 0;
        while (m.find()) {
            count += 1;
        }
        return count;
    }

    @Override
    public String getNonSkipMatchedText() {
        String str = "";
        for (final SPPTNode n : this.getNonSkipChildren()) {
            str += n.getNonSkipMatchedText();
        }
        return str;
    }

    @Override
    public Set<FixedList<SPPTNode>> getChildrenAlternatives() {
        return this.childrenAlternatives;
    }

    @Override
    public boolean isEmptyRuleMatch() {
        // children must be complete or we would not have created the node
        // therefore must match empty if start and next-input positions are the same
        return this.getStartPosition() == this.getNextInputPosition();
    }

    @Override
    public boolean isSkip() {
        return this.getRuntimeRule().getIsSkipRule();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isBranch() {
        return true;
    }

    @Override
    public SPPTLeaf asLeaf() {
        return null;
    }

    @Override
    public SPPTBranch asBranch() {
        return this;
    }

    @Override
    public boolean contains(final SPPTNode other) {
        if (other instanceof SPPTBranch) {
            final SPPTBranch otherBranch = (SPPTBranch) other;

            if (this.getIdentity().equals(other.getIdentity())) {
                // for each alternative list of other children, check there is a matching list
                // of children in this alternative children
                boolean allOthersAreContained = true; // if no other children alternatives contain is a match
                for (final FixedList<SPPTNode> otherChildren : otherBranch.getChildrenAlternatives()) {
                    // for each of this alternative children, find one that 'contains' otherChildren
                    boolean foundContainMatch = false;
                    for (final FixedList<SPPTNode> thisChildren : this.getChildrenAlternatives()) {
                        if (thisChildren.size() == otherChildren.size()) {
                            // for each pair of nodes, one from each of otherChildren thisChildren
                            // check thisChildrenNode contains otherChildrenNode
                            boolean thisMatch = true;
                            for (int i = 0; i < thisChildren.size(); ++i) {
                                final SPPTNode thisChildrenNode = thisChildren.get(i);
                                final SPPTNode otherChildrenNode = otherChildren.get(i);
                                thisMatch &= thisChildrenNode.contains(otherChildrenNode);
                            }
                            if (thisMatch) {
                                foundContainMatch = true;
                                break;
                            } else {
                                // if thisChildren alternative doesn't contain, try the next one
                                continue;
                            }
                        } else {
                            // if sizes don't match check next in set of this alternative children
                            continue;
                        }
                    }
                    allOthersAreContained &= foundContainMatch;
                }
                return allOthersAreContained;
            } else {
                // if identities don't match
                return false;
            }

        } else {
            // if other is not a branch
            return false;
        }
    }

    // @Override
    // public List<ISPPFBranch> findBranches(final String name) {
    // final List<ISPPFBranch> result = new ArrayList<>();
    // if (Objects.equals(this.getName(), name)) {
    // result.add(this);
    // } else {
    // for (final ISPPFNode child : this.getChildren()) {
    // result.addAll(child.findBranches(name));
    // }
    // }
    // return result;
    // }

    @Override
    public String toStringTree() {
        String r = "";
        r += this.getStartPosition() + ",";
        r += this.getNextInputPosition();
        r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";

        r += "{";
        for (final SPPTNode c : this.getChildren()) {
            r += c.accept(new ParseTreeToSingleLineTreeString(), null);
        }
        r += "}";

        return r;
    }

    // --- IParseTreeVisitable ---
    @Override
    public <T, A, E extends Throwable> T accept(final SharedPackedParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
        return visitor.visit(this, arg);
    }

    // --- Object ---
    @Override
    public String toString() {
        String r = "";
        r += this.getStartPosition() + ",";
        r += this.getNextInputPosition();
        r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
        return r;
    }
}
