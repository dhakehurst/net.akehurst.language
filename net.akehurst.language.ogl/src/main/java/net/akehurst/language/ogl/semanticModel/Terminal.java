package net.akehurst.language.ogl.semanticModel;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class Terminal extends TangibleItem {

	public Terminal(String value) {
		this.value = value;
	}
	
	String value;
	public String getValue() {
		return this.value;
	}

	abstract public Pattern getPattern();
	
	public boolean matches(String value) {
		return this.getPattern().matcher(value).matches();
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
		result.add(this);
		return result;
	}
	
	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		Set<NonTerminal> result = new HashSet<>();
		return result;
	}
	
//	
//	@Override
//	public boolean isMatchedBy(INode node) throws RuleNotFoundException {
//		return node.getNodeType().equals(this.getNodeType());
//	}
}
