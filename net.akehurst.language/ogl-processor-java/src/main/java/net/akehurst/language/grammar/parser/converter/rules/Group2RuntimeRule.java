package net.akehurst.language.grammar.parser.converter.rules;

import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.ChoiceAbstract;
import net.akehurst.language.ogl.semanticStructure.GroupDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Group2RuntimeRule extends AbstractSimpleItem2RuntimeRule<GroupDefault> {

    @Override
    public boolean isValidForLeft2Right(final GroupDefault left) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRule right) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAMatch(final GroupDefault left, final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RuntimeRule constructLeft2Right(final GroupDefault left, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final RuntimeRule right = converter.createVirtualRule(left);
        return right;
    }

    @Override
    public GroupDefault constructRight2Left(final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateLeft2Right(final GroupDefault left, final RuntimeRule right, final BinaryTransformer transformer) {

        final ChoiceAbstract choice = left.getChoice();

        final RuntimeRuleItem ruleItem = transformer
                .transformLeft2Right((Class<? extends BinaryRule<ChoiceAbstract, RuntimeRuleItem>>) (Class<?>) AbstractChoice2RuntimeRuleItem.class, choice);
        right.setRhs(ruleItem);

    }

    @Override
    public void updateRight2Left(final GroupDefault left, final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

}
