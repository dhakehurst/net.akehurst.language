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

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public class Leaf extends AbstractNode implements ILeaf {

	public Leaf(INodeType nodeType, String matchedText) {
		super(nodeType);
		this.matchedText = matchedText;
		this.length = this.matchedText.length();
	}

	String matchedText;
	public String getMatchedText() {
		return this.matchedText;
	}
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	public Leaf deepClone() {
		Leaf clone = new Leaf(this.nodeType, this.matchedText);
		clone.matchedText = this.matchedText;
		clone.length = this.length;
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "<" + this.getNodeType() + ">{"+this.getMatchedText()+"}";
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Leaf) {
			Leaf other = (Leaf)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
