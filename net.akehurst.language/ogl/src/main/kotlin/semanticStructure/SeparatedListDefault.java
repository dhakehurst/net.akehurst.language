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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.SeparatedList;
import net.akehurst.language.api.grammar.Terminal;

public class SeparatedListDefault extends ConcatenationItemAbstract implements SeparatedList {

    private List<Integer> index;
    private final int min;
    private final int max;
    private final TerminalLiteralDefault separator;
    private final TangibleItemAbstract item;

    public SeparatedListDefault(final int min, final int max, final TerminalLiteralDefault separator, final TangibleItemAbstract item) {
        this.min = min;
        this.max = max;
        this.separator = separator;
        this.item = item;
    }

    @Override
    public List<Integer> getIndex() {
        return this.index;
    }

    @Override
    public RuleItem getSubItem(final int i) {
        if (0 == i) {
            return this.getItem();
        }
        if (1 == i) {
            return this.getSeparator();
        } else {
            return null;
        }
    }

    @Override
    public void setOwningRule(final RuleDefault value, final List<Integer> index) {
        this.owningRule = value;
        this.index = index;
        final ArrayList<Integer> nextIndex0 = new ArrayList<>(index);
        nextIndex0.add(0);
        final ArrayList<Integer> nextIndex1 = new ArrayList<>(index);
        nextIndex1.add(1);
        this.getItem().setOwningRule(value, nextIndex0);
        this.getSeparator().setOwningRule(value, nextIndex1);
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public TerminalLiteralDefault getSeparator() {
        return this.separator;
    }

    public TangibleItemAbstract getItem() {
        return this.item;
    }

    @Override
    public <T, E extends Throwable> T accept(final GrammarVisitor<T, E> visitor, final Object... arg) throws E {
        return visitor.visit(this, arg);
    }

    @Override
    public Set<Terminal> findAllTerminal() {
        final Set<Terminal> result = new HashSet<>();
        result.add(this.getSeparator());
        result.addAll(this.getItem().findAllTerminal());
        return result;
    }

    @Override
    public Set<NonTerminal> findAllNonTerminal() {
        final Set<NonTerminal> result = new HashSet<>();
        result.addAll(this.getItem().findAllNonTerminal());
        return result;
    }

    // --- Object ---
    @Override
    public String toString() {
        return "( " + this.getItem() + " / " + this.getSeparator() + " )" + (this.min == 0 ? "*" : "+");
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg instanceof SeparatedListDefault) {
            final SeparatedListDefault other = (SeparatedListDefault) arg;
            return this.getOwningRule().equals(other.getOwningRule()) && this.index.equals(other.index);
        } else {
            return false;
        }
    }

}
