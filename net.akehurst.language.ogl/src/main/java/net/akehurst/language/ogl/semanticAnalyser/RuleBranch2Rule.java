package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class RuleBranch2Rule implements Relation<INode, Rule> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "anyRule".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(Rule right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Rule constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode rule = ((IBranch) left).getChild(1);
			if ("normalRule".equals(rule.getName())) {
				return transformer.transformLeft2Right(Node2NormalRule.class, rule);
			} else {
				return null;
			}
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}

	@Override
	public INode constructRight2Left(Rule right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(INode left, Rule right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configureRight2Left(INode left, Rule right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
