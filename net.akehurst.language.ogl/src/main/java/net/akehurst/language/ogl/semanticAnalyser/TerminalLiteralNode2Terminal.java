package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.Transformer;

public class TerminalLiteralNode2Terminal implements Relation<IBranch, TerminalLiteral> {

	@Override
	public void configureLeft2Right(IBranch left, TerminalLiteral arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(IBranch left, TerminalLiteral arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TerminalLiteral constructLeft2Right(IBranch left, Transformer transformer) {
		INode child = left.getChildren().get(0);
		ILeaf leaf = (ILeaf)child;
		TerminalLiteral right = new TerminalLiteral(leaf.getMatchedText());
		return right;
	}

	@Override
	public IBranch constructRight2Left(TerminalLiteral arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForLeft2Right(IBranch left) {
		return left.getName().equals("LITERAL");
	}

	@Override
	public boolean isValidForRight2Left(TerminalLiteral right) {
		return true;
	}

}
