package net.akehurst.language.parser.converter;

import java.util.List;

import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class Concatenation2RuntimeRuleItem extends AbstractRuleItem2RuntimeRuleItem<Concatenation> {

	@Override
	public boolean isValidForLeft2Right(Concatenation arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(Concatenation arg0, Transformer arg1) {
		return new RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION);
	}
	
	@Override
	public void configureLeft2Right(Concatenation left, RuntimeRuleItem right, Transformer transformer) {
		List<TangibleItem> tis = left.getItem();
		
		try {
			List<? extends RuntimeRule> rr = transformer.transformAllLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>)AbstractTangibleItem2RuntimeRule.class, tis);
			RuntimeRule[] items = rr.toArray(new RuntimeRule[rr.size()]);
			
			right.setItems(items);
		
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void configureRight2Left(Concatenation arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Concatenation constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
