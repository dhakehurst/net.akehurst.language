package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatination;
import net.akehurst.language.ogl.semanticModel.LeafNodeType;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.CannotExtendTreeException;
import net.akehurst.language.parser.CannotGrowTreeException;

public abstract class AbstractParseTree implements IParseTree {

	public AbstractParseTree(Input input, INode root) {
		this.input = input;
		this.root = root;
	}

	Input input;
	INode root;

	@Override
	public INode getRoot() {
		return this.root;
	}

	abstract boolean getIsComplete();

	abstract boolean getCanGrow();

	public abstract TangibleItem getNextExpectedItem();

	public abstract AbstractParseTree deepClone();

	public boolean getIsSkip() throws ParseTreeException {
		return this.getRoot().getNodeType() instanceof SkipNodeType;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	public List<AbstractParseTree> createNewBuds(CharSequence text, Set<Terminal> allTerminal) throws RuleNotFoundException {
		int pos = this.getRoot().getMatchedTextLength();
		String subString = text.toString().substring(pos);
		List<AbstractParseTree> buds = new ArrayList<>();
		buds.add(new ParseTreeEmptyBud(this.input)); // always add empty bud as a new bud
		for (Terminal terminal : allTerminal) {
			Matcher m = terminal.getPattern().matcher(subString);
			if (m.lookingAt()) {
				String matchedText = subString.substring(0, m.end());
				int start = pos + m.start();
				int end = pos + m.end();
				Leaf leaf = new Leaf(this.input, start, end, terminal);
				ParseTreeBud bud = new ParseTreeBud(this.input, leaf);
				buds.add(bud);
			}
		}
		return buds;
	}

	public List<IParseTree> grow(List<Rule> rules) throws CannotGrowTreeException {
		List<IParseTree> grownTrees = new ArrayList<>();
		if (this.getIsComplete()) {
			grownTrees.add(this);
			for (Rule rule : rules) {
				List<IParseTree> newTree = this.grow(rule.getRhs());
				for (IParseTree nt : newTree) {
					if (((AbstractParseTree) nt).getIsComplete()) {
						List<IParseTree> newTree2 = ((AbstractParseTree) nt).grow(rules);
						grownTrees.addAll(newTree2);
					}
				}
				grownTrees.addAll(newTree);
			}
		}
		return grownTrees;
	}

	public AbstractParseTree expand(AbstractParseTree newBranch) throws CannotExtendTreeException, RuleNotFoundException, ParseTreeException {
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

	public List<IParseTree> grow(RuleItem item) throws CannotGrowTreeException {
		if (item instanceof Concatination) {
			return this.grow((Concatination) item);
		} else if (item instanceof Choice) {
			return this.grow((Choice) item);
		} else if (item instanceof Multi) {
			return this.grow((Multi) item);
		} else if (item instanceof SeparatedList) {
			return this.grow((SeparatedList) item);
		} else if (item instanceof NonTerminal) {
			return this.grow((NonTerminal) item);
		} else if (item instanceof Terminal) {
			return this.grow((Terminal) item);
		} else {
			throw new CannotGrowTreeException("RuleItem subtype not handled.");
		}
	}

	IParseTree growMe(RuleItem target) {
		INodeType nodeType = target.getOwningRule().getNodeType();
		List<INode> children = new ArrayList<>();
		children.add(this.getRoot());
		Branch newBranch = new Branch(nodeType, children);
		ParseTreeBranch newTree = new ParseTreeBranch(this.input, newBranch, target.getOwningRule(), 1);
		return newTree;
	}
	
	public List<IParseTree> grow(Concatination target) throws CannotGrowTreeException {
		try {
			List<IParseTree> result = new ArrayList<>();
			if (target.getItem().get(0).getNodeType().equals(this.getRoot().getNodeType())) {
				IParseTree newTree = this.growMe(target);
				result.add(newTree);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	public List<IParseTree> grow(Choice target) throws CannotGrowTreeException {
		try {
			List<IParseTree> result = new ArrayList<>();
			for (TangibleItem alt : target.getAlternative()) {
				if (this.getRoot().getNodeType().equals(alt.getNodeType())) {
					IParseTree newTree = this.growMe(target);
					result.add(newTree);
				}
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Should not happend");
		}
	}

	public List<IParseTree> grow(Multi target) throws CannotGrowTreeException {
		try {
			IParseTree oldBranch = this;
			List<IParseTree> result = new ArrayList<>();
			if (target.getItem().getNodeType().equals(oldBranch.getRoot().getNodeType())
					|| (0 == target.getMin() && oldBranch.getRoot().getNodeType().equals(new LeafNodeType()))) {
				IParseTree newTree = this.growMe(target);
				result.add(newTree);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	public List<IParseTree> grow(SeparatedList target) throws CannotGrowTreeException {
		try {
			IParseTree oldBranch = this;
			List<IParseTree> result = new ArrayList<>();
			if (target.getConcatination().getNodeType().equals(oldBranch.getRoot().getNodeType())) {
				IParseTree newTree = this.growMe(target);
				result.add(newTree);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	public List<IParseTree> grow(NonTerminal target) throws CannotGrowTreeException {
		return new ArrayList<>();
	}
	public List<IParseTree> grow(Terminal target) throws CannotGrowTreeException {
		return new ArrayList<>();
	}
}
