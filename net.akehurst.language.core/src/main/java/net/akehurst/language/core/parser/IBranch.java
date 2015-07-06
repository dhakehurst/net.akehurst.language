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
package net.akehurst.language.core.parser;

import java.util.List;

public interface IBranch extends INode, IParseTreeVisitable {
	/**
	 * 
	 * @return the children of this branch.
	 */
	List<INode> getChildren();
	
	/**
	 * this creates a new branch it does not modify current one
	 *  
	 * @param i index of required child
	 * @return i'th non skip child.
	 */
	INode getChild(int i);
	
}
