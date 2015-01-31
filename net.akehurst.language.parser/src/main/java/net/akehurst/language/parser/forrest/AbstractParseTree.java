package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.ILeaf;
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

	public AbstractParseTree(Input input, INode root, Stack<AbstractParseTree> stack) {
		this.input = input;
		this.root = root;
		this.stackedRoots = stack;
	}

	Input input;
	INode root;
	Stack<AbstractParseTree> stackedRoots;

	@Override
	public INode getRoot() {
		return this.root;
	}

	public AbstractParseTree peekTopStackedRoot() throws ParseTreeException {
		if (this.stackedRoots.isEmpty()) {
			throw new ParseTreeException("Nothing on the stack", null);
		} else {
			return this.stackedRoots.peek();
		}
	}

	abstract public boolean getIsComplete();

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

	// List<ILeaf> createNewBuds(Set<Terminal> allTerminal) throws RuleNotFoundException {
	// int pos = this.getRoot().getEnd();
	// String subString = this.input.text.toString().substring(pos);
	// List<ILeaf> buds = new ArrayList<>();
	// //buds.add(new ParseTreeEmptyBud(this.input)); // always add empty bud as a new bud
	// for (Terminal terminal : allTerminal) {
	// try {
	// // if (terminal.getNodeType() instanceof SkipNodeType) {
	// ILeaf l = this.tryCreateBud(terminal, subString, pos);
	// buds.add(l);
	// // } else if ( terminal.getNodeType().equals( this.getNextExpectedItem().getNodeType() ) ) {
	// // ILeaf l = this.tryCreateBud(terminal, subString, pos);
	// // buds.add(l);
	// // }
	// } catch (CannotCreateNewBud e) {
	//
	// }
	// }
	// return buds;
	// }

	// ILeaf tryCreateBud(Terminal terminal, String subString, int pos) throws CannotCreateNewBud {
	// Matcher m = terminal.getPattern().matcher(subString);
	// if (m.lookingAt()) {
	// String matchedText = subString.substring(0, m.end());
	// int start = pos + m.start();
	// int end = pos + m.end();
	// Leaf leaf = new Leaf(this.input, start, end, terminal);
	// return leaf;
	// }
	// throw new CannotCreateNewBud();
	// }

	/**
	 * shift
	 * 
	 * look for next possible tokens for this tree - create buds if there are none then this tree can no longer grow for all possible buds clone a new tree with
	 * current root on the pushed stack, and new current root is the bud.
	 * 
	 * @throws RuleNotFoundException
	 * @throws ParseTreeException
	 * 
	 */
	public Set<AbstractParseTree> growWidth(Set<Terminal> allTerminal, List<Rule> rules) throws RuleNotFoundException, ParseTreeException {
		Set<AbstractParseTree> result = new HashSet<>();
		List<ParseTreeBud> buds = this.input.createNewBuds(allTerminal, this.getRoot().getEnd());
		for (ParseTreeBud bud : buds) {
			Stack<AbstractParseTree> stack = new Stack<>();
			stack.addAll(this.stackedRoots);
			stack.push(this);
			ParseTreeBud nt = new ParseTreeBud(this.input, bud.getRoot(), stack);
			Set<AbstractParseTree> nts = nt.growHeight(rules);
			result.addAll(nts);
		}

		return result;
	}

	/**
	 * reduce for this tree, see if the current root will expand (fit into the top stacked root)
	 * 
	 * 
	 * @throws ParseTreeException
	 * @throws RuleNotFoundException
	 * @throws CannotGraftBackException
	 * 
	 **/
	public AbstractParseTree tryGraftBack() throws RuleNotFoundException, CannotGraftBackException {
		try {
			AbstractParseTree parent = this.peekTopStackedRoot();
			if (parent.getIsComplete()) {
				throw new CannotExtendTreeException();
			} else {
				return this.tryGraftInto(parent);
			}
		} catch (CannotExtendTreeException e) {
			throw new CannotGraftBackException("", e);
		} catch (ParseTreeException e) { // StackEmpty
			throw new CannotGraftBackException("", null);
		}
	}

	public Set<AbstractParseTree> growHeight(List<Rule> rules) throws RuleNotFoundException, ParseTreeException {
		Set<AbstractParseTree> result = new HashSet<>();
		for (Rule rule : rules) {
			try {
				List<IParseTree> newTrees = this.grow(rule.getRhs());
				for (IParseTree nt : newTrees) {
					if (((AbstractParseTree) nt).getIsComplete()) {
						Set<AbstractParseTree> newTree2 = ((AbstractParseTree) nt).growHeight(rules);
						result.addAll(newTree2);
					} else {
						result.add((AbstractParseTree) nt);
					}
				}
			} catch (CannotGrowTreeException e) {
				result.add((AbstractParseTree) this);
			}
		}

		return result;
	}

	AbstractParseTree tryGraftInto(AbstractParseTree parent) throws CannotExtendTreeException, RuleNotFoundException, ParseTreeException {
		try {
			if (parent.getNextExpectedItem().getNodeType().equals(this.getRoot().getNodeType())) {
				return parent.extendWith(this.getRoot());
			} else if (this.getIsSkip()) {
				return parent.extendWith(this.getRoot());
			} else {
				throw new CannotExtendTreeException();
			}
		} catch (CannotExtendTreeException e) {
			throw e;
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw new CannotExtendTreeException();
		}
	}

	public abstract ParseTreeBranch extendWith(INode extension) throws CannotExtendTreeException, ParseTreeException;

	List<IParseTree> grow(RuleItem item) throws CannotGrowTreeException, RuleNotFoundException, ParseTreeException {
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
		Stack<AbstractParseTree> stack = new Stack<>();
		stack.addAll(this.stackedRoots);
		ParseTreeBranch newTree = new ParseTreeBranch(this.input, newBranch, stack, target.getOwningRule(), 1);
		return newTree;
	}

	List<IParseTree> grow(Concatination target) throws CannotGrowTreeException, RuleNotFoundException, ParseTreeException {

		List<IParseTree> result = new ArrayList<>();
		if (0 == target.getItem().size()) {
			if (this.getRoot() instanceof EmptyLeaf) {
				IParseTree newTree = this.growMe(target);
				result.add(newTree);
			}
		} else if (target.getItem().get(0).getNodeType().equals(this.getRoot().getNodeType())) {
			IParseTree newTree = this.growMe(target);
			result.add(newTree);
		} else {
			throw new CannotGrowTreeException("tree cannot grow with item " + target);
		}
		return result;

	}

	List<IParseTree> grow(Choice target) throws CannotGrowTreeException {
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

	List<IParseTree> grow(Multi target) throws CannotGrowTreeException {
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

	List<IParseTree> grow(SeparatedList target) throws CannotGrowTreeException {
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

	List<IParseTree> grow(NonTerminal target) throws CannotGrowTreeException {
		return new ArrayList<>();
	}

	List<IParseTree> grow(Terminal target) throws CannotGrowTreeException {
		return new ArrayList<>();
	}
}
