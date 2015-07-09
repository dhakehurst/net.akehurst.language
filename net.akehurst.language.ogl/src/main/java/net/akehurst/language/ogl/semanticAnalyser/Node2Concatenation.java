package net.akehurst.language.ogl.semanticAnalyser;

import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Node2Concatenation extends AbstractRhsNode2RuleItem<Concatenation> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "concatenation".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(Concatenation right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Concatenation constructLeft2Right(INode left, Transformer transformer) {
		try {
			List<? extends INode> allLeft = ((IBranch) left).getNonSkipChildren();
			List<? extends TangibleItem> allRight;

			allRight = transformer.transformAllLeft2Right(
					(Class<Relation<INode, TangibleItem>>) (Class<?>) ItemNode2TangibleItem.class, allLeft);

			Concatenation right = new Concatenation(allRight.toArray(new TangibleItem[0]));
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to construct Concatenation", e);
		}
	}

	@Override
	public INode constructRight2Left(Concatenation right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Concatenation right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, Concatenation right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
