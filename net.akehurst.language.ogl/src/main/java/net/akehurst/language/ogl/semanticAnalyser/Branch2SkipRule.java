package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.ogl.semanticModel.SkipRule;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class Branch2SkipRule implements Relation<IBranch, SkipRule> {

	@Override
	public boolean isValidForLeft2Right(IBranch left) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isValidForRight2Left(SkipRule right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SkipRule constructLeft2Right(IBranch left, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBranch constructRight2Left(SkipRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(IBranch left, SkipRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(IBranch left, SkipRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

}
