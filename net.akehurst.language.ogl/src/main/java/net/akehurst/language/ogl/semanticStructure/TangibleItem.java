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

import java.util.ArrayList;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.RuleNotFoundException;

public abstract class TangibleItem extends SimpleItem {

	public abstract INodeType getNodeType() throws RuleNotFoundException;

	ArrayList<Integer> index;
	public ArrayList<Integer> getIndex() {
		return this.index;
	}
	@Override
	public void setOwningRule(Rule value, ArrayList<Integer> index) {
		this.owningRule = value;
		this.index = index;
	}
}
