package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatenation;
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

	public AbstractParseTree(Factory factory, Input input, INode root, AbstractParseTree stackedTree) {
		this.factory = factory;
		this.input = input;
		this.root = root;
		this.stackedTree = stackedTree;
	}

	Factory factory;
	Input input;
	INode root;
	
	AbstractParseTree stackedTree;
	public AbstractParseTree getStackedTree() {
		return this.stackedTree;
	}
	
	@Override
	public INode getRoot() {
		return this.root;
	}
	
	public boolean getIsEmpty() {
		return this.getRoot().getIsEmpty();
	}

	public AbstractParseTree peekTopStackedRoot() {//throws ParseTreeException {
		//if (null==this.stackedTree) {
		//	throw new ParseTreeException("Nothing on the stack", null);
		//} else {
			return this.stackedTree;
		//}
	}

	abstract public boolean getIsComplete();

	public abstract boolean getCanGrow();

	abstract boolean getCanGraftBack();

	abstract boolean getCanGrowWidth();
	
	public abstract TangibleItem getNextExpectedItem() ;

//	public abstract AbstractParseTree deepClone();

	public boolean getIsSkip() throws ParseTreeException {
		return this.getRoot().getNodeType() instanceof SkipNodeType;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

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
	public Set<AbstractParseTree> growWidth(Set<Terminal> allTerminal, Set<Rule> rules) throws RuleNotFoundException, ParseTreeException {
		Set<AbstractParseTree> result = new HashSet<>();
		if ( this.getCanGrowWidth() ) { //don't grow width if its complete...cant graft back
			List<ParseTreeBud> buds = this.input.createNewBuds(allTerminal, this.getRoot().getEnd());
	// doing this causes non termination of parser
	//		ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
	//		buds.add(empty);
			for (ParseTreeBud bud : buds) {
				AbstractParseTree nt = this.pushStackNewRoot(bud.getRoot());
				Set<AbstractParseTree> nts = nt.growHeight(rules);
				if (nts.isEmpty()) {
					result.add(nt);
				} else {
					result.addAll(nts);
				}
			}
		}
		return result;
	}

	AbstractParseTree pushStackNewRoot(ILeaf n) {
		return new ParseTreeBud(this.factory, this.input, n, this);
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
	public AbstractParseTree tryGraftBack() throws RuleNotFoundException {//, CannotGraftBackException {
//		try {
//			if (this.getCanGraftBack()) {
				AbstractParseTree parent = this.peekTopStackedRoot();
//				if (parent.getCanGrow()) {
					return this.tryGraftInto(parent);
//				} else {
//					throw new CannotExtendTreeException("parent cannot grow");
//				}
//			} else {
//				throw new CannotExtendTreeException("tree not complete");
//			}
//		} catch (CannotExtendTreeException e) {
//			throw new CannotGraftBackException(e.getMessage(), e);
//		} catch (ParseTreeException e) { // StackEmpty
//			throw new CannotGraftBackException(e.getMessage(), null);
//		}
	}

	public Set<AbstractParseTree> growHeight(Set<Rule> rules) throws RuleNotFoundException, ParseTreeException {
		Set<AbstractParseTree> result = new HashSet<>();
		//result.add((AbstractParseTree) this);
		if (this.getIsComplete()) {
			for (Rule rule : rules) {
				if (this.getRoot().getNodeType().equals(rule.getNodeType())) {
					result.add(this);
				}
				try {
					List<IParseTree> newTrees = this.grow(rule.getRhs());
					for (IParseTree nt : newTrees) {
						if (((AbstractParseTree) nt).getIsComplete()) {
							Set<AbstractParseTree> newTree2 = ((AbstractParseTree) nt).growHeight(rules);
							if (newTree2.isEmpty()) {
								result.add((AbstractParseTree)nt);
							} else {
								result.addAll(newTree2);
							}
						} else {
							result.add((AbstractParseTree) nt);
						}
					}
				} catch (CannotGrowTreeException e) {
					//result.add((AbstractParseTree) this);
				}
			}
		} else {
			result.add(this);
		}
		return result;
	}

	AbstractParseTree tryGraftInto(AbstractParseTree parent) throws RuleNotFoundException {
		try {
//			if (parent instanceof ParseTreeBud) {
//				throw new CannotExtendTreeException("parent is a bud, cannot extend it");
//			} else {
				if (parent.getNextExpectedItem().getNodeType().equals(this.getRoot().getNodeType())) {
					return parent.extendWith(this.getRoot());
				} else if (this.getIsSkip()) {
					return parent.extendWith(this.getRoot());
				} else {
//					throw new CannotExtendTreeException("node is not next expected item or a skip node");
					return null;
				}
//			}
//		} catch (CannotExtendTreeException e) {
//			throw e;
//		} catch (NoNextExpectedItemException e) {
//			throw new CannotExtendTreeException(e.getMessage());
//		} catch (RuntimeException e) {
//			e.printStackTrace();
//			throw new CannotExtendTreeException(e.getMessage());
//		}
		} catch (ParseTreeException e) {
			throw new RuntimeException("Should not happen",e);
		}
	}

	public abstract ParseTreeBranch extendWith(INode extension) throws ParseTreeException;

	List<IParseTree> grow(RuleItem item) throws CannotGrowTreeException, RuleNotFoundException, ParseTreeException {
		if (item instanceof Concatenation) {
			return this.grow((Concatenation) item);
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
		IBranch newBranch = this.factory.createBranch(nodeType, children);
		ParseTreeBranch newTree = new ParseTreeBranch(this.factory, this.input, newBranch, this.stackedTree, target.getOwningRule(), 1);
		return newTree;
	}
	
	List<IParseTree> grow(Concatenation target) throws CannotGrowTreeException, RuleNotFoundException, ParseTreeException {

		List<IParseTree> result = new ArrayList<>();
		if (0 == target.getItem().size()) {
//			throw new CannotGrowTreeException("tree cannot grow with item " + target);
			//if (this.getRoot() instanceof EmptyLeaf) {
//				IParseTree newTree = this.empty(target, this.getRoot().getEnd());
//				result.add(newTree);
			//}
			
		} else if (target.getItem().get(0).getNodeType().equals(this.getRoot().getNodeType())) {
			IParseTree newTree = this.growMe(target);
			result.add(newTree);
		} else {
//			throw new CannotGrowTreeException("tree cannot grow with item " + target);
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
				
				//need to create a 'complete' version and a 'non-complete' version of the tree
				IParseTree newTree = this.growMe(target);
				ParseTreeBranch ntB = (ParseTreeBranch)newTree;
				ParseTreeBranch newTree2 = new ParseTreeBranch(this.factory, ntB.input, (IBranch)ntB.root, ntB.stackedTree, ntB.rule, ntB.nextItemIndex);
				newTree2.complete = true;
				result.add(newTree);
				//result.add(newTree2);
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
				ParseTreeBranch ntB = (ParseTreeBranch)newTree;
				ParseTreeBranch newTree2 = new ParseTreeBranch(this.factory, ntB.input, (IBranch)ntB.root, ntB.stackedTree, ntB.rule, ntB.nextItemIndex);
				newTree2.complete = false;
				result.add(newTree);
				//result.add(newTree2);
			} else {
				throw new CannotGrowTreeException(target.toString() + " cannot grow " + this);
			}
			return result;
		} catch (CannotGrowTreeException e) {
			throw e;
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
