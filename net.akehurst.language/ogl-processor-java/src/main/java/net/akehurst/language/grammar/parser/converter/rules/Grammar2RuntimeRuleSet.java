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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalAbstract;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Grammar2RuntimeRuleSet implements BinaryRule<GrammarDefault, RuntimeRuleSet> {

    @Override
    public boolean isValidForLeft2Right(final GrammarDefault arg0) {
        return true;
    }

    @Override
    public boolean isAMatch(final GrammarDefault left, final RuntimeRuleSet right, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public RuntimeRuleSet constructLeft2Right(final GrammarDefault left, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final int totalRuleNumber = left.getAllRule().size() + left.getAllTerminal().size() + 1000; //FIXME: bad code...find the right number!
        final RuntimeRuleSet right = converter.getFactory().createRuntimeRuleSet(totalRuleNumber);
        return right;
    }

    @Override
    public void updateLeft2Right(final GrammarDefault left, final RuntimeRuleSet right, final BinaryTransformer transformer) {
        final Converter converter = (Converter) transformer;
        final List<Rule> rules = left.getAllRule();
        final List<Terminal> terminals = Arrays.asList(left.getAllTerminal().toArray(new TerminalAbstract[0]));

        final List<? extends RuntimeRule> runtimeRules = transformer.transformAllLeft2Right(Rule2RuntimeRule.class, rules);
        final List<? extends RuntimeRule> runtimeRules2 = transformer.transformAllLeft2Right(Terminal2RuntimeRule.class, terminals);

        final List<RuntimeRule> rr = new ArrayList<>();
        rr.addAll(runtimeRules);
        rr.addAll(runtimeRules2);
        rr.addAll(converter.getVirtualRules());

        right.setRuntimeRules(rr);

    }

    @Override
    public void updateRight2Left(final GrammarDefault arg0, final RuntimeRuleSet arg1, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub

    }

    @Override
    public GrammarDefault constructRight2Left(final RuntimeRuleSet arg0, final BinaryTransformer transformer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidForRight2Left(final RuntimeRuleSet arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}