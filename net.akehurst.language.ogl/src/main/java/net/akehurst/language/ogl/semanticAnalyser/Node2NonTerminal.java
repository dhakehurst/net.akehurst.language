package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2NonTerminal extends AbstractNode2TangibleItem<NonTerminal> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "nonTerminal".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(NonTerminal right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NonTerminal constructLeft2Right(INode left, Transformer transformer) {
		try {
			String referencedRuleName = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch)left).getChild(0));
			NonTerminal right = new NonTerminal(referencedRuleName);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to copnstruct TangibleItem", e);
		}
	}

	@Override
	public INode constructRight2Left(NonTerminal right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, NonTerminal right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, NonTerminal right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
