package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.SkipRule;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class SkipRuleNode2SkipRule extends NormalRuleNode2Rule {

	@Override
	public boolean isValidForLeft2Right(INode left) {
		return "skipRule".equals(left.getName());
	}
	
	@Override
	public SkipRule constructLeft2Right(INode left, Transformer transformer) {
		try {
			INode grammarNode = left.getParent().getParent().getParent().getParent();
			Grammar grammar = transformer.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, grammarNode);
			String name = transformer.transformLeft2Right(IDENTIFIERBranch2String.class, ((IBranch)left).getChild(1));
			SkipRule right = new SkipRule(grammar, name);
			return right;
		} catch (RelationNotFoundException e) {
			throw new RuntimeException("Unable to configure Grammar", e);
		}
	}
	
}
