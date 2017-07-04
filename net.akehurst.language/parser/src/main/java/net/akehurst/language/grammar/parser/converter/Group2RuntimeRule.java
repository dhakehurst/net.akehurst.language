package net.akehurst.language.grammar.parser.converter;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.transform.binary.IBinaryRule;
import net.akehurst.transform.binary.ITransformer;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Group2RuntimeRule extends AbstractSimpleItem2RuntimeRule<Group> {

	@Override
	public boolean isValidForLeft2Right(final Group left) {
		return true;
	}

	@Override
	public boolean isValidForRight2Left(final RuntimeRule right) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAMatch(final Group left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RuntimeRule constructLeft2Right(final Group left, final ITransformer transformer) {
		final Converter converter = (Converter) transformer;
		final RuntimeRule right = converter.createVirtualRule(left);
		return right;
	}

	@Override
	public Group constructRight2Left(final RuntimeRule right, final ITransformer transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLeft2Right(final Group left, final RuntimeRule right, final ITransformer transformer) throws RuleNotFoundException, TransformException {

		final AbstractChoice choice = left.getChoice();

		final RuntimeRuleItem ruleItem = transformer
				.transformLeft2Right((Class<? extends IBinaryRule<AbstractChoice, RuntimeRuleItem>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class, choice);
		right.setRhs(ruleItem);

	}

	@Override
	public void updateRight2Left(final Group left, final RuntimeRule right, final ITransformer transformer) {
		// TODO Auto-generated method stub

	}

}
