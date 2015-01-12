package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class NamespaceBranch2Namespace implements Relation<IBranch, Namespace> {

	@Override
	public void configureLeft2Right(IBranch arg0, Namespace arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(IBranch arg0, Namespace arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Namespace constructLeft2Right(IBranch arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBranch constructRight2Left(Namespace arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(IBranch arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isValidForRight2Left(Namespace arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
