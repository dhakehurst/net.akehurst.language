package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeType;


@Deprecated
public class Group extends ConcatinationItem {

	public Group(TangibleItem choice) {
		this.choice = choice;
	}
	
	public void setOwningRule(Rule value) {
		this.owningRule = value;
		this.getChoice().setOwningRule(value);
	}
	
	TangibleItem choice;
	public TangibleItem getChoice() {
		return this.choice;
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
//		return this.getChoice().findFirstTangibleItem();
//	}
//	
//	public Set<Terminal> findFirstTerminal() throws RuleNotFoundException {
//		return this.getChoice().findFirstTerminal();
//	}
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		//TODO this need to check for being a pseudonode I think
//		return this.getChoice().isMatchedBy(node);
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "(" + this.getChoice() + ")";
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Group) {
			Group other = (Group)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
