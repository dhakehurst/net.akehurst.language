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

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.ogl.semanticModel.Rule;

public class Branch extends AbstractNode implements IBranch {

	public Branch(INodeType nodeType) {
		super(nodeType);
		this.childeren = new ArrayList<>();
		this.length = 0;
	}
	Rule rule;
	
	List<INode> childeren;
	@Override
	public List<INode> getChildren() {
		return this.childeren;
	}
	public IBranch addChild(INode child) {
		this.childeren.add(child);
		//child.setParent(this);
		this.length += child.getLength();
		return this;
	}

	@Override
	public Branch deepClone() {
		Branch clone = new Branch(this.getNodeType());
		clone.name = this.name;
		for(INode n: this.getChildren()) {
			AbstractNode an = (AbstractNode)n;
			clone.addChild(an.deepClone());
		}
		clone.length = this.length;
		return clone;
	}
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
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
