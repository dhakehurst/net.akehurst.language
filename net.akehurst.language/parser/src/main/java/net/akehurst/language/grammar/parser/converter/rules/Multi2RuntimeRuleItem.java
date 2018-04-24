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
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.SimpleItem;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Multi2RuntimeRuleItem implements BinaryRule<Multi, RuntimeRuleItem> {

    @Override
    public boolean isValidForLeft2Right(final Multi left) {
        return true;
    }

    @Override
    public boolean isAMatch(final Multi left, final RuntimeRuleItem right, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RuntimeRuleItem constructLeft2Right(final Multi left, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final RuntimeRuleItem right = converter.getFactory().createRuntimeRuleItem(RuntimeRuleItemKind.MULTI);
        return right;
    }

    @Override
    public void updateLeft2Right(final Multi left, final RuntimeRuleItem right, final BinaryTransformer transformer) {
        final SimpleItem ti = left.getItem();

        final RuntimeRule rr = transformer
                .transformLeft2Right((Class<? extends BinaryRule<SimpleItem, RuntimeRule>>) (Class<?>) AbstractConcatinationItem2RuntimeRule.class, ti);
        final RuntimeRule[] items = new RuntimeRule[] { rr };

        right.setItems(items);
        right.setMultiMin(left.getMin());
        right.setMultiMax(left.getMax());

    }

    @Override
    public void updateRight2Left(final Multi arg0, final RuntimeRuleItem arg1, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public Multi constructRight2Left(final RuntimeRuleItem arg0, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRuleItem arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}
