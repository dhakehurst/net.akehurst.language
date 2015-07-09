package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class ChoceNode2Choice extends RhsNode2RuleItem<Choice>{

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "choice".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(Choice right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Choice constructLeft2Right(INode left, Transformer transformer) {
		transformer.transformAllLeft2Right(AbstractNode2TangibleItem.class, ((IBranch)left).getNonSkipChildren());
		Choice right = new Choice(alternative);
		return null;
	}

	@Override
	public INode constructRight2Left(Choice right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Choice right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(INode left, Choice right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

}
