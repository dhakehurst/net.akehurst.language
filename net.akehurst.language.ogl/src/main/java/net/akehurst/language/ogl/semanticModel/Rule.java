package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeType;

public class Rule {

	public Rule(Grammar grammar, String name) {
		this.grammar = grammar;
		this.name = name;
	}
	
	Grammar grammar;
	public Grammar getGrammar() {
		return this.grammar;
	}
	
	String name;
	public String getName(){
		return name;
	}

	RuleItem rhs;
	public RuleItem getRhs(){
		return rhs;
	}
	public void setRhs(RuleItem value) {
		this.rhs = value;
		this.rhs.setOwningRule(this);
	}
	
//	public Set<TangibleItem> findFirstTangibleItem() {
//		return this.getRhs().findFirstTangibleItem();
//	}
//	
//	public Set<Terminal> findFirstTerminal() throws RuleNotFoundException {
//		return this.getRhs().findFirstTerminal();
//	}
	
	public INodeType getNodeType() {
		return new RuleNodeType(this);
	}
	
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		return this.getRhs().isMatchedBy(node);
//	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getName()+" = "+this.getRhs();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Rule) {
			Rule other = (Rule)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
