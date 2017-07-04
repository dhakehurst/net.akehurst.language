package net.akehurst.language.grammar.parser.converter;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Concatenation2RuntimeRule implements IBinaryRule<Concatenation, RuntimeRule> {

	@Override
	public boolean isValidForLeft2Right(final Concatenation left) {
		return true;
	}

	@Override
	public boolean isValidForRight2Left(final RuntimeRule right) {
		return false;
	}

	@Override
	public boolean isAMatch(final Concatenation left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(final Concatenation left, final ITransformer transformer) {
		final Converter converter = (Converter) transformer;
		final RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}

	@Override
	public Concatenation constructRight2Left(final RuntimeRule right, final ITransformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final Concatenation left, final RuntimeRule right, final ITransformer transformer)
			throws RuleNotFoundException, TransformException {

		final RuntimeRuleItem ruleItem = transformer.transformLeft2Right(Concatenation2RuntimeRuleItem.class, left);
		right.setRhs(ruleItem);

	}

	@Override
	public void updateRight2Left(final Concatenation left, final RuntimeRule right, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

}
