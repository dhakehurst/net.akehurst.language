package net.akehurst.language.ogl.semanticModel;

import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.core.parser.INodeType;

public class NonTerminal extends TangibleItem {

	public NonTerminal(String referencedRuleName) {
		this.referencedRuleName = referencedRuleName;
	}
	
	String referencedRuleName;
	Rule referencedRule;
	public Rule getReferencedRule() throws RuleNotFoundException {
		if (null == this.referencedRule) {
			this.referencedRule = this.getOwningRule().getGrammar().findAllRule(this.referencedRuleName);
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
	@Override
	public Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		result.add(this);
		return result;
	}
	
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		return node.getNodeType().equals(this.getNodeType());
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.referencedRuleName;
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
