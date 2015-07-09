package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.transform.binary.Relation;

abstract
public class AbstractSemanticAnalysisRelation<R> implements Relation<INode, R>{

	abstract public String getNodeName();
	
	@Override
	public boolean isValidForLeft2Right(INode left) {
		return this.getNodeName().equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(R right) {
		return false;
	}
}
