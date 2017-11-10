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
package net.akehurst.language.core.sppf;

import java.util.List;

import net.akehurst.language.core.parser.IParseTreeVisitable;

public interface INode_old extends ISPPFNode, IParseTreeVisitable {

	ISPPFBranch getParent();

	void setParent(ISPPFBranch value);

	int getNextInputPosition();

	boolean isEmptyLeaf();

	// boolean getIsEmpty();
	boolean getIsSkip();

	int getNumberOfLines();

	/**
	 * find all branches with the given name that are descendants of this node. Will not include branches with the given name that are descendants of a found
	 * branch.
	 *
	 * @param name
	 * @return
	 */
	List<ISPPFBranch> findBranches(String name);
}
