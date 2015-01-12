package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class IDENTIFIERBranch2String implements Relation<IBranch, String>{

	@Override
	public void configureLeft2Right(IBranch arg0, String arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(IBranch arg0, String arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String constructLeft2Right(IBranch left, Transformer transformer) {
		ILeaf leaf = (ILeaf)left.getChildren().get(0);
		String right = leaf.getMatchedText();
		return right;
	}

	@Override
	public IBranch constructRight2Left(String arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(IBranch left) {
		return left.getName().equals("IDENTIFIER");
	}

	@Override
	public boolean isValidForRight2Left(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
