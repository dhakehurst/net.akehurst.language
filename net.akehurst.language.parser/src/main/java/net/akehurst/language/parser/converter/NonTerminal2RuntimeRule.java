package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.Transformer;

public class NonTerminal2RuntimeRule extends AbstractTangibleItem2RuntimeRule<NonTerminal> {

	@Override
	public boolean isValidForLeft2Right(NonTerminal arg0) {
		return true;
	}
	

	@Override
	public RuntimeRule constructLeft2Right(NonTerminal left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = null;
		try {
			right = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getReferencedRule());
			//right = converter.getFactory().createRuntimeRule(left.getReferencedRule(), RuntimeRuleKind.NON_TERMINAL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return right;
	}
	
	@Override
	public void configureLeft2Right(NonTerminal arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(NonTerminal arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public NonTerminal constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
