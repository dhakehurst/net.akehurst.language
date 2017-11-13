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
package net.akehurst.language.core.sppt;

import java.util.List;

public interface IBranch_old extends ISPBranch {

	/**
	 * returns true if there are 0 non skip children or if the only child node is an EmptyLeaf
	 *
	 * @return
	 */
	boolean getIsEmpty();

	/**
	 *
	 * @return the children of this branch.
	 */
	List<ISPNode> getChildren();

	List<ISPNode> getNonSkipChildren();

	/**
	 * this returns the i'th non skip child of this Branch
	 *
	 * @param i
	 *            index of required child
	 * @return i'th non skip child.
	 */
	ISPNode getChild(int i);

	/**
	 * Convenience method. returns the i'th non skip child of this Branch but assumes the child is also a Branch and casts the result.
	 *
	 * @param i
	 * @return
	 */
	ISPBranch getBranchChild(int i);

	/**
	 * Filters out any children that are skip nodes or not branches
	 *
	 * @return all children that are branches and non skip
	 */
	List<ISPBranch> getBranchNonSkipChildren();
}
