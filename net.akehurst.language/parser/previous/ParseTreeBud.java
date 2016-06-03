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

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.forrest.ParseTreeBranch;
import net.akehurst.language.parser.forrest.SubParseTree;

public class ParseTreeBud extends SubParseTree {

	public ParseTreeBud(Leaf leaf, int inputLength) {
		super(inputLength);
		this.root = leaf;
		this.complete = true;
	}

	@Override
	public Leaf getRoot() {
		return (Leaf)super.getRoot();
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public ParseTreeBud deepClone() {
		ParseTreeBud clone = new ParseTreeBud(this.getRoot(), this.inputLength);
		return clone;
	}
	
	public ParseTreeBranch extendWith(IParseTree extension) throws CannotExtendTreeException {
		throw new CannotExtendTreeException();
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getRoot().toString();
	}
	
	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof ParseTreeBud) {
			ParseTreeBud other = (ParseTreeBud)arg;
			return this.getRoot().equals(other.getRoot());
		} else {
			return false;
		}
	}

}
