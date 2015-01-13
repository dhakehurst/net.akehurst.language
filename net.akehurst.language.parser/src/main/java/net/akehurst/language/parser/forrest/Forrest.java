package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.parser.CannotExtendTreeException;
import net.akehurst.language.parser.CannotGrowTreeException;

public 	class Forrest {

	public Forrest(INodeType goal, Grammar grammar, Input input) {
		this.goal = goal;
		this.grammar = grammar;
		this.input = input;
		this.canGrow = false;
	}
	Grammar grammar;
	INodeType goal;
	Input input;
	List<AbstractParseTree> possibleTrees = new ArrayList<>();
	List<IParseTree> goalTrees = new ArrayList<>();

	public Forrest clone() {
		Forrest clone = new Forrest(this.goal, this.grammar, this.input);
		clone.possibleTrees.addAll(this.possibleTrees);
		clone.goalTrees.addAll(this.goalTrees);
		return clone;
	}
	
	boolean canGrow;
	public boolean getCanGrow() {
		return this.canGrow;
	}
	
	public IParseTree getLongestMatch(CharSequence text) throws ParseFailedException {
		if (!this.goalTrees.isEmpty() && this.goalTrees.size() >= 1) {
			IParseTree lt = this.goalTrees.get(0);
			for (IParseTree gt : this.goalTrees) {
				if (gt.getRoot().getLength() > lt.getRoot().getLength()) {
					lt = gt;
				}
			}
			if (lt.getRoot().getLength() < text.length()) {
				throw new ParseFailedException("Goal does not match full text");
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal");
		}
	}
	
	/**
	 * <code>
	 *  for each growable tree in the forrest
	 *    create new buds for that tree (grab all possible next tokens)
	 *    grow buds into all possible complete branches
	 *    try expand possible tree by those branches
	 *    add expanded trees to new Forrest (this will sort them into goal matches, possibles, or dropped)
	 * </code>
	 * @return
	 * @throws RuleNotFoundException 
	 * @throws ParseTreeException 
	 */
	public Forrest grow(CharSequence text) throws RuleNotFoundException, ParseTreeException {
		Forrest newForrest = new Forrest(this.goal, this.grammar, this.input);
		newForrest.goalTrees.addAll(this.goalTrees);
		for(AbstractParseTree tree: this.possibleTrees) {
			List<AbstractParseTree> buds = tree.createNewBuds(text, this.grammar.getAllTerminal()); //grab all possible next tokens
			//grow buds according to rules
			List<AbstractParseTree> newBranches = new ArrayList<>();
			for(AbstractParseTree bud : buds) {
				try {
					List<IParseTree> newTrees = bud.grow(this.grammar.getRule());
					for (IParseTree pt : newTrees) {
						AbstractParseTree npt = (AbstractParseTree) pt;
						newBranches.add(npt);
						newForrest.addIfGoal(npt);
					}
				} catch (CannotGrowTreeException e) {
					
				}
			}

			for(AbstractParseTree newBranch: newBranches) {
				try {
					AbstractParseTree newTree = tree.expand(newBranch);
					newForrest.add(newTree);
					if (newTree.getIsComplete()) {
						List<IParseTree> newTrees = newTree.grow(this.grammar.getRule());
						for (IParseTree pt : newTrees) {
							AbstractParseTree npt = (AbstractParseTree) pt;
							newForrest.add(npt);
						}
					}
				} catch (CannotExtendTreeException e) {
					
				} catch (CannotGrowTreeException e) {
				
				}
			}
		}
		return newForrest;
	}
		
	public void add(AbstractParseTree tree) throws ParseTreeException {
		if (tree.getIsComplete()) {
			if ( goal.equals(tree.getRoot().getNodeType()) ) {
				goalTrees.add(tree.deepClone());
			}
		}
			if (tree.getCanGrow()) {
				// tree is incomplete but still possible to grow it
				possibleTrees.add(tree);
				this.canGrow |= tree.getCanGrow();
			} else {
				// drop tree
				int i = 0;
			}
		
	}
	
	void addIfGoal(AbstractParseTree tree) throws ParseTreeException {
		if (tree.getIsComplete()) {
			if (goal.equals(tree.getRoot().getNodeType())) {
				goalTrees.add(tree.deepClone());
			}
		}
	}
}

