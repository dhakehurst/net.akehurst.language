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
package net.akehurst.language.ogl.semanticStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.api.grammar.Choice;
import net.akehurst.language.api.grammar.RuleItem;

abstract public class ChoiceAbstract extends RuleItemAbstract implements Choice {

    private final List<ConcatenationDefault> alternative;
    private List<Integer> index;

    public ChoiceAbstract(final ConcatenationDefault... alternative) {
        this.alternative = Arrays.asList(alternative);
    }

    public List<ConcatenationDefault> getAlternative() {
        return this.alternative;
    }

    @Override
    public List<Integer> getIndex() {
        return this.index;
    }

    @Override
    public RuleItem getSubItem(final int i) {
        if (i < this.getAlternative().size()) {
            return this.getAlternative().get(i);
        } else {
            return null;
        }
    }

    @Override
    public void setOwningRule(final RuleDefault value, final List<Integer> index) {
        this.owningRule = value;
        this.index = index;
        int i = 0;
        for (final ConcatenationDefault c : this.getAlternative()) {
            final ArrayList<Integer> nextIndex = new ArrayList<>(index);
            nextIndex.add(i++);
            c.setOwningRule(value, nextIndex);
        }
    }
}
