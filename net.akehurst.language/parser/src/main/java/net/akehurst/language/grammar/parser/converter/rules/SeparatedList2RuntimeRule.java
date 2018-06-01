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
import net.akehurst.language.ogl.semanticStructure.SeparatedListDefault;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class SeparatedList2RuntimeRule extends AbstractConcatinationItem2RuntimeRule<SeparatedListDefault> {

    @Override
    public boolean isValidForLeft2Right(final SeparatedListDefault arg0) {
        return true;
    }

    @Override
    public boolean isAMatch(final SeparatedListDefault left, final RuntimeRule right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RuntimeRule constructLeft2Right(final SeparatedListDefault left, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final RuntimeRule right = converter.createVirtualRule(left);
        return right;
    }

    @Override
    public void updateLeft2Right(final SeparatedListDefault left, final RuntimeRule right, final BinaryTransformer transformer) {

        final RuntimeRuleItem ruleItem = transformer.transformLeft2Right(SeparatedList2RuntimeRuleItem.class, left);
        right.setRhs(ruleItem);

    }

    @Override
    public void updateRight2Left(final SeparatedListDefault arg0, final RuntimeRule arg1, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public SeparatedListDefault constructRight2Left(final RuntimeRule arg0, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRule arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}
