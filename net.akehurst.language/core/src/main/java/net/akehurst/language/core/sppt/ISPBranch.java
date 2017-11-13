package net.akehurst.language.core.sppt;

import java.util.List;
import java.util.Set;

public interface ISPBranch extends ISPNode {

	Set<List<ISPNode>> getChildrenAlternatives();

	// --- convienience methods
	/**
	 * @return the one of the children alternatives of this branch.
	 */
	List<ISPNode> getChildren();

	/**
	 * @return the one of the children alternatives of this branch with all skip nodes removed.
	 */
	List<ISPNode> getNonSkipChildren();

	/**
	 * this returns the i'th element from getNonSkipChildren
	 *
	 * @param i
	 *            index of required child
	 * @return i'th non skip child.
	 */
	ISPNode getChild(int index);

	/**
	 * Convenience method. returns the i'th non skip child of this Branch but assumes the child is also a Branch and casts the result.
	 *
	 * @param i
	 * @return
	 */
	ISPBranch getBranchChild(int index);

	/**
	 * Filters out any children that are skip nodes or not branches
	 *
	 * @return all children that are branches and non skip
	 */
	List<ISPBranch> getBranchNonSkipChildren();
}
