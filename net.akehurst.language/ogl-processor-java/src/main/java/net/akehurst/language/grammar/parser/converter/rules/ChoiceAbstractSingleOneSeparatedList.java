/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.grammar.parser.converter.rules;

import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItem;
import net.akehurst.language.ogl.semanticStructure.ChoiceAbstract;
import net.akehurst.language.ogl.semanticStructure.SeparatedListDefault;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class ChoiceAbstractSingleOneSeparatedList extends AbstractChoice2RuntimeRuleItem<ChoiceAbstract> {

    @Override
    public boolean isValidForLeft2Right(final ChoiceAbstract left) {
        return 1 == left.getAlternative().size() && 1 == left.getAlternative().get(0).getItem().size()
                && left.getAlternative().get(0).getItem().get(0) instanceof SeparatedListDefault;
    }

    @Override
    public boolean isAMatch(final ChoiceAbstract left, final RuntimeRuleItem right, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public RuntimeRuleItem constructLeft2Right(final ChoiceAbstract left, final BinaryTransformer transformer) {
        final SeparatedListDefault item = (SeparatedListDefault) left.getAlternative().get(0).getItem().get(0);

        final RuntimeRuleItem right = transformer.transformLeft2Right(SeparatedList2RuntimeRuleItem.class, item);
        return right;

    }

    @Override
    public void updateLeft2Right(final ChoiceAbstract left, final RuntimeRuleItem right, final BinaryTransformer transformer) {
        // in other cases, a multi is converted to a group and the empty rule is added then,
        // but not in this special case

        final SeparatedListDefault item = (SeparatedListDefault) left.getAlternative().get(0).getItem().get(0);
        if (0 == item.getMin()) {
            final Converter converter = (Converter) transformer;
            final RuntimeRule ruleThatIsEmpty = transformer.transformLeft2Right(Rule2RuntimeRule.class, left.getOwningRule());
            final RuntimeRule rhs = converter.createEmptyRuleFor(ruleThatIsEmpty);
        }

    }

    @Override
    public void updateRight2Left(final ChoiceAbstract arg0, final RuntimeRuleItem arg1, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public ChoiceAbstract constructRight2Left(final RuntimeRuleItem arg0, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRuleItem arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}