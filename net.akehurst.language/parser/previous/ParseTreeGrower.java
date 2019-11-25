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

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatination;
import net.akehurst.language.ogl.semanticModel.Group;
import net.akehurst.language.ogl.semanticModel.LeafNodeType;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.ogl.semanticModel.TerminalPattern;
import net.akehurst.language.ogl.semanticModel.Visitor;
import net.akehurst.language.parser.forrest.ParseTreeBranch;

/*
 * arg = oldBranch: IParseTree
 */
public class ParseTreeGrower implements Visitor<List<IParseTree>, CannotGrowTreeException> {

	public  ParseTreeGrower(int inputLength) {
		this.inputLength = inputLength;
	}
	int inputLength;
	@Override
	public List<IParseTree> visit(Choice target, Object... arg) throws CannotGrowTreeException {
		try {
			if (arg.length > 0 && arg[0] instanceof IParseTree) {
				IParseTree oldBranch = (IParseTree) arg[0];
				List<IParseTree> result = new ArrayList<>();
				for (TangibleItem alt : target.getAlternative()) {
					if (oldBranch.getRoot().getNodeType().equals(alt.getNodeType())) {
						INodeType nodeType = target.getOwningRule().getNodeType();
						ParseTreeBranch branch = new ParseTreeBranch(nodeType, oldBranch, target.getOwningRule(), this.inputLength);
						result.add(branch);
					}
				}
				return result;
			} else {
				throw new RuntimeException("Should not happend");
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happend");
		}
	}

	@Override
	public List<IParseTree> visit(Concatination target, Object... arg) throws CannotGrowTreeException {
		try {
			if (arg.length > 0 && arg[0] instanceof IParseTree) {
				IParseTree oldBranch = (IParseTree) arg[0];
				List<IParseTree> result = new ArrayList<>();
				if (target.getItem().get(0).getNodeType().equals(oldBranch.getRoot().getNodeType())) {
					INodeType nodeType = target.getOwningRule().getNodeType();
					ParseTreeBranch branch = new ParseTreeBranch(nodeType, oldBranch, target.getOwningRule(), this.inputLength);
					result.add(branch);
				}
				return result;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	@Override
	public List<IParseTree> visit(Group target, Object... arg) throws CannotGrowTreeException {
		return null;
	}

	@Override
	public List<IParseTree> visit(Multi target, Object... arg) throws CannotGrowTreeException {
		try {
			if (arg.length > 0 && arg[0] instanceof IParseTree) {
				IParseTree oldBranch = (IParseTree) arg[0];
				List<IParseTree> result = new ArrayList<>();
				if (target.getItem().getNodeType().equals(oldBranch.getRoot().getNodeType()) || (0==target.getMin() && oldBranch.getRoot().getNodeType().equals(new LeafNodeType())) ) {
					INodeType nodeType = target.getOwningRule().getNodeType();
					ParseTreeBranch branch = new ParseTreeBranch(nodeType, oldBranch, target.getOwningRule(), this.inputLength);
					result.add(branch);
				}
				return result;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	@Override
	public List<IParseTree> visit(SeparatedList target, Object... arg) throws CannotGrowTreeException {
		try {
			if (arg.length > 0 && arg[0] instanceof IParseTree) {
				IParseTree oldBranch = (IParseTree) arg[0];
				List<IParseTree> result = new ArrayList<>();
				if (target.getConcatination().getNodeType().equals(oldBranch.getRoot().getNodeType())) {
					INodeType nodeType = target.getOwningRule().getNodeType();
					ParseTreeBranch branch = new ParseTreeBranch(nodeType, oldBranch, target.getOwningRule(), this.inputLength);
					result.add(branch);
				}
				return result;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	@Override
	public List<IParseTree> visit(NonTerminal target, Object... arg) throws CannotGrowTreeException {
		return new ArrayList<>();
	}

	@Override
	public List<IParseTree> visit(TerminalPattern target, Object... arg) throws CannotGrowTreeException {
		return new ArrayList<>();
	}

	@Override
	public List<IParseTree> visit(TerminalLiteral target, Object... arg) throws CannotGrowTreeException {
		return new ArrayList<>();
	}

}
