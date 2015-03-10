package net.akehurst.language.ogl.semanticModel;

import java.util.HashSet;
import java.util.Set;



public class Multi extends RuleItem {

	public Multi(int min, int max, TangibleItem item) {
		this.min = min;
		this.max = max;
		this.item = item;
	}
	
	@Override
	public void setOwningRule(Rule value) {
		this.owningRule = value;
		this.getItem().setOwningRule(value);
	}
	
	int min;
	public int getMin() {
		return this.min;
	}
	
	int max;
	public int getMax() {
		return this.max;
	}
	
	TangibleItem item;
	public TangibleItem getItem() {
		return this.item;
	}
	
//	@Override
//	public INodeType getNodeType() {
//		return new RuleNodeType(this.getOwningRule());
//	}
	
	@Override
	public <T, E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E {
		return visitor.visit(this, arg);
	}
	
//	public Set<TangibleItem> findFirstTangibleItem() {
//		Set<TangibleItem> result = new HashSet<>();
//		result.addAll( this.getItem().findFirstTangibleItem() );
//		return result;
//	}
//	
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		//must add the empty terminal first, or later we get over writing due to 
		// no identity distiction between tree with empty and tree without,
		//tree id uses position and empty leaf doesn't change the star-end position
		if (this.getMin() == 0) {
			result.add( new TerminalEmpty() ); //empty terminal
		}
		result.addAll( this.getItem().findAllTerminal() );
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		result.addAll( this.getItem().findAllNonTerminal() );
		return result;
	}
//	
//	@Override
//	public boolean isMatchedBy(INode node) {
//		// TODO Auto-generated method stub
//		return false;
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getItem() + (0==min?(0==max?"*":"?"):"+");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Multi) {
			Multi other = (Multi)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
