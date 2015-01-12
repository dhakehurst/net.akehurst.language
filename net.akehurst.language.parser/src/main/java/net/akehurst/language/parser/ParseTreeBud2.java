package net.akehurst.language.parser;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;

public class ParseTreeBud2 extends SubParseTree {

	public ParseTreeBud2(Terminal terminal, Character c, int inputLength) {
		this(new Leaf2(terminal, c), inputLength);

	}

	ParseTreeBud2(Leaf2 leaf, int inputLength) {
		super(inputLength);
		this.root = leaf;
		super.canGrow = this.getRoot().getIsPossible();
		super.complete = this.getRoot().getIsComplete();
	}
	
	@Override
	public Leaf2 getRoot() {
		return (Leaf2)super.getRoot();
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		// TODO Auto-generated method stub
		return null;
	}

	public ParseTreeBranch extendWith(IParseTree extension) throws CannotExtendTreeException {
		throw new CannotExtendTreeException();
	}
	
	public void tryExtendWith(Character character) {
	
		this.getRoot().tryExtendWith(character);
		super.canGrow =  this.getRoot().getIsPossible();
		super.complete = this.getRoot().getIsComplete();
	}
	
	public ParseTreeBud2 deepClone() {
		ParseTreeBud2 clone = new ParseTreeBud2(this.getRoot().deepClone(), this.inputLength);
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof ParseTreeBud2) {
			ParseTreeBud2 other = (ParseTreeBud2)arg;
			return this.getRoot().equals(other.getRoot());
		} else {
			return false;
		}
	}
}
