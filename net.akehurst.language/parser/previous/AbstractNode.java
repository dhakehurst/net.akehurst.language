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
package net.akehurst.language.parser;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;

public abstract class AbstractNode implements INode {

	public AbstractNode(INodeType nodeType) {
		this.nodeType = nodeType;
	}
	
	INodeType nodeType;
	@Override
	public INodeType getNodeType() {
		return this.nodeType;
	}
	
	String name;
	@Override
	public String getName() {
		return this.getNodeType().getIdentity().asPrimitive();
	}

	int length;
	@Override
	public int getLength() {
		return this.length;
	}

	public abstract AbstractNode deepClone();
}
