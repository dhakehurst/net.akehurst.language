package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.IBranch;
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

public class ParseTreeBranch2 implements IParseTree {

	public ParseTreeBranch2(Input input, INode root, Stack<INode> stack, Rule rule, int nextItemIndex) {
		this.input = input;
		this.root = root;
		this.stackedRoots = stack;
		this.rule = rule;
		this.nextItemIndex = nextItemIndex;
		this.canGrow = this.calculateCanGrow();
		this.complete = this.calculateIsComplete();
	}

	Input input;
	INode root;
	Stack<INode> stackedRoots;
	Rule rule;
	int nextItemIndex;
	boolean canGrow;
	boolean complete;
	
	@Override
	public INode getRoot() {
		return this.root;
	}
	
	public INode peekTopStackedRoot() {
		if (this.stackedRoots.isEmpty()) {
			throw ?
		} else {
			return this.stackedRoots.peek();
		}
	}

	boolean getCanGrow() {
		return this.canGrow;
	}
	
	@Override
	public boolean getIsComplete() {
		return this.complete;
	}

	public TangibleItem getNextExpectedItem() {
		RuleItem item = this.rule.getRhs();
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return c.getItem().get(this.nextItemIndex);
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			return m.getItem();
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			if ( (this.nextItemIndex % 2) == 1 ) {
				return sl.getSeparator();
			} else {
				return sl.getConcatination();
			}
		} else {
			throw new RuntimeException("Should never happen");
		}
	}

	public boolean getIsSkip() throws ParseTreeException {
		return this.getRoot().getNodeType() instanceof SkipNodeType;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	List<INode> createNewBuds(Set<Terminal> allTerminal) throws RuleNotFoundException {
		int pos = this.getRoot().getEnd();
		String subString = this.input.text.toString().substring(pos);
		List<INode> buds = new ArrayList<>();
		//buds.add(new ParseTreeEmptyBud(this.input)); // always add empty bud as a new bud
		for (Terminal terminal : allTerminal) {
			boolean createBud = terminal.getNodeType().equals( this.getNextExpectedItem().getNodeType() )
					|| terminal.getNodeType() instanceof SkipNodeType
					;
			if ( createBud ) {
				Matcher m = terminal.getPattern().matcher(subString);
				if (m.lookingAt()) {
					String matchedText = subString.substring(0, m.end());
					int start = pos + m.start();
					int end = pos + m.end();
					Leaf leaf = new Leaf(this.input, start, end, terminal);
					buds.add(leaf);
				}
			}
		}
		return buds;
	}

	public List<IParseTree> grow(List<Rule> rules) throws CannotGrowTreeException {
		List<IParseTree> grownTrees = new ArrayList<>();
		if (this.getIsComplete()) {
			grownTrees.add(this);
			for (Rule rule : rules) {
				List<IParseTree> newTrees = this.grow(rule.getRhs());
				for (IParseTree nt : newTrees) {
					if (((ParseTreeBranch2) nt).getIsComplete()) {
						List<IParseTree> newTree2 = ((ParseTreeBranch2) nt).grow(rules);
						grownTrees.addAll(newTree2);
					} else {
						grownTrees.add(nt);
					}
				}
				//grownTrees.addAll(newTree);
			}
		}
		return grownTrees;
	}

	
	public Set<ParseTreeBranch2> expand(Set<Terminal> allTerminal) throws CannotExtendTreeException, RuleNotFoundException, ParseTreeException {
		Set<ParseTreeBranch2> result = new HashSet<>();
		List<INode> buds = this.createNewBuds(allTerminal);
		for(INode newBranch : buds) {
			
			if (newBranch.getIsComplete()) {
				if (this.getNextExpectedItem().getNodeType().equals(newBranch.getRoot().getNodeType())) {
					result.add(this.extendWith(newBranch));
				} else if (newBranch.getIsSkip()) {
					result.add(this.extendWith(newBranch));
				} else {
					//?
				}
			} else {
				//? 
			}
			
		}
		return result;
	}

	/**
	 * shift
	 * 
	 * look for next possible tokens for this tree - create buds
	 * if there are none then this tree can no longer grow
	 * for all possible buds
	 *   clone a new tree with current root on the pushed stack, and new current root is the bud.
	 * @throws RuleNotFoundException 
	 * 
	 */
	Set<ParseTreeBranch2> growWidth(Set<Terminal> allTerminal) throws RuleNotFoundException {
		Set<ParseTreeBranch2> result = new HashSet<>();
		
		List<INode> buds = this.createNewBuds(allTerminal);
		for(INode newRoot : buds) {
			Stack<INode> stack = new Stack<INode>();
			stack.addAll(this.stackedRoots);
			
			ParseTreeBud nt = new ParseTreeBud(this.input, newRoot, stack, );
			
			
		}
		
		return result;
	}
	
	
	/**
	 * reduce
	 * for this tree, see if the current root will expand (fit into the top stacked root) it
	 * (are any of the new branches able to be the next expected node for the given tree)
	 * @throws ParseTreeException 
	 * @throws RuleNotFoundException 
	 *  
	 **/
	
	
	boolean calculateIsComplete() {
		RuleItem item = this.rule.getRhs();
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return c.getItem().size() <= this.nextItemIndex;
		} else if (item instanceof Choice) {
			return true;
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			int size = this.nextItemIndex;
			return m.getMin() <= size && (size <= m.getMax() || -1 == m.getMax());
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			int size = this.nextItemIndex;
			return (size % 2) == 1;
		} else {
			throw new RuntimeException("Should never happen");
		}
	}
	boolean calculateCanGrow() {
		RuleItem item = this.rule.getRhs();
		boolean reachedEnd = this.getRoot().getMatchedTextLength() >= this.input.getLength();
		if (reachedEnd) return false;
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return this.nextItemIndex < c.getItem().size();
		} else if (item instanceof Choice) {
			return false;
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			int size = this.nextItemIndex;
			return size < m.getMax();
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			int size = this.nextItemIndex;
			return true;
		} else {
			throw new RuntimeException("Should never happen");
		}
	}
	
	
	public ParseTreeBranch extendWith(IParseTree extension) throws ParseTreeException {
		IBranch nb = this.getRoot().addChild(extension.getRoot());

		if (extension.getRoot().getNodeType() instanceof SkipNodeType) {
			ParseTreeBranch newBranch = new ParseTreeBranch(this.input, nb, this.rule, this.nextItemIndex);
			return newBranch;			
		} else {
			ParseTreeBranch newBranch = new ParseTreeBranch(this.input, nb, this.rule, this.nextItemIndex+1);
			return newBranch;
		}
	}
	
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
			if (0==target.getItem().size()) {
				if (this.getRoot() instanceof EmptyLeaf) {
					IParseTree newTree = this.growMe(target);
					result.add(newTree);
				}
			} else if (target.getItem().get(0).getNodeType().equals(this.getRoot().getNodeType())) {
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
