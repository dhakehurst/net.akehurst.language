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

import net.akehurst.language.core.sppt.ISPLeaf;
import net.akehurst.language.core.sppt.IParseTreeVisitor;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;

public class ToStringVisitor implements IParseTreeVisitor<String, String, RuntimeException> {

	public ToStringVisitor() {
		this(System.lineSeparator(), "  ");
	}

	public ToStringVisitor(final String lineSeparator, final String indentIncrement) {
		this.lineSeparator = lineSeparator;
		this.indentIncrement = indentIncrement;
	}

	private final String lineSeparator;
	private final String indentIncrement;

	@Override
	public String visit(final ISharedPackedParseTree target, final String indent) throws RuntimeException {
		String s = indent;
		final ISPNode root = target.getRoot();
		s += root.accept(this, indent);
		return s;
	}

	@Override
	public String visit(final ISPLeaf target, final String indent) throws RuntimeException {
		final String s = indent + target.getName() + " : \"" + target.getMatchedText().replace("\n", new String(Character.toChars(0x23CE))) + "\"";
		return s;
	}

	@Override
	public String visit(final ISPBranch target, final String indent) throws RuntimeException {
		String s = indent;
		s += target.getName();
		s += target.getChildrenAlternatives().size() > 1 ? "*" : "";
		s += " {";
		if (0 < target.getChildren().size()) {
			s += this.lineSeparator;
			s += target.getChildren().get(0).accept(this, indent + this.indentIncrement);
			for (int i = 1; i < target.getChildren().size(); ++i) {
				s += ", " + this.lineSeparator;
				s += target.getChildren().get(i).accept(this, indent + this.indentIncrement);
			}
			s += this.lineSeparator + indent;
		}
		s += "}";
		return s;
	}

}
