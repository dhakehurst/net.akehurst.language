package net.akehurst.language.ogl.semanticModel;

import java.util.HashSet;
import java.util.Set;



public class SeparatedList extends RuleItem {

	public SeparatedList(int min, TerminalLiteral separator, TangibleItem concatination) {
		this.min = min;
		this.separator = separator;
		this.concatination = concatination;
	}
	
	@Override
	public void setOwningRule(Rule value) {
		this.owningRule = value;
		this.getConcatination().setOwningRule(value);
		this.getSeparator().setOwningRule(value);
	}
	
	int min;
	public int getMin() {
		return this.min;
	}
	
	TerminalLiteral separator;
	public TerminalLiteral getSeparator() {
		return this.separator;
	}
	
	TangibleItem concatination;
	public TangibleItem getConcatination() {
		return this.concatination;
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
//		result.addAll( this.getConcatination().findFirstTangibleItem() );
//		return result;
//	}
//	
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		result.add(this.getSeparator());
		result.addAll( this.getConcatination().findAllTerminal() );
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		result.addAll( this.getConcatination().findAllNonTerminal() );
		return result;
	}
	
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		// TODO Auto-generated method stub
//		return false;
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "( "+this.getConcatination()+" / "+this.getSeparator()+" )"+(this.min==0?"*":"+");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof SeparatedList) {
			SeparatedList other = (SeparatedList)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
	
}
