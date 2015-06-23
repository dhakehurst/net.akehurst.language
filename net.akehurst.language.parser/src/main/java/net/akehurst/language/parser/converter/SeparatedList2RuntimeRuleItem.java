package net.akehurst.language.parser.converter;

import java.util.List;

import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.transform.binary.Relation;
import net.akehurst.transform.binary.RelationNotFoundException;
import net.akehurst.transform.binary.Transformer;

public class SeparatedList2RuntimeRuleItem extends AbstractRuleItem2RuntimeRuleItem<SeparatedList> {

	@Override
	public boolean isValidForLeft2Right(SeparatedList arg0) {
		return true;
	}
	
	@Override
	public RuntimeRuleItem constructLeft2Right(SeparatedList left, Transformer transformer) {
		Converter converter = (Converter)transformer;
		int maxRuleRumber = converter.getFactory().getRuntimeRuleSet().getTotalRuleNumber();
		RuntimeRuleItem right = new RuntimeRuleItem(RuntimeRuleItemKind.SEPARATED_LIST,maxRuleRumber);
		return right;
	}
	
	@Override
	public void configureLeft2Right(SeparatedList left, RuntimeRuleItem right, Transformer transformer) {
		TangibleItem ti = left.getConcatination();
		Terminal sep = left.getSeparator();
		
		try {
			RuntimeRule rr = transformer.transformLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>)AbstractTangibleItem2RuntimeRule.class, ti);
			RuntimeRule rrsep = transformer.transformLeft2Right((Class<? extends Relation<TangibleItem, RuntimeRule>>)AbstractTangibleItem2RuntimeRule.class, sep);
			RuntimeRule[] items = new RuntimeRule[]{ rr, rrsep };
			
			right.setItems(items);
		
		} catch (RelationNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void configureRight2Left(SeparatedList arg0, RuntimeRuleItem arg1, Transformer arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SeparatedList constructRight2Left(RuntimeRuleItem arg0, Transformer arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValidForRight2Left(RuntimeRuleItem arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
