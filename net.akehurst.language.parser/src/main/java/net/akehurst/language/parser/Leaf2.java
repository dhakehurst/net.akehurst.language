package net.akehurst.language.parser;

import java.util.regex.Matcher;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.ogl.semanticModel.LeafNodeType;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.ogl.semanticModel.TerminalPattern;

public class Leaf2 extends AbstractNode implements ILeaf {

	public Leaf2(Terminal terminal, Character firstCharacter) {
		this(terminal);
		this.tryExtendWith(firstCharacter);
	}

	 Leaf2(Terminal terminal) {
		super((terminal instanceof TerminalLiteral)?new LeafNodeType((TerminalLiteral)terminal):new LeafNodeType((TerminalPattern)terminal));
		this.matchedText = new StringBuffer();
		this.terminal = terminal;
		this.length = 0;
	 }
	
	Terminal terminal;
	public Terminal getTerminal() {
		return this.terminal;
	}
	
	@Override
	public LeafNodeType getNodeType() {
		return (LeafNodeType)super.getNodeType();
	}
	
	StringBuffer matchedText;
	public String getMatchedText() {
		return this.matchedText.toString();
	}
	
	boolean isComplete;
	public boolean getIsComplete() {
		return this.isComplete;
	}
	
	boolean isPossible;
	public boolean getIsPossible() {
		return this.isPossible;
	}
	
	public void tryExtendWith(Character c) {
		Matcher m = this.terminal.getPattern().matcher(this.getMatchedText()+c.toString());
		this.isComplete = m.matches();
		this.isPossible = m.hitEnd();
		if (this.isComplete || this.isPossible) {
			this.matchedText.append(c);
			this.length = this.matchedText.length();
		}
	}
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	public Leaf2 deepClone() {
		Leaf2 clone = new Leaf2(this.terminal);
		clone.matchedText = this.matchedText;
		clone.isComplete = this.isComplete;
		clone.isPossible = this.isPossible;
		clone.length = this.length;
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
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Leaf2) {
			Leaf2 other = (Leaf2)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}

}
