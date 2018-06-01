package net.akehurst.language.api.sppt;

import java.util.List;
import java.util.Set;

/**
 *
 * A branch in a SharedPackedParseTree
 *
 */
public interface SPPTBranch extends SPPTNode {

    /**
     *
     * @return the set of alternative children for this branch
     */
    Set<FixedList<SPPTNode>> getChildrenAlternatives();

    // --- convienience methods ---
    /**
     * @return the one of the children alternatives of this branch.
     */
    FixedList<SPPTNode> getChildren();

    /**
     * @return the one of the children alternatives of this branch with all skip nodes removed.
     */
    List<SPPTNode> getNonSkipChildren();

    /**
     * this returns the i'th element from getNonSkipChildren
     *
     * @param i
     *            index of required child
     * @return i'th non skip child.
     */
    SPPTNode getChild(int index);

    /**
     * Convenience method. returns the i'th non skip child of this Branch but assumes the child is also a Branch and casts the result.
     *
     * @param i
     * @return
     */
    SPPTBranch getBranchChild(int index);

    /**
     * Filters out any children that are skip nodes or not branches
     *
     * @return all children that are branches and non skip
     */
    List<SPPTBranch> getBranchNonSkipChildren();
}
