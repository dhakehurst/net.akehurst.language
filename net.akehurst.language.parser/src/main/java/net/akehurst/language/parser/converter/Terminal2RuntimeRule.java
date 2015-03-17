package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.Transformer;

public class Terminal2RuntimeRule extends AbstractTangibleItem2RuntimeRule<Terminal> {

	@Override
	public boolean isValidForLeft2Right(Terminal arg0) {
		return true;
	}

	@Override
	public RuntimeRule constructLeft2Right(Terminal left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right =  converter.getFactory().createRuntimeRule(left, RuntimeRuleKind.TERMINAL);
		return right;
	}
	
	@Override
	public void configureLeft2Right(Terminal arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(Terminal arg0, RuntimeRule arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Terminal constructRight2Left(RuntimeRule arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isValidForRight2Left(RuntimeRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
