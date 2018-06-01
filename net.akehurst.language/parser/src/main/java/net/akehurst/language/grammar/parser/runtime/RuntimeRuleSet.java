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
package net.akehurst.language.grammar.parser.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.core.grammar.Grammar;
import net.akehurst.language.core.grammar.IRuleItem;
import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.grammar.parser.NonTerminalRuleReference;
import net.akehurst.language.ogl.semanticStructure.Rule;

public class RuntimeRuleSet {

    private RuntimeRule[] emptyRulesFor;
    private final int totalRuleNumber;
    private RuntimeRule[] runtimeRules;
    private List<RuntimeRule> allSkipRules_cache;
    private Set<RuntimeRule> allSkipTerminals;
    private int[] isSkipTerminal;
    private ArrayList<String> nodeTypes;
    private Map<String, Integer> ruleNumbers;
    private RuntimeRule[][] possibleSubRule;
    private RuntimeRule[][] possibleFirstRule;
    private RuntimeRule[][] possibleSuperRule;
    private SuperRuleInfo[][] possibleSuperRuleInfo;
    private RuntimeRule[][] possibleSubTerminal;
    private RuntimeRule[][] possibleFirstTerminals;
    private RuntimeRule[] possibleFirstSkipTerminals;
    private Map<String, RuntimeRule> terminalMap;
    private String toString_cache;

    /*
     * necessary to know total number of rules up front, so it can be used in RuntimeRuleItem
     */
    public RuntimeRuleSet(final int totalRuleNumber) { // , int emptyRuleNumber) {
        this.totalRuleNumber = totalRuleNumber;
        // this.emptyRuleNumber = emptyRuleNumber;
    }

    // int emptyRuleNumber;

    public RuntimeRule getEmptyRule(final RuntimeRule ruleThatIsEmpty) {
        return this.emptyRulesFor[ruleThatIsEmpty.getRuleNumber()];
    }

    public int getTotalRuleNumber() {
        return this.totalRuleNumber;
    }

    public RuntimeRule[] getAllRules() {
        return this.runtimeRules;
    }

    public void setRuntimeRules(final List<? extends RuntimeRule> value) {
        final int numberOfRules = value.size();
        this.possibleSubTerminal = new RuntimeRule[numberOfRules][];
        this.possibleFirstTerminals = new RuntimeRule[numberOfRules][];
        this.possibleSubRule = new RuntimeRule[numberOfRules][];
        this.possibleFirstRule = new RuntimeRule[numberOfRules][];
        this.possibleSuperRule = new RuntimeRule[numberOfRules][];
        this.possibleSuperRuleInfo = new SuperRuleInfo[numberOfRules][];
        this.runtimeRules = new RuntimeRule[numberOfRules];
        this.nodeTypes = new ArrayList<>(Arrays.asList(new String[numberOfRules]));
        this.ruleNumbers = new HashMap<>();
        this.terminalMap = new HashMap<>();
        this.emptyRulesFor = new RuntimeRule[numberOfRules];
        this.isSkipTerminal = new int[numberOfRules];

        for (final RuntimeRule rrule : value) {
            if (null == rrule) {
                throw new RuntimeException("RuntimeRuleSet must not containan  null rule!");
            }
            final int i = rrule.getRuleNumber();
            this.runtimeRules[i] = rrule;
            if (RuntimeRuleKind.NON_TERMINAL == rrule.getKind()) {
                this.nodeTypes.set(i, rrule.getNodeTypeName());
                this.ruleNumbers.put(rrule.getNodeTypeName(), i);
            } else {
                this.terminalMap.put(rrule.getTerminalPatternText(), rrule);
                if (rrule.getIsEmptyRule()) {
                    this.emptyRulesFor[rrule.getRuleThatIsEmpty().getRuleNumber()] = rrule;
                }
            }
        }
    }

    public RuntimeRule getRuntimeRule(final String ruleName) {
        final int index = this.getRuleNumber(ruleName);
        return this.runtimeRules[index];
    }

    public RuntimeRule getRuntimeRule(final int index) {
        return this.runtimeRules[index];
    }

    public RuntimeRule getRuntimeRule(final Rule rule) {
        final int index = this.getRuleNumber(rule.getName());
        return this.getRuntimeRule(index);
    }

    // public Rule getRule(int ruleNumber) {
    // return this.getRuntimeRule(ruleNumber).getGrammarRule();
    // }

    public List<RuntimeRule> getAllSkipRules() {
        if (null == this.allSkipRules_cache) {
            this.allSkipRules_cache = new ArrayList<>();
            for (final RuntimeRule r : this.getAllRules()) {
                if (r.getIsSkipRule()) {
                    this.allSkipRules_cache.add(r);
                }
            }
        }
        return this.allSkipRules_cache;
    }

    public Set<RuntimeRule> getAllSkipTerminals() {
        if (null == this.allSkipTerminals) {
            final Set<RuntimeRule> result = new HashSet<>();
            for (final RuntimeRule r : this.getAllSkipRules()) {
                for (final RuntimeRule rr : r.getRhs().getItems()) {
                    if (RuntimeRuleKind.TERMINAL == rr.getKind()) {
                        result.add(rr);
                    }
                }
            }
            this.allSkipTerminals = result;
        }
        return this.allSkipTerminals;
    }

    public boolean isSkipTerminal(final RuntimeRule rr) {
        final int res = this.isSkipTerminal[rr.getRuleNumber()];
        if (res == 0) {
            final boolean b = this.getAllSkipTerminals().contains(rr);
            this.isSkipTerminal[rr.getRuleNumber()] = b ? 1 : -1;
            return b;
        }
        return res == 1;
    }

    public String getNodeType(final int nodeTypeNumber) {
        return this.nodeTypes.get(nodeTypeNumber);
    }

    public int getRuleNumber(final String rule) {
        return this.ruleNumbers.get(rule);
    }

    public RuntimeRule[] getPossibleSubRule(final RuntimeRule runtimeRule) {
        RuntimeRule[] result = this.possibleSubRule[runtimeRule.getRuleNumber()];
        if (null == result) {
            final Set<RuntimeRule> rr = runtimeRule.findSubRules();
            result = rr.toArray(new RuntimeRule[rr.size()]);
            this.possibleSubRule[runtimeRule.getRuleNumber()] = result;
        }
        return result;
    }

    public RuntimeRule[] getPossibleFirstSubRule(final RuntimeRule runtimeRule) {
        RuntimeRule[] result = this.possibleFirstRule[runtimeRule.getRuleNumber()];
        if (null == result) {
            final Set<RuntimeRule> rr = runtimeRule.findSubRulesAt(0);
            result = rr.toArray(new RuntimeRule[rr.size()]);
            this.possibleFirstRule[runtimeRule.getRuleNumber()] = result;
        }
        return result;
    }

    public RuntimeRule[] getPossibleSuperRule(final RuntimeRule runtimeRule) {
        RuntimeRule[] result = this.possibleSuperRule[runtimeRule.getRuleNumber()];
        if (null == result) {
            result = this.findAllSuperRule(runtimeRule);
            this.possibleSuperRule[runtimeRule.getRuleNumber()] = result;
        }
        return result;
    }

    public SuperRuleInfo[] getPossibleSuperRuleInfo(final RuntimeRule runtimeRule) {
        SuperRuleInfo[] result = this.possibleSuperRuleInfo[runtimeRule.getRuleNumber()];
        if (null == result) {
            result = this.findAllSuperRuleInfo(runtimeRule);
            this.possibleSuperRuleInfo[runtimeRule.getRuleNumber()] = result;
        }
        return result;
    }

    public RuntimeRule[] getPossibleSubTerminal(final RuntimeRule runtimeRule) {
        RuntimeRule[] result = this.possibleSubTerminal[runtimeRule.getRuleNumber()];
        if (null == result) {
            final Set<RuntimeRule> rr = runtimeRule.findAllTerminal();
            for (final RuntimeRule r : this.getPossibleSubRule(runtimeRule)) {
                rr.addAll(r.findAllTerminal());
            }
            final Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
            rr.addAll(skipTerminal);
            result = rr.toArray(new RuntimeRule[rr.size()]);
            this.possibleSubTerminal[runtimeRule.getRuleNumber()] = result;
        }
        return result;
    }

    public RuntimeRule[] getPossibleFirstTerminals(final RuntimeRule runtimeRule) {
        RuntimeRule[] result = this.possibleFirstTerminals[runtimeRule.getRuleNumber()];
        if (null == result) {
            final Set<RuntimeRule> rr = runtimeRule.findTerminalAt(0);
            for (final RuntimeRule r : this.getPossibleFirstSubRule(runtimeRule)) {
                rr.addAll(r.findTerminalAt(0));
            }
            // Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
            // rr.addAll( skipTerminal );
            result = rr.toArray(new RuntimeRule[rr.size()]);
            this.possibleFirstTerminals[runtimeRule.getRuleNumber()] = result;
        }
        return result;
    }

    public RuntimeRule[] getPossibleFirstSkipTerminals() {
        RuntimeRule[] result = this.possibleFirstSkipTerminals;
        if (null == result) {
            final Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
            result = skipTerminal.toArray(new RuntimeRule[skipTerminal.size()]);
            this.possibleFirstSkipTerminals = result;
        }
        return result;
    }

    public void build() {
        for (final RuntimeRule runtimeRule : this.getAllRules()) {
            this.getPossibleSubTerminal(runtimeRule);
            this.getPossibleFirstTerminals(runtimeRule);
            this.getPossibleSubRule(runtimeRule);
            this.getPossibleFirstSubRule(runtimeRule);
            this.getPossibleSuperRule(runtimeRule);
            this.getPossibleSuperRuleInfo(runtimeRule);
            this.isSkipTerminal(runtimeRule);
        }
    }

    public static final class SuperRuleInfo {
        public SuperRuleInfo(final RuntimeRule runtimeRule, final int index) {
            this.runtimeRule = runtimeRule;
            this.index = index;
            this.hashCode_cache = Objects.hash(runtimeRule, index);
        }

        RuntimeRule runtimeRule;
        int index;

        public RuntimeRule getRuntimeRule() {
            return this.runtimeRule;
        }

        public int getIndex() {
            return this.index;
        }

        int hashCode_cache;

        @Override
        public int hashCode() {
            return this.hashCode_cache;
        }

        @Override
        public boolean equals(final Object arg) {
            if (!(arg instanceof SuperRuleInfo)) {
                return false;
            }
            final SuperRuleInfo other = (SuperRuleInfo) arg;
            return this.index == other.index && this.runtimeRule == other.runtimeRule;
        }

        @Override
        public String toString() {
            return "(".concat(this.runtimeRule.toString()).concat(",").concat(Integer.toString(this.index)).concat(")");
        }
    }

    SuperRuleInfo[] findAllSuperRuleInfo(final RuntimeRule runtimeRule) {
        final Set<SuperRuleInfo> result = new HashSet<>();
        for (final RuntimeRule r : this.runtimeRules) {
            if (RuntimeRuleKind.TERMINAL == r.getKind()) {
                // if (r.equals(runtimeRule)) {
                // int index = 0;
                // result.add(new SuperRuleInfo(r, index));
                // }
            } else {
                // final List<RuntimeRule> rhs = Arrays.asList(r.getRhs().getItems());
                // if (rhs.contains(runtimeRule)) {
                // final int index = rhs.indexOf(runtimeRule);
                // result.add(new SuperRuleInfo(r, index));
                // }
                if (r.couldHaveChild(runtimeRule, 0)) {
                    // if (r.getRhs().getItems()[0].getRuleNumber() == runtimeRule.getRuleNumber()) {
                    result.add(new SuperRuleInfo(r, 0));
                }
            }
        }
        if (runtimeRule.getIsEmptyRule()) {
            result.add(new SuperRuleInfo(runtimeRule.getRuleThatIsEmpty(), 0));
        }
        return result.toArray(new SuperRuleInfo[result.size()]);
    }

    RuntimeRule[] findAllSuperRule(final RuntimeRule runtimeRule) {
        final Set<RuntimeRule> result = new HashSet<>();
        for (final RuntimeRule r : this.runtimeRules) {
            if (RuntimeRuleKind.NON_TERMINAL == runtimeRule.getKind() && r.findAllNonTerminal().contains(runtimeRule)
                    || RuntimeRuleKind.TERMINAL == runtimeRule.getKind() && r.findAllTerminal().contains(runtimeRule)) {
                result.add(r);
            }
        }
        return result.toArray(new RuntimeRule[result.size()]);
    }

    public RuntimeRule getForTerminal(final String terminal) {
        final RuntimeRule rr = this.terminalMap.get(terminal);
        return rr;
    }

    public void putForTerminal(final String terminal, final RuntimeRule runtimeRule) {
        this.terminalMap.put(terminal, runtimeRule);
    }

    public IRuleItem getOriginalItem(final RuntimeRule rr, final Grammar grammar) throws GrammarRuleNotFoundException {
        final String name = rr.getName();
        if (name.startsWith("$")) {
            // decode it (see Converter) and RuleItem.setOwningRule
            final String[] split = name.split("[.]");
            final String ruleName = split[0].substring(1);
            final IRuleItem rhs = grammar.findAllRule(ruleName).getRhs();
            final String type = split[1];
            final int[] index = new int[split.length - 3];
            for (int i = 3; i < split.length; ++i) {
                final int ix = Integer.parseInt(split[i]);
                index[i - 3] = ix;
            }
            IRuleItem item = rhs;
            for (final int i : index) {
                item = item.getSubItem(i);
            }

            return item;
        } else {
            // find grammar rule
            return new NonTerminalRuleReference(grammar, name);
        }
    }

    @Override
    public String toString() {
        if (null == this.toString_cache) {
            this.toString_cache = "";
            for (final RuntimeRule r : this.getAllRules()) {
                this.toString_cache += r + System.lineSeparator();
            }
        }
        return this.toString_cache;
    }

}