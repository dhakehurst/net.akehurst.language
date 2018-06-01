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
package net.akehurst.language.ogl.semanticAnalyser;

import net.akehurst.language.core.analyser.GrammarLoader;
import net.akehurst.language.core.analyser.SemanticAnalyser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.sppt.SPPTBranch;
import net.akehurst.language.core.sppt.SharedPackedParseTree;
import net.akehurst.language.ogl.semanticAnalyser.rules.AbstractNode2Choice;
import net.akehurst.language.ogl.semanticAnalyser.rules.AbstractNode2ConcatenationItem;
import net.akehurst.language.ogl.semanticAnalyser.rules.AbstractNode2TangibleItem;
import net.akehurst.language.ogl.semanticAnalyser.rules.AbstractNode2Terminal;
import net.akehurst.language.ogl.semanticAnalyser.rules.AbstractRhsNode2RuleItem;
import net.akehurst.language.ogl.semanticAnalyser.rules.AnyRuleNode2Rule;
import net.akehurst.language.ogl.semanticAnalyser.rules.GrammarDefinitionBranch2Grammar;
import net.akehurst.language.ogl.semanticAnalyser.rules.IDENTIFIERBranch2String;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2ChoicePriority;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2ChoiceSimple;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2Concatenation;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2ConcatenationItem;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2Group;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2Multi;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2Namespace;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2NonTerminal;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2SeparatedList;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2SimpleItem;
import net.akehurst.language.ogl.semanticAnalyser.rules.Node2Terminal;
import net.akehurst.language.ogl.semanticAnalyser.rules.NormalRuleNode2Rule;
import net.akehurst.language.ogl.semanticAnalyser.rules.SkipRuleNode2SkipRule;
import net.akehurst.language.ogl.semanticAnalyser.rules.TerminalLiteralNode2Terminal;
import net.akehurst.language.ogl.semanticAnalyser.rules.TerminalPatternNode2Terminal;
import net.akehurst.language.ogl.semanticStructure.GrammarStructure;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.basic.BinaryTransformerBasic;

public class OglSemanicAnalyserRuleBased extends BinaryTransformerBasic implements SemanticAnalyser {

    private GrammarLoader grammarLoader;

    public OglSemanicAnalyserRuleBased() {
        super.registerRule((Class<? extends BinaryRule<?, ?>>) (Class<?>) AbstractNode2Choice.class);
        super.registerRule((Class<? extends BinaryRule<?, ?>>) (Class<?>) AbstractNode2ConcatenationItem.class);
        super.registerRule((Class<? extends BinaryRule<?, ?>>) (Class<?>) AbstractNode2TangibleItem.class);
        super.registerRule((Class<? extends BinaryRule<?, ?>>) (Class<?>) AbstractNode2Terminal.class);
        super.registerRule((Class<? extends BinaryRule<?, ?>>) (Class<?>) AbstractRhsNode2RuleItem.class);
        super.registerRule(AnyRuleNode2Rule.class);
        super.registerRule(GrammarDefinitionBranch2Grammar.class);
        super.registerRule(IDENTIFIERBranch2String.class);
        super.registerRule(Node2ChoicePriority.class);
        super.registerRule(Node2ChoiceSimple.class);
        super.registerRule(Node2Concatenation.class);
        super.registerRule(Node2ConcatenationItem.class);
        super.registerRule(Node2Group.class);
        super.registerRule(Node2Multi.class);
        super.registerRule(Node2Namespace.class);
        super.registerRule(Node2NonTerminal.class);
        super.registerRule(Node2SeparatedList.class);
        super.registerRule(Node2SimpleItem.class);
        super.registerRule(Node2Terminal.class);
        super.registerRule(NormalRuleNode2Rule.class);
        super.registerRule(SkipRuleNode2SkipRule.class);
        super.registerRule(TerminalLiteralNode2Terminal.class);
        super.registerRule(TerminalPatternNode2Terminal.class);
    }

    GrammarStructure analyse(final SharedPackedParseTree parseTree) throws UnableToAnalyseExeception {
        try {
            final SPPTBranch root = (SPPTBranch) parseTree.getRoot();
            final GrammarStructure grammar = this.transformLeft2Right(GrammarDefinitionBranch2Grammar.class, root);
            return grammar;
        } catch (final Exception e) {
            throw new UnableToAnalyseExeception("Cannot Analyse ParseTree", e);
        }

    }

    @Override
    public <T> T analyse(final Class<T> targetType, final SharedPackedParseTree forest) throws UnableToAnalyseExeception {
        return (T) this.analyse(forest);
    }

    @Override
    public GrammarLoader getGrammarLoader() {
        return this.grammarLoader;
    }

    @Override
    public void setGrammarLoader(final GrammarLoader value) {
        this.grammarLoader = value;
    }
}
