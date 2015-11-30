package net.akehurst.language.grammar.parser.converter;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Group2RuntimeRule extends AbstractSimpleItem2RuntimeRule<Group> {

	@Override
	public boolean isValidForLeft2Right(Group left) {
		return true;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRule right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(Group left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}

	@Override
	public Group constructRight2Left(RuntimeRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(Group left, RuntimeRule right, Transformer transformer) {
		try {
			AbstractChoice choice = left.getChoice();
			
			RuntimeRuleItem ruleItem = transformer.transformLeft2Right((Class<? extends Relation<AbstractChoice, RuntimeRuleItem>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class, choice);
			right.setRhs(ruleItem);

		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(Group left, RuntimeRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

}
