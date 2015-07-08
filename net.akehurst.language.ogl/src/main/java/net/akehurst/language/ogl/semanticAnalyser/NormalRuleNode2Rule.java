package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class NormalRuleNode2Rule implements Relation<INode, Rule> {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "normalRule".equals(left.getName());
	}

	@Override
	public boolean isValidForRight2Left(Rule right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Rule constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode grammarNode = left.getParent().getParent().getParent().getParent();
			Grammar grammar = transformer.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, grammarNode);
			String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch)left).getChild(0));
			Rule right = new Rule(grammar, name);
			return right;
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
		RuleItem ruleItem = new Choice();
		right.setRhs(ruleItem);
	}

	@Override
	public void configureRight2Left(INode left, Rule right, Transformer transformer) {
		// TODO Auto-generated method stub

	}

}
