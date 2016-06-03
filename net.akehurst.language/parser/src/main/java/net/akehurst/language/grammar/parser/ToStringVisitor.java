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
package net.akehurst.language.grammar.parser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.forrest.AbstractParseTree;

public class ToStringVisitor implements IParseTreeVisitor<String, String, RuntimeException> {

	public ToStringVisitor() {
		this(System.lineSeparator(), "  ");
	}

	public ToStringVisitor(String lineSeparator, String indentIncrement) {
		this.lineSeparator = lineSeparator;
		this.indentIncrement = indentIncrement;
	}

	String lineSeparator;
	String indentIncrement;

	@Override
	public String visit(IParseTree target, String indent) throws RuntimeException {
		String s = indent;
		AbstractParseTree t = (AbstractParseTree) target;
		s += "{" + (target.getIsComplete() ? "*" : "+") + (target.getCanGrowWidth() ? "?" : "") + target.getRoot().getName() + " " + t.identifier + getStackRootsAsString(target) + "}";
		// s += target.getRoot().accept(this, indent);
		return s;
	}

	String getStackRootsAsString(IParseTree target) {
		AbstractParseTree st = ((AbstractParseTree) target).getStackedTree();
		if (null == st) {
			return "";
		} else {
			String s = " " + "[" + (st.getIsComplete() ? "*" : "+") + (st.getCanGrow() ? "?" : "") + st.getRoot().getName() + " " + st.identifier + "]";
			s += getStackRootsAsString(st);
			return s;
		}
	}

	@Override
	public String visit(ILeaf target, String indent) throws RuntimeException {
		String s = indent + target.getName() + " : \"" + target.getMatchedText().replace("\n", new String(Character.toChars(0x23CE))) + "\"";
		return s;
	}

	@Override
	public String visit(IBranch target, String indent) throws RuntimeException {
		String s = indent;
		s += target.getName() + " : [";
		if (0 < target.getChildren().size()) {
			s += this.lineSeparator;
			s += target.getChildren().get(0).accept(this, indent + indentIncrement);
			for (int i = 1; i < target.getChildren().size(); ++i) {
				s += ", " + this.lineSeparator;
				s += target.getChildren().get(i).accept(this, indent + indentIncrement);
			}
			s += this.lineSeparator + indent;
		}
		s += "]";
		return s;
	}

}
