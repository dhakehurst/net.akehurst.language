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
package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;
import net.akehurst.language.ogl.semanticStructure.SkipNodeType;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.parser.CannotGrowTreeException;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.Leaf;
import net.akehurst.language.parser.runtime.Node;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.parser.runtime.RuntimeRuleSet;

public abstract class AbstractParseTree implements IParseTree {

	public AbstractParseTree(ForrestFactory factory, Node root, AbstractParseTree stackedTree) {
		this.ffactory = factory;
		this.root = root;
		this.stackedTree = stackedTree;
		this.identifier = new BranchIdentifier(root.getRuntimeRule().getRuleNumber(), root.getStart(), root.getEnd());
	}

	ForrestFactory ffactory;
	Node root;
	public BranchIdentifier identifier;
	
	AbstractParseTree stackedTree;
	public AbstractParseTree getStackedTree() {
		return this.stackedTree;
	}
	
	@Override
	public Node getRoot() {
		return this.root;
	}
	
	public boolean getIsEmpty() {
		return this.getRoot().getIsEmpty();
	}

	public AbstractParseTree peekTopStackedRoot() {
		return this.stackedTree;
	}

	/**
	 * true indicates that this tree is valid (complete) for the given rule
	 * it could still grow (width) if e.g. it is a multi or separated list 
	 * MayGrowHeight!
	 */
	abstract public boolean getIsComplete();

	public abstract boolean getCanGrow();

	abstract boolean getCanGraftBack();

	abstract boolean getCanGrowWidth();
	
	public abstract RuntimeRule getNextExpectedItem();

	/**
	 * if tree can grow in width or height and if it can at some point grow into something that can be 
	 * grafted back (reduced).
	 * 
	 * i.e., if the current node is a possible sub node of the current stackedTree
	 * 
	 * @return
	 */
	public boolean getHasPotential(RuntimeRuleSet runtimeRuleSet) {
		if (!this.getCanGrow()) {
			return false;
		} else {
			if(null==this.getStackedTree()) {
				return true;
			} else {
				RuntimeRule thisRule = this.getRoot().getRuntimeRule();
				RuntimeRule nextExpectedRule = this.getStackedTree().getNextExpectedItem();
				if (thisRule==nextExpectedRule || thisRule.getIsSkipRule()) {
					return true;
				} else {
					if (thisRule.getKind() == RuntimeRuleKind.NON_TERMINAL) {
//						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubRule(nextExpectedRule));
						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstSubRule(nextExpectedRule));
						boolean res = possibles.contains(thisRule);
						return res;
					} else {
//						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleSubTerminal(nextExpectedRule));
						List<RuntimeRule> possibles = Arrays.asList(runtimeRuleSet.getPossibleFirstTerminals(nextExpectedRule));
						boolean res = possibles.contains(thisRule);
						return res;
					}
				}
			}

		}
	}
	
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
	public ArrayList<AbstractParseTree> growWidth(RuntimeRule[] terminalRules, RuntimeRuleSet ruleSet) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree> result = new ArrayList<>();
		if ( this.getCanGrowWidth() ) { //don't grow width if its complete...cant graft back
			List<ParseTreeBud> buds = this.ffactory.createNewBuds(terminalRules, this.getRoot().getEnd());
	// doing this causes non termination of parser
	//		ParseTreeBud empty = new ParseTreeEmptyBud(this.input, this.getRoot().getEnd());
	//		buds.add(empty);
			for (ParseTreeBud bud : buds) {
				AbstractParseTree nt = this.pushStackNewRoot(bud.getRoot());
//				ArrayList<AbstractParseTree> nts = nt.growHeight(terminalRules, ruleSet);
				//if (nts.isEmpty()) {
					result.add(nt);
				//} else {
//					result.addAll(nts);
				//}
			}
		}
		return result;
	}

	AbstractParseTree pushStackNewRoot(Leaf n) {
		return this.ffactory.fetchOrCreateBud(n, this);
	}
	
	public ArrayList<AbstractParseTree> growWidthAndHeight(RuntimeRule[] terminalRules,RuntimeRuleSet runtimeRuleSet) throws RuleNotFoundException, ParseTreeException {
		return this.growWidthAndHeight1(terminalRules, runtimeRuleSet);
	}
	
	public ArrayList<AbstractParseTree> growWidthAndHeight1(RuntimeRule[] terminalRules,RuntimeRuleSet runtimeRuleSet) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree> result = new ArrayList<AbstractParseTree>();
		
		ArrayList<AbstractParseTree> nts = this.growHeight(terminalRules, runtimeRuleSet);
		result.addAll(nts);
		
		if (this.getCanGraftBack() ) {
			AbstractParseTree nt = this.tryGraftBack();
			if (null!=nt) {
				//RuntimeRule[] possibleSuperRules = this.runtimeRuleSet.getPossibleSuperRule(nt.getRoot().getRuntimeRule());
				ArrayList<AbstractParseTree> nts2 = nt.growWidthAndHeight(terminalRules, runtimeRuleSet);
				result.add(nt); // must add nt as well here, as higher multi rules might need it
				result.addAll(nts2);
			}
		}
		
		if (this.getCanGrow() ) {
			int i = 1;

			if (this.getIsEmpty() || (this.getIsComplete() && !this.getCanGrow())) {
				// don't grow width
			} else {
				ArrayList<AbstractParseTree> newBranches = this.growWidth(terminalRules, runtimeRuleSet);
				result.addAll(newBranches);
			}
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
	public ArrayList<AbstractParseTree> growHeight(RuntimeRule[] terminalRules, RuntimeRuleSet runtimeRuleSet) throws RuleNotFoundException, ParseTreeException {
//		RuntimeRule treeRR = this.getRoot().getRuntimeRule();
//		RuntimeRule[] terminalRules = runtimeRuleSet.getPossibleSubTerminal(treeRR);
		return this.growHeight3(terminalRules, runtimeRuleSet);
	}
	public ArrayList<AbstractParseTree> growHeight1(RuntimeRule[] terminalRules, RuntimeRuleSet ruleSet) {
		if (this.getIsComplete()) {
			ArrayList<AbstractParseTree> toGrowUp = new ArrayList<>();
			final ArrayList<AbstractParseTree> result = new ArrayList<>();
			toGrowUp.add(this);
			while(!toGrowUp.isEmpty()) {
				ArrayList<AbstractParseTree> newToGrowUp = new ArrayList<>();
				for(AbstractParseTree t: toGrowUp) {
					final RuntimeRule[] rules = ruleSet.getPossibleSuperRule(t.getRoot().getRuntimeRule());
					for (RuntimeRule rule : rules) {
						if (t.root.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
							result.add(t);
						}
						final ParseTreeBranch[] newTrees = t.grow(rule);
						for (ParseTreeBranch nt : newTrees) {
							result.add(nt);
							if (nt.getIsComplete()) {
								newToGrowUp.add(nt);
							}
						}
					}
				}
				toGrowUp = newToGrowUp;
			}
			
			return result;			
		} else {
			final ArrayList<AbstractParseTree> result = new ArrayList<>();
			result.add(this);
			return result;
		}
	}
	
	public ArrayList<AbstractParseTree> growHeight2(RuntimeRule[] terminalRules, RuntimeRuleSet ruleSet) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree> result = new ArrayList<>();
		//result.add((AbstractParseTree) this);
		if (this.getIsComplete()) {
			RuntimeRule[] rules = ruleSet.getPossibleSuperRule(this.getRoot().getRuntimeRule());
			for (RuntimeRule rule : rules) {
				if (this.root.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
					result.add(this);
				}
				ParseTreeBranch[] newTrees = this.grow(rule);
				for (ParseTreeBranch nt : newTrees) {
					if (nt.getIsComplete()) {
						ArrayList<AbstractParseTree> newTree2 = nt.growHeight(terminalRules, ruleSet);
						//if (newTree2.isEmpty()) {
							result.add(nt);
						//} else {
							result.addAll(newTree2);
						//}
					} else {
						result.add(nt);
					}
				}
			}
		} else {
			result.add(this);
		}
		return result;
	}

	public ArrayList<AbstractParseTree> growHeight3(RuntimeRule[] terminalRules, RuntimeRuleSet ruleSet) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree> result = new ArrayList<>();
		//result.add((AbstractParseTree) this);
		if (this.getIsComplete()) {
			RuntimeRule[] rules = ruleSet.getPossibleSuperRule(this.getRoot().getRuntimeRule());
			for (RuntimeRule rule : rules) {
				if (this.root.getRuntimeRule().getRuleNumber() == rule.getRuleNumber()) {
					result.add(this);
				}
				ParseTreeBranch[] newTrees = this.grow(rule);
				for (ParseTreeBranch nt : newTrees) {
////					if (nt.getIsComplete()) {
//						ArrayList<AbstractParseTree> newTree2 = nt.growWidthAndHeight(terminalRules, ruleSet);
//						//if (newTree2.isEmpty()) {
							result.add(nt);
//						//} else {
//							result.addAll(newTree2);
//						//}
////					} else {
////						result.add(nt);
////					}
				}
			}
		} else {
//			result.add(this);
		}
		return result;
	}
	
	AbstractParseTree tryGraftInto(AbstractParseTree parent) throws RuleNotFoundException {
		try {
//			if (parent instanceof ParseTreeBud) {
//				throw new CannotExtendTreeException("parent is a bud, cannot extend it");
//			} else {
				if (parent.getNextExpectedItem().getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber()) {
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
			throw new RuntimeException("Internal Error: Should not happen",e);
		}
	}

	public abstract ParseTreeBranch extendWith(INode extension) throws ParseTreeException;

	ParseTreeBranch[] grow(RuntimeRule runtimeRule) {
		switch(runtimeRule.getRhs().getKind()) {
		case CHOICE:
			return this.growChoice(runtimeRule);
		case CONCATENATION:
			return this.growConcatenation(runtimeRule);
		case MULTI:
			return this.growMulti(runtimeRule);
		case SEPARATED_LIST:
			return this.growSeparatedList(runtimeRule);
		default:
			break;
		}
		throw new RuntimeException("Internal Error: RuleItem kind not handled.");
	}

	ParseTreeBranch growMe(RuntimeRule target) {
		//INodeType nodeType = target.getOwningRule().getNodeType();
		INode[] children = new INode[] {this.getRoot()};
//		Branch newBranch = this.factory.fetchOrCreateBranch(target, children);
//		ParseTreeBranch newTree = new ParseTreeBranch(this.factory, this.input, newBranch, this.stackedTree, target, 1);
		ParseTreeBranch newTree = this.ffactory.fetchOrCreateBranch(target, children, this.stackedTree, 1);
		return newTree;
	}
	
	ParseTreeBranch[] growConcatenation(RuntimeRule target) {
		if (0 == target.getRhs().getItems().length) {
			return new ParseTreeBranch[0];
		} else if (target.getRhsItem(0).getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber() ) {
			ParseTreeBranch newTree = this.growMe(target);
			return new ParseTreeBranch[]{newTree};
		} else {
			return new ParseTreeBranch[0];
		}
	}

	ParseTreeBranch[] growChoice(RuntimeRule target) {
		RuntimeRule[] rrs = target.getRhs().getItems(this.getRoot().getRuntimeRule().getRuleNumber());
		ParseTreeBranch[] result = new ParseTreeBranch[rrs.length];
		for (int i=0; i<rrs.length; ++i) {
			ParseTreeBranch newTree = this.growMe(target);
			result[i] = newTree;
		}
		return result;
	}

	ParseTreeBranch[] growMulti(RuntimeRule target) {
		try {
			if (target.getRhsItem(0).getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber() 
			|| (0 == target.getRhs().getMultiMin() && this.getRoot() instanceof Leaf) ) {
				
				//need to create a 'complete' version and a 'non-complete' version of the tree
				ParseTreeBranch newTree = this.growMe(target);
				//ParseTreeBranch ntB = (ParseTreeBranch)newTree;
				//ParseTreeBranch newTree2 = new ParseTreeBranch(this.factory, ntB.input, (Branch)ntB.root, ntB.stackedTree, ntB.rule, ntB.nextItemIndex);
				//newTree2.complete = false;
				//result.add(newTree);
				//result.add(newTree2);
				return new ParseTreeBranch[] {newTree};
			} else {
				return new ParseTreeBranch[0];
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	ParseTreeBranch[] growSeparatedList(RuntimeRule target) {
		try {
			if (target.getRhsItem(0).getRuleNumber() == this.getRoot().getRuntimeRule().getRuleNumber() 
			|| (0 == target.getRhs().getMultiMin() && this.getRoot() instanceof Leaf) ) {
				ParseTreeBranch newTree = this.growMe(target);
//				ParseTreeBranch ntB = (ParseTreeBranch)newTree;
//				ParseTreeBranch newTree2 = new ParseTreeBranch(this.factory, ntB.input, (Branch)ntB.root, ntB.stackedTree, ntB.rule, ntB.nextItemIndex);
//				newTree2.complete = false;
				//result.add(newTree);
				//result.add(newTree2);
				return new ParseTreeBranch[] {newTree};
			} else {
				return new ParseTreeBranch[0];
			}
		} catch (Exception e) {
			throw new RuntimeException("Should not happen", e);
		}
	}

	public String getIdString() {
		String s = this.identifier.toString();
		AbstractParseTree t = this.stackedTree;
		while(null!=t) {
			s += " " + t.identifier;
			t = t.stackedTree;
		}
		return s;
	}

}
