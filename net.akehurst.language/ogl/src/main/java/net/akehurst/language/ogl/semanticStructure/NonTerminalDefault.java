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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.NodeType;
import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.Terminal;

public class NonTerminalDefault extends TangibleItemAbstract implements NonTerminal {

    private final String referencedRuleName;
    private Rule referencedRule;

    public NonTerminalDefault(final String referencedRuleName) {// , final Rule owner, final List<Integer> index) {
        this.referencedRuleName = referencedRuleName;
    }

    @Override
    public Rule getReferencedRule() throws GrammarRuleNotFoundException {
        if (null == this.referencedRule) {
            this.referencedRule = this.getOwningRule().getGrammar().findAllRule(this.referencedRuleName);
        }
        return this.referencedRule;
    }

    @Override
    public String getName() {
        try {
            return this.getNodeType().getIdentity().asPrimitive();
        } catch (final GrammarRuleNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public NodeType getNodeType() throws GrammarRuleNotFoundException {
        return new RuleNodeTypeDefault(this.getReferencedRule());
    }

    @Override
    public <T, E extends Throwable> T accept(final GrammarVisitor<T, E> visitor, final Object... arg) throws E {
        return visitor.visit(this, arg);
    }

    @Override
    public Set<Terminal> findAllTerminal() {
        final Set<Terminal> result = new HashSet<>();
        return result;
    }

    @Override
    public Set<NonTerminal> findAllNonTerminal() {
        final Set<NonTerminal> result = new HashSet<>();
        result.add(this);
        return result;
    }

    // --- Object ---
    @Override
    public String toString() {
        return this.referencedRuleName;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg instanceof NonTerminalDefault) {
            final NonTerminalDefault other = (NonTerminalDefault) arg;
            return Objects.equals(this.referencedRuleName, other.referencedRuleName) && Objects.equals(this.getIndex(), other.getIndex())
                    && Objects.equals(this.getOwningRule(), other.getOwningRule());
        } else {
            return false;
        }
    }
}
