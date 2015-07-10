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
package net.akehurst.language.ogl.semanticStructure;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;

public class RuleNodeType implements INodeType {
	
	public RuleNodeType(Rule rule) {
		this.identity = new NodeIdentity(rule.getName());
	}

	INodeIdentity identity;
	@Override
	public INodeIdentity getIdentity() {
		return this.identity;
	}

	//--- Object ---
	@Override
	public String toString() {
		return this.getIdentity().asPrimitive();
	}
	
	@Override
	public int hashCode() {
		return this.getIdentity().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof RuleNodeType) {
			RuleNodeType other = (RuleNodeType)arg;
			return this.getIdentity().equals(other.getIdentity());
		} else {
			return false;
		}
	}
}
