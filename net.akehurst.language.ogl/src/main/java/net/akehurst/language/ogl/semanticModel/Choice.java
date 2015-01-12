package net.akehurst.language.ogl.semanticModel;

import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.INodeType;


public class Choice extends RuleItem {
	
	public Choice(TangibleItem... alternative) {
		this.alternative = Arrays.asList(alternative);
	}
	
	public void setOwningRule(Rule value) {
		this.owningRule = value;
		for(TangibleItem c: this.getAlternative()) {
			c.setOwningRule(value);
		}
	}
	
	List<TangibleItem> alternative;
	public List<TangibleItem> getAlternative() {
		return this.alternative;
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
//		for(Concatination c : this.getAlternative()) {
//			Set<TangibleItem> ft = c.findFirstTangibleItem();
//			result.addAll(ft);
//		}		return result;
//	}
//	
//	public Set<Terminal> findFirstTerminal() throws RuleNotFoundException {
//		Set<Terminal> result = new HashSet<>();
//		for(Concatination c : this.getAlternative()) {
//			Set<Terminal> ft = c.findFirstTerminal();
//			result.addAll(ft);
//		}
//		return result;
//	}
	
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		for(Concatination c : this.getAlternative()) {
//			boolean isMatched = c.isMatchedBy(node);
//			if (isMatched) {
//				return true;
//			}
//		}
//		return false;
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = "";
		for(TangibleItem a : this.getAlternative()) {
			r += a.toString() + " | ";
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Choice) {
			Choice other = (Choice)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
