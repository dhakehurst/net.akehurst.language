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

import java.util.regex.Pattern;

import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;
import net.akehurst.language.parser.sppf.Branch;
import net.akehurst.language.parser.sppf.Factory;
import net.akehurst.language.parser.sppf.Leaf;

public class RuntimeRuleSetBuilder {

    public RuntimeRuleSetBuilder() {
        this.parseTreeFactory = new Factory();
    }

    private final Factory parseTreeFactory;
    private RuntimeRuleSet runtimeRuleSet;
    private int nextRuleNumber;

    public RuntimeRule getRuntimeRule(final Terminal terminal) {
        return this.runtimeRuleSet.getForTerminal(terminal.getValue());
    }

    public RuntimeRule getRuntimeRule(final Rule rule) {
        return this.runtimeRuleSet.getRuntimeRule(rule);
    }

    public RuntimeRule createRuntimeRule(final Rule grammarRule) {
        final RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, grammarRule.getName(), this.nextRuleNumber, RuntimeRuleKind.NON_TERMINAL, Pattern.LITERAL);
        ++this.nextRuleNumber;
        return rr;
    }

    public RuntimeRule createRuntimeRule(final Terminal terminal) {
        final int patternFlags = terminal instanceof TerminalPatternDefault ? Pattern.MULTILINE : Pattern.LITERAL;
        final RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, terminal.getValue(), this.nextRuleNumber, RuntimeRuleKind.TERMINAL, patternFlags);
        ++this.nextRuleNumber;
        return rr;
    }

    public RuntimeRule createEmptyRule(final RuntimeRule ruleThatIsEmpty) {
        final RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, "$empty." + ruleThatIsEmpty.getName() + "$", this.nextRuleNumber, RuntimeRuleKind.TERMINAL,
                Pattern.LITERAL);
        ++this.nextRuleNumber;
        final RuntimeRuleItem emptyRhs = this.createRuntimeRuleItem(RuntimeRuleItemKind.EMPTY);
        rr.setRhs(emptyRhs);
        emptyRhs.setItems(new RuntimeRule[] { ruleThatIsEmpty });
        return rr;
    }

    public RuntimeRuleSet createRuntimeRuleSet(final int totalRuleNumber) {
        if (null == this.runtimeRuleSet) {
            this.runtimeRuleSet = new RuntimeRuleSet(totalRuleNumber);// , this.getEmptyRule().getRuleNumber());
        }
        return this.runtimeRuleSet;
    }

    public RuntimeRuleSet getRuntimeRuleSet() {
        if (null == this.runtimeRuleSet) {
            throw new RuntimeException("Internal Error: must createRuntimeRuleSet before getting");
        } else {
            return this.runtimeRuleSet;
        }
    }

    public Branch createBranch(final RuntimeRule r, final SPPTNode[] children) {
        return this.parseTreeFactory.createBranch(r, children);
    }

    public Leaf createLeaf(final String text, final int start, final int nextInputPosition, final RuntimeRule terminalRule) {
        return this.parseTreeFactory.createLeaf(text, start, nextInputPosition, terminalRule);
    }

    public Leaf createEmptyLeaf(final int pos, final RuntimeRule terminalRule) {
        return this.parseTreeFactory.createEmptyLeaf(pos, terminalRule);
    }

    public RuntimeRuleItem createRuntimeRuleItem(final RuntimeRuleItemKind kind) {
        final int maxRuleRumber = this.getRuntimeRuleSet().getTotalRuleNumber();
        return new RuntimeRuleItem(kind, maxRuleRumber);
    }
}
