package net.akehurst.language.ogl.semanticStructure;

import java.util.List;

abstract
public class AbstractChoice extends RuleItem {

	
	List<Concatenation> alternative;
	public List<Concatenation> getAlternative() {
		return this.alternative;
	}
}
