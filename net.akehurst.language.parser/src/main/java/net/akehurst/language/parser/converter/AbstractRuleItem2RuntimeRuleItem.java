package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;

abstract
public class AbstractRuleItem2RuntimeRuleItem<L extends RuleItem> implements Relation<L, RuntimeRuleItem> {

	RuntimeRuleItemKind getRuleItemKind(RuleItem item) {
		if (item instanceof Choice) {
			return RuntimeRuleItemKind.CHOICE;
		} else if (item instanceof Concatenation) {
			return RuntimeRuleItemKind.CONCATENATION;
		} else if (item instanceof Multi) {
			return RuntimeRuleItemKind.MULTI;
		} else if (item instanceof SeparatedList) {
			return RuntimeRuleItemKind.SEPARATED_LIST;			
		} else {
			throw new RuntimeException("Internal Error, type of RuleItem not recognised");
		}
	}
	
}
