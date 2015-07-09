package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class ItemNode2TangibleItem extends AbstractSemanticAnalysisRelation<TangibleItem> {

	@Override
	public String getNodeName() {
		return "item";
	}
	
	@Override
	public TangibleItem constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode node = ((IBranch)left).getChild(0);
			TangibleItem right = transformer.transformLeft2Right(
					(Class<Relation<INode, TangibleItem>>) (Class<?>) AbstractNode2TangibleItem.class, node);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to construct TangibleItem", e);
		}
	}

	@Override
	public INode constructRight2Left(TangibleItem right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, TangibleItem right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void configureRight2Left(INode left, TangibleItem right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}


}
