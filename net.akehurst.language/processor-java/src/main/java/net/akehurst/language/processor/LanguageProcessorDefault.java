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
package net.akehurst.language.processor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.api.analyser.SemanticAnalyser;
import net.akehurst.language.api.analyser.UnableToAnalyseExeception;
import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.parser.Parser;
import net.akehurst.language.api.processor.CompletionItem;
import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;

public class LanguageProcessorDefault implements LanguageProcessor {

    private final RuntimeRuleSetBuilder parseTreeFactory;
    private final Grammar grammar;
    private final Parser parser;
    private final SemanticAnalyser semanticAnalyser;
    private CompletionProvider completionProvider;

    public LanguageProcessorDefault(final Grammar grammar, final SemanticAnalyser semanticAnalyser) {
        this.grammar = grammar;
        // this.defaultGoalName = defaultGoalName;
        // this.lexicalAnalyser = new LexicalAnalyser(grammar.findTokenTypes());
        this.parseTreeFactory = new RuntimeRuleSetBuilder();
        this.parser = new ScannerLessParser3(this.parseTreeFactory, grammar);
        this.semanticAnalyser = semanticAnalyser;
    }

    @Override
    public Grammar getGrammar() {
        return this.grammar;
    }

    @Override
    public Parser getParser() {
        return this.parser;
    }

    public SemanticAnalyser getSemanticAnalyser() {
        return this.semanticAnalyser;
    }

    public CompletionProvider getCompletionProvider() {
        if (null == this.completionProvider) {
            this.completionProvider = new CompletionProvider();
        }
        return this.completionProvider;
    }

    @Override
    public <T> T process(final String text, final String goalRuleName, final Class<T> targetType) throws ParseFailedException, UnableToAnalyseExeception {
        try {

            final SharedPackedParseTree forest = this.getParser().parse(goalRuleName, text);
            if (null == this.getSemanticAnalyser()) {
                throw new UnableToAnalyseExeception("No SemanticAnalyser supplied", null);
            }
            final T t = this.getSemanticAnalyser().analyse(targetType, forest);

            return t;
        } catch (final GrammarRuleNotFoundException | ParseTreeException e) {
            throw new ParseFailedException(e.getMessage(), null, new HashMap<>());
        }
    }

    @Override
    public <T> T process(final Reader reader, final String goalRuleName, final Class<T> targetType) {
        final SharedPackedParseTree forest = this.getParser().parse(goalRuleName, reader);
        if (null == this.getSemanticAnalyser()) {
            throw new UnableToAnalyseExeception("No SemanticAnalyser supplied", null);
        }
        final T t = this.getSemanticAnalyser().analyse(targetType, forest);

        return t;
    }

    @Override
    public List<CompletionItem> expectedAt(final String text, final String goalRuleName, final long position, final long desiredDepth) {
        final List<RuleItem> parserExpected = this.getParser().expectedAt(goalRuleName, text, position);
        final Set<CompletionItem> expected = new LinkedHashSet<>();
        for (final RuleItem item : parserExpected) {
            final List<CompletionItem> exp = this.getCompletionProvider().provideFor(item, desiredDepth);
            expected.addAll(exp);
        }
        return new ArrayList<>(expected);
    }

    @Override
    public List<CompletionItem> expectedAt(final Reader reader, final String goalRuleName, final long position, final long desiredDepth) {
        final List<RuleItem> parserExpected = this.getParser().expectedAt(goalRuleName, reader, position);
        final Set<CompletionItem> expected = new LinkedHashSet<>();
        for (final RuleItem item : parserExpected) {
            final List<CompletionItem> exp = this.getCompletionProvider().provideFor(item, desiredDepth);
            expected.addAll(exp);
        }
        return new ArrayList<>(expected);
    }
}
