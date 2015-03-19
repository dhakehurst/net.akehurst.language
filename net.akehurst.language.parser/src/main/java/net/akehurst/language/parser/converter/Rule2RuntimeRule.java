package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.SkipRule;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Rule2RuntimeRule implements Relation<Rule, RuntimeRule>{

	@Override
	public boolean isValidForLeft2Right(Rule left) {
		return true;
	}
	
	@Override
	public RuntimeRule constructLeft2Right(Rule left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = converter.getFactory().createRuntimeRule(left, RuntimeRuleKind.NON_TERMINAL);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Rule left, RuntimeRule right, Transformer transformer) {

		try {
			RuntimeRuleItem rrItem = transformer.transformLeft2Right((Class<? extends Relation<RuleItem, RuntimeRuleItem>>)AbstractRuleItem2RuntimeRuleItem.class, left.getRhs());
			right.setRhs(rrItem);
			right.setIsSkipRule(left instanceof SkipRule);
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Rule arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Rule constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
