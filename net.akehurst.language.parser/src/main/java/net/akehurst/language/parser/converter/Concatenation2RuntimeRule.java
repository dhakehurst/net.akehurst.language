package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.parser.runtime.RuntimeRuleKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Concatenation2RuntimeRule implements Relation<Concatenation, RuntimeRule> {

	@Override
	public boolean isValidForLeft2Right(Concatenation left) {
		return true;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRule right) {
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(Concatenation left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}

	@Override
	public Concatenation constructRight2Left(RuntimeRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureLeft2Right(Concatenation left, RuntimeRule right, Transformer transformer) {

		try {
			
			RuntimeRuleItem ruleItem = transformer.transformLeft2Right(Concatenation2RuntimeRuleItem.class, left);
			right.setRhs(ruleItem);

		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}


	}

	@Override
	public void configureRight2Left(Concatenation left, RuntimeRule right, Transformer transformer) {
		// TODO Auto-generated method stub
		
	}

}
