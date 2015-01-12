package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeType;

public class NonTerminal extends TangibleItem {

	public NonTerminal(String referencedRuleName) {
		this.referencedRuleName = referencedRuleName;
	}
	
	String referencedRuleName;
	Rule referencedRule;
	public Rule getReferencedRule() throws RuleNotFoundException {
		if (null == this.referencedRule) {
			this.referencedRule = this.getOwningRule().getGrammar().findRule(this.referencedRuleName);
		}
		return this.referencedRule;
	}

	public INodeType getNodeType() throws RuleNotFoundException {
		return new RuleNodeType(this.getReferencedRule());
	}
	
	@Override
	public <T, E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E {
		return visitor.visit(this, arg);
	}
	
//	public Set<TangibleItem> findFirstTangibleItem() {
//		Set<TangibleItem> result = new HashSet<>();
//		result.add( this );
//		return result;
//	}
//	
//	public Set<Terminal> findFirstTerminal() throws RuleNotFoundException {
//		Set<Terminal> result = new HashSet<>();
//		Set<TangibleItem> items = this.getReferencedRule().findFirstTangibleItem();
//		items.remove(this);
//		for(TangibleItem item: items) {
//			result.addAll( item.findFirstTerminal() );
//		}
//		return result;
//	}
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		return node.getNodeType().equals(this.getNodeType());
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		try {
			return this.getReferencedRule().getName();
		} catch (RuleNotFoundException e) {
			return "ERROR: cannot find rule "+this.referencedRuleName;
		}
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof NonTerminal) {
			NonTerminal other = (NonTerminal)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
