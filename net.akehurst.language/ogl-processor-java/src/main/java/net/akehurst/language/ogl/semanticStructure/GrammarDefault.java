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

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.Namespace;
import net.akehurst.language.api.grammar.NodeType;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.api.lexicalAnalyser.ITokenType;

public class GrammarDefault implements Grammar {

    private final NamespaceDefault namespace;
    private final String name;
    private List<GrammarDefault> extends_;
    private List<Rule> rule;
    private Set<Terminal> allTerminal_cache;

    public GrammarDefault(final NamespaceDefault namespace, final String name) {
        this.namespace = namespace;
        this.name = name;
        this.extends_ = new ArrayList<>();
        this.rule = new ArrayList<>();
    }

    @Override
    public Namespace getNamespace() {
        return this.namespace;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public List<GrammarDefault> getExtends() {
        return this.extends_;
    }

    public void setExtends(final List<GrammarDefault> value) {
        this.extends_ = value;
    }

    @Override
    public List<Rule> getRule() {
        return this.rule;
    }

    public void setRule(final List<Rule> value) {
        this.rule = value;
    }

    public List<Rule> getAllRule() {
        final ArrayList<Rule> allRules = new ArrayList<>();
        allRules.addAll(this.getRule());
        for (final GrammarDefault pg : this.getExtends()) {
            allRules.addAll(pg.getAllRule());
        }
        return allRules;
    }

    @Override
    public Rule findAllRule(final String ruleName) throws GrammarRuleNotFoundException {
        try {
            return this.findRule(ruleName);
        } catch (final GrammarRuleNotFoundException e) {
        }
        for (final GrammarDefault pg : this.getExtends()) {
            try {
                return pg.findRule(ruleName);
            } catch (final GrammarRuleNotFoundException e) {
            }
        }
        throw new GrammarRuleNotFoundException(ruleName + " in Grammar(" + this.getName() + ").findAllRule");
    }

    public Rule findRule(final String ruleName) throws GrammarRuleNotFoundException {
        final ArrayList<Rule> rules = new ArrayList<>();
        for (final Rule r : this.getRule()) {
            if (r.getName().equals(ruleName)) {
                rules.add(r);
            }
        }
        if (rules.isEmpty()) {
            throw new GrammarRuleNotFoundException(ruleName + " in Grammar(" + this.getName() + ").findRule");
        } else if (rules.size() == 1) {
            return rules.get(0);
        } else {
            throw new GrammarRuleNotFoundException(ruleName + "too many rules in Grammar(" + this.getName() + ").findRule with name " + ruleName);
        }
    }

    @Override
    public Set<Terminal> getAllTerminal() {
        if (null == this.allTerminal_cache) {
            this.allTerminal_cache = this.findAllTerminal();
        }
        return this.allTerminal_cache;
    }

    @Override
    public Terminal findAllTerminal(final String terminalPattern) {
        for (final Terminal t : this.findAllTerminal()) {
            if (t.getValue().equals(terminalPattern)) {
                return t;
            }
        }
        return null;
    }

    Set<Terminal> findAllTerminal() {
        final Set<Terminal> result = new HashSet<>();
        for (final Rule rule : this.getAllRule()) {
            final RuleItem ri = rule.getRhs();
            result.addAll(this.findAllTerminal(0, rule, ri));
        }
        return result;
    }

    Set<Terminal> findAllTerminal(final int totalItems, final Rule rule, final RuleItem item) {
        final Set<Terminal> result = new HashSet<>();
        if (item instanceof TerminalAbstract) {
            final TerminalAbstract t = (TerminalAbstract) item;
            result.add(t);
        } else if (item instanceof MultiDefault) {
            result.addAll(this.findAllTerminal(totalItems, rule, ((MultiDefault) item).getItem()));
        } else if (item instanceof ChoiceAbstract) {
            for (final ConcatenationDefault ti : ((ChoiceAbstract) item).getAlternative()) {
                result.addAll(this.findAllTerminal(totalItems, rule, ti));
            }
        } else if (item instanceof ConcatenationDefault) {
            for (final ConcatenationItemAbstract ti : ((ConcatenationDefault) item).getItem()) {
                result.addAll(this.findAllTerminal(totalItems, rule, ti));
            }
        } else if (item instanceof SeparatedListDefault) {
            result.addAll(this.findAllTerminal(totalItems, rule, ((SeparatedListDefault) item).getSeparator()));
            result.addAll(this.findAllTerminal(totalItems, rule, ((SeparatedListDefault) item).getItem()));
        } else if (item instanceof GroupDefault) {
            result.addAll(this.findAllTerminal(totalItems, rule, ((GroupDefault) item).getChoice()));
        } else if (item instanceof NonTerminalDefault) {
            // add nothing
        } else {
            throw new RuntimeException("Internal Error: Should never happen");
        }
        return result;
    }

    public List<ITokenType> findTokenTypes() {
        final List<ITokenType> result = new ArrayList<>();
        for (final Terminal t : this.getAllTerminal()) {
            final String pattern = t.getValue();
            final String identity = ((TerminalAbstract) t).getOwningRule().getName();
            final TokenType tt = new TokenType(identity, pattern, t instanceof TerminalPatternDefault);
            if (!result.contains(tt)) {
                result.add(tt);
            }
        }
        return result;
    }

    @Override
    public Set<NodeType> findAllNodeType() {
        final Set<NodeType> res = new HashSet<>();
        for (final Terminal t : this.getAllTerminal()) {
            try {
                res.add(((TerminalAbstract) t).getNodeType());
            } catch (final GrammarRuleNotFoundException e) {
                e.printStackTrace();
            }
        }

        for (final Rule r : this.getAllRule()) {
            res.add(r.getNodeType());
        }

        return res;
    }

    public NodeType findNodeType(final String ruleName) throws GrammarRuleNotFoundException {
        for (final Rule r : this.getAllRule()) {
            if (r.getName().equals(ruleName)) {
                return new RuleNodeTypeDefault(r);
            }
        }
        throw new GrammarRuleNotFoundException(ruleName);
    }

    // --- Object ---
    @Override
    public String toString() {
        String r = this.getNamespace() + System.lineSeparator();
        String extendStr = "";
        if (!this.getExtends().isEmpty()) {
            extendStr += " extends ";
            for (final GrammarDefault pg : this.getExtends()) {
                extendStr += pg.getName() + ", ";
            }
        }
        r += "grammar " + this.getName() + extendStr + " {" + System.lineSeparator();
        for (final Rule i : this.getRule()) {
            r += i.toString() + System.lineSeparator();
        }
        r += "}";
        return r;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg instanceof GrammarDefault) {
            final GrammarDefault other = (GrammarDefault) arg;
            return this.toString().equals(other.toString());
        } else {
            return false;
        }
    }

}
