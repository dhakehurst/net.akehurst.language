package net.akehurst.language.core.sppf;

import java.util.List;
import java.util.Set;

public interface ISPPFBranch extends ISPPFNode {

	Set<List<ISPPFNode>> getChildrenAlternatives();

	// --- convienience methods
	/**
	 * @return the one of the children alternatives of this branch.
	 */
	List<ISPPFNode> getChildren();

	/**
	 * @return the one of the children alternatives of this branch with all skip nodes removed.
	 */
	List<ISPPFNode> getNonSkipChildren();

	/**
	 * this returns the i'th element from getNonSkipChildren
	 *
	 * @param i
	 *            index of required child
	 * @return i'th non skip child.
	 */
	ISPPFNode getChild(int index);

	/**
	 * Convenience method. returns the i'th non skip child of this Branch but assumes the child is also a Branch and casts the result.
	 *
	 * @param i
	 * @return
	 */
	ISPPFBranch getBranchChild(int index);

	/**
	 * Filters out any children that are skip nodes or not branches
	 *
	 * @return all children that are branches and non skip
	 */
	List<ISPPFBranch> getBranchNonSkipChildren();
}
