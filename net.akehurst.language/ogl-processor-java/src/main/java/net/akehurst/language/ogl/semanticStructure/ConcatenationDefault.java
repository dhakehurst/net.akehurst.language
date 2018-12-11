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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.api.analyser.UnableToAnalyseExeception;
import net.akehurst.language.api.grammar.Concatenation;
import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.Terminal;

public class ConcatenationDefault extends RuleItemAbstract implements Concatenation {

    private List<Integer> index;
    private final List<ConcatenationItemAbstract> item;

    public ConcatenationDefault(final ConcatenationItemAbstract... item) {
        if (item.length < 1) {
            throw new UnableToAnalyseExeception("A concatentation must have 1 or more items in it.", null);
        }
        this.item = Arrays.asList(item);
    }

    @Override
    public List<Integer> getIndex() {
        return this.index;
    }

    @Override
    public RuleItem getSubItem(final int i) {
        if (i < this.getItem().size()) {
            return this.getItem().get(i);
        } else {
            return null;
        }
    }

    @Override
    public void setOwningRule(final RuleDefault value, final List<Integer> index) {
        this.owningRule = value;
        this.index = index;
        int i = 0;
        for (final ConcatenationItemAbstract c : this.getItem()) {
            final ArrayList<Integer> nextIndex = new ArrayList<>(index);
            nextIndex.add(i++);
            c.setOwningRule(value, nextIndex);
        }
    }

    public List<ConcatenationItemAbstract> getItem() {
        return this.item;
    }

    @Override
    public <T, E extends Throwable> T accept(final GrammarVisitor<T, E> visitor, final Object... arg) throws E {
        return visitor.visit(this, arg);
    }

    @Override
    public Set<Terminal> findAllTerminal() {
        final Set<Terminal> result = new HashSet<>();
        for (final ConcatenationItemAbstract ti : this.getItem()) {
            result.addAll(ti.findAllTerminal());
        }
        return result;
    }

    @Override
    public Set<NonTerminal> findAllNonTerminal() {
        final Set<NonTerminal> result = new HashSet<>();
        for (final ConcatenationItemAbstract ti : this.getItem()) {
            result.addAll(ti.findAllNonTerminal());
        }
        return result;
    }

    // --- Object ---
    @Override
    public String toString() {
        String r = "";
        for (final RuleItemAbstract i : this.getItem()) {
            r += i.toString() + " ";
        }
        return r;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg instanceof ConcatenationDefault) {
            final ConcatenationDefault other = (ConcatenationDefault) arg;
            return Objects.equals(this.getOwningRule(), other.getOwningRule()) && Objects.equals(this.index, other.index);
        } else {
            return false;
        }
    }
}
