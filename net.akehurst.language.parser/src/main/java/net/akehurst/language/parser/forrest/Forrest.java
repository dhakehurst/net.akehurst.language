package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.CannotExtendTreeException;
import net.akehurst.language.parser.CannotGrowTreeException;
import net.akehurst.language.parser.ToStringVisitor;

public 	class Forrest {

	public Forrest(INodeType goal, Grammar grammar, List<Rule> allRules, Input input) {
		this.goal = goal;
		this.grammar = grammar;
		this.allRules = allRules;
		this.input = input;
		this.canGrow = false;
		this.newGrownBranches = new HashSet<>();
	}
	Grammar grammar;
	List<Rule> allRules;
	INodeType goal;
	Input input;
	Set<AbstractParseTree> possibleTrees = new HashSet<>();
	Set<IParseTree> goalTrees = new HashSet<>();
	ArrayList<IParseTree> gt = new ArrayList<>();
	
	Set<AbstractParseTree> newGrownBranches;
	
	public ArrayList<IParseTree> getGT() {
		gt = new ArrayList<>();
		gt.addAll(this.goalTrees);
		return gt;
	}
	public Forrest clone() {
		Forrest clone = new Forrest(this.goal, this.grammar, this.allRules, this.input);
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
			IParseTree lt = this.getGT().get(0);
			for (IParseTree gt : this.goalTrees) {
				if (gt.getRoot().getMatchedTextLength() > lt.getRoot().getMatchedTextLength()) {
					lt = gt;
				}
			}
			if (lt.getRoot().getMatchedTextLength() < text.length()) {
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
	public Forrest grow() throws RuleNotFoundException, ParseTreeException {
		Forrest newForrest = new Forrest(this.goal, this.grammar, this.allRules, this.input);
		newForrest.goalTrees.addAll(this.goalTrees);

		for(AbstractParseTree tree: this.possibleTrees) {

			try {
				AbstractParseTree nt = tree.tryGraftBack();
				Set<AbstractParseTree> nts = nt.growHeight(this.allRules);
				newForrest.addAll(nts);
			} catch (CannotGraftBackException e) {
				int i=1;
				if (tree.getIsEmpty()) {
					//don't grow width
				} else {
					Set<AbstractParseTree> newBranches = tree.growWidth( this.grammar.getAllTerminal(), this.allRules );
					newForrest.addAll(newBranches);
				}
			}
			

		}

		return newForrest;
	}
	
	
	/**
	 * reduce
	 * for the given tree, see if any of the newly grown branches will expand it
	 * (are any of the new branches able to be the next expected node for the given tree)
	 * @throws ParseTreeException 
	 * @throws RuleNotFoundException 
	 *  
	 **/
//	Set<AbstractParseTree> growHeight(AbstractParseTree tree) throws RuleNotFoundException, ParseTreeException {
//		Set<AbstractParseTree> newGrowth = new HashSet<>();
//		for(AbstractParseTree newBranch: this.newGrownBranches) {
//			try {
//				AbstractParseTree newTree = tree.expand(newBranch);
//				newGrowth.add(newTree);
////				if (newTree.getIsComplete()) {
////					newGrowth.add(newTree);
////					List<IParseTree> newTrees = newTree.grow(this.grammar.getRule());
////					for (IParseTree pt : newTrees) {
////						AbstractParseTree npt = (AbstractParseTree) pt;
////						newForrest.add(npt);
////						//expanded = true;
////					}
//					//newGrowth.add(newTree);
////				} else {
////					newForrest.add(newTree);
////					//expanded = true;
////				}
//			} catch (CannotExtendTreeException e) {
//				//Can't extend tree...yet!
//				//if do this: newForrest.add(tree);
//				// doesn't terminate
//				// could make the isComplete dynamically evaluated, but this is time consuming!
//				newGrowth.add(newBranch);
////			} catch (CannotGrowTreeException e) {
////			
//			}
//		}
//		return newGrowth;
//	}
	
	/** shift 
	 * @throws RuleNotFoundException 
	 * @throws ParseTreeException **/
//	Set<AbstractParseTree> growNewBranches(AbstractParseTree tree) throws RuleNotFoundException, ParseTreeException {
//		Set<AbstractParseTree> newGrowth = new HashSet<>();
//		List<AbstractParseTree> buds = tree.createNewBuds(this.input.text, this.grammar.getAllTerminal()); //grab all possible next tokens
//		buds.add(new ParseTreeEmptyBud(input, -1));
//		List<AbstractParseTree> newBranches = buds;
//		newBranches.addAll(this.newGrownBranches);
//		for(AbstractParseTree ng : newBranches) {
//			try {
//				List<IParseTree> newTrees = ng.grow(this.grammar.getRule());
//				for (IParseTree pt : newTrees) {
//					AbstractParseTree npt = (AbstractParseTree) pt;
//					newGrowth.add(npt);
//					//grown = true;
//				}
//			} catch (CannotGrowTreeException e) {
//				
//			}
//		}
//		return newGrowth;
//	}

	
	public void addNonGrowing(AbstractParseTree tree) {
		possibleTrees.add(tree);
	}
	
	public void addAll(Collection<? extends AbstractParseTree> trees) throws ParseTreeException {
		for(AbstractParseTree tree: trees) {
			this.add(tree);
		}
	}
	
	public void add(AbstractParseTree tree) throws ParseTreeException {
		if (tree.getIsComplete()) {
			this.newGrownBranches.add(tree);
			if ( tree.stackedRoots.isEmpty() && goal.equals(tree.getRoot().getNodeType()) ) {
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
	
	public Forrest shallowClone() {
		Forrest clone = new Forrest(this.goal, this.grammar, this.allRules, this.input);
		clone.canGrow = this.canGrow;
		clone.goalTrees.addAll(this.goalTrees);
		clone.possibleTrees.addAll(this.possibleTrees);
		clone.newGrownBranches.addAll(this.newGrownBranches);
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String s = "Forrest {";
		s += "goal=="+this.goal;
		s+= ", canGrow=="+this.possibleTrees;
		s+= "}";
		return s;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Forrest) {
			Forrest other = (Forrest)arg;
			return this.possibleTrees.equals(other.possibleTrees);
		} else {
			return false;
		}
	}
}

