package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;

public abstract class SubParseTree implements IParseTree {

	public SubParseTree(int inputLength) {
		this.inputLength = inputLength;
		this.grower = new ParseTreeGrower(inputLength);
	}
	
	int inputLength;
	AbstractNode root;

	@Override
	public AbstractNode getRoot() {
		return this.root;
	}

	boolean complete;

	public boolean getIsComplete() {
		return this.complete;
	}

	boolean canGrow;

	public boolean getCanGrow() {
		return this.canGrow;
	}

	public abstract TangibleItem getNextExpectedItem();

	public abstract SubParseTree deepClone();

	public boolean getIsSkip() {
		return this.getRoot().getNodeType() instanceof SkipNodeType;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	public List<SubParseTree> createNewBuds(CharSequence text, Set<Terminal> allTerminal) throws RuleNotFoundException {
		int pos = this.getRoot().getLength();
		String subString = text.toString().substring(pos);
		List<SubParseTree> buds = new ArrayList<>();
		buds.add(new ParseTreeEmptyBud(text.length())); //always add empty bud as a new bud
		for (Terminal terminal : allTerminal) {
			Matcher m = terminal.getPattern().matcher(subString);
			if (m.lookingAt()) {
				String matchedText = subString.substring(0, m.end());
				Leaf leaf = new Leaf(terminal.getNodeType(), matchedText);
				ParseTreeBud bud = new ParseTreeBud(leaf, text.length());
				buds.add(bud);
			}
		}
		return buds;
	}

	ParseTreeGrower grower;

	public List<IParseTree> grow(List<Rule> rules) throws CannotGrowTreeException {
		List<IParseTree> grownTrees = new ArrayList<>();
		if (this.getIsComplete()) {
			grownTrees.add(this);
			for (Rule rule : rules) {
				List<IParseTree> newTree = rule.getRhs().accept(this.grower, this);
				for (IParseTree nt : newTree) {
					if (((SubParseTree) nt).getIsComplete()) {
						List<IParseTree> newTree2 = ((SubParseTree) nt).grow(rules);
						grownTrees.addAll(newTree2);
					}
				}
				grownTrees.addAll(newTree);
			}
		}
		return grownTrees;
	}

	public SubParseTree expand(SubParseTree newBranch) throws CannotExtendTreeException, RuleNotFoundException, ParseTreeException {
		if (this != newBranch && newBranch.getIsComplete()) {
			if (this.getNextExpectedItem().getNodeType().equals(newBranch.getRoot().getNodeType())) {
				return this.extendWith(newBranch);
			} else if (newBranch.getIsSkip()) {
				return this.extendWith(newBranch);
			} else {
				throw new CannotExtendTreeException();
			}
		} else {
			throw new CannotExtendTreeException();
		}
	}

	public abstract ParseTreeBranch extendWith(IParseTree extension) throws CannotExtendTreeException, ParseTreeException;
}
