/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.parser.sppf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.akehurst.language.core.sppt.FixedList;
import net.akehurst.language.core.sppt.SPNodeIdentity;
import net.akehurst.language.core.sppt.SPPTBranch;
import net.akehurst.language.core.sppt.SPPTNode;
import net.akehurst.language.core.sppt.SharedPackedParseTreeVisitor;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Branch extends Node implements SPPTBranch {

    private final SPNodeIdentity identity;
    private final Set<FixedList<SPPTNode>> childrenAlternatives;
    private int length;
    private List<SPPTNode> nonSkipChildren_cache;

    public Branch(final RuntimeRule runtimeRule, final SPPTNode[] children) {
        super(runtimeRule, children.length == 0 ? -1 : children[0].getStartPosition());
        this.childrenAlternatives = new HashSet<>();
        this.childrenAlternatives.add(FixedLists.of(children));
        this.length = 0;
        // this.isEmpty = true;
        // this.firstLeaf = this.children.length==0 ? null : children[0].getFirstLeaf();
        for (final SPPTNode n : children) {
            // this.isEmpty &= n.getIsEmpty();
            this.length += n.getMatchedTextLength();
        }
        this.identity = new SPPTNodeIdentitySimple(runtimeRule.getRuleNumber(), this.getStartPosition(), this.length);

    }

    // --- ISPPFBranch ---
    @Override
    public Set<FixedList<SPPTNode>> getChildrenAlternatives() {
        return this.childrenAlternatives;
    }

    @Override
    public FixedList<SPPTNode> getChildren() {
        return this.childrenAlternatives.iterator().next();
    }

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

    // --- ISPPFNode ---
    @Override
    public SPNodeIdentity getIdentity() {
        return this.identity;
    }

    @Override
    public int getMatchedTextLength() {
        return this.length;
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
    public String getNonSkipMatchedText() {
        String str = "";
        for (final SPPTNode n : this.getNonSkipChildren()) {
            str += n.getNonSkipMatchedText();
        }
        return str;
    }

    @Override
    public boolean isEmptyLeaf() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    // @Override
    // public boolean containsOnlyEmptyLeafs() {
    // // TODO Auto-generated method stub
    // return false;
    // }

    @Override
    public boolean isBranch() {
        return true;
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

    // --- IParseTreeVisitable ---
    @Override
    public <T, A, E extends Throwable> T accept(final SharedPackedParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
        return visitor.visit(this, arg);
    }

    // @Override
    // public boolean getIsEmpty() {
    // if (this.getNonSkipChildren().isEmpty()) {
    // return true;
    // } else {
    // if (this.getNonSkipChildren().get(0) instanceof EmptyLeaf) {
    // return true;
    // } else {
    // return false;
    // }
    // }
    // }

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

    // --- Object ---
    @Override
    public String toString() {
        final ToStringVisitor v = new ToStringVisitor();
        return this.accept(v, new ToStringVisitor.Indent("")).iterator().next();
    }

    @Override
    public int hashCode() {
        return this.getIdentity().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (!(arg instanceof SPPTBranch)) {
            return false;
        }
        final SPPTBranch other = (SPPTBranch) arg;
        if (!Objects.equals(this.getIdentity(), other.getIdentity())) {
            return false;
        }
        return this.getChildren().equals(other.getChildren());
    }

}
