package net.akehurst.language.grammar.parser;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.NodeType;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.parser.Parser;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.rules.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.Forrest3;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.log.Log;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.parse.graph.ICompleteNode;
import net.akehurst.language.parse.graph.IGrowingNode;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parse.graph.ParseGraph;
import net.akehurst.language.parser.sppf.IInput;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class ScannerLessParser3 implements Parser {

    public final static String START_SYMBOL = "\u0000";
    public final static TerminalLiteralDefault START_SYMBOL_TERMINAL = new TerminalLiteralDefault(ScannerLessParser3.START_SYMBOL);
    public final static String FINISH_SYMBOL = "\u0001";
    public final static TerminalLiteralDefault FINISH_SYMBOL_TERMINAL = new TerminalLiteralDefault(ScannerLessParser3.FINISH_SYMBOL);

    public ScannerLessParser3(final RuntimeRuleSetBuilder runtimeFactory, final Grammar grammar) {
        this.grammar = grammar;
        this.runtimeBuilder = runtimeFactory;
        this.converter = new Converter(this.runtimeBuilder);
    }

    private final Converter converter;
    private final RuntimeRuleSetBuilder runtimeBuilder;
    private final Grammar grammar;
    // Grammar pseudoGrammar;
    private RuntimeRuleSet runtimeRuleSet;

    private Grammar getGrammar() {
        return this.grammar;
    }

    private RuntimeRuleSet getRuntimeRuleSet() {
        if (null == this.runtimeRuleSet) {
            this.build();
        }
        return this.runtimeRuleSet;
    }

    private void init() {
        try {
            this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, (GrammarDefault) this.grammar);
        } catch (final Exception e) {
            e.printStackTrace();

        }
    }

    private SharedPackedParseTree parse2(final String goalRuleName, final IInput input) throws ParseFailedException, ParseTreeException {
        try {
            final NodeType goal = ((GrammarDefault) this.getGrammar()).findRule(goalRuleName).getNodeType();
            final ICompleteNode gr = this.parse3(goal, input);
            final SharedPackedParseTree tree = new SharedPackedParseTreeSimple((SPPTNode) gr);
            // set the parent property of each child, these are not set during parsing
            // TODO: don't know if we need this, probably not
            this.setParentForChildren((SPPTBranch) tree.getRoot());
            return tree;
        } catch (final GrammarRuleNotFoundException e) {
            // Should never happen!
            throw new RuntimeException("Should never happen", e);
        }
    }

    private void setParentForChildren(final SPPTBranch node) {
        final SPPTBranch parent = node;
        for (final SPPTNode child : parent.getChildren()) {
            child.setParent(parent);
            if (child instanceof SPPTBranch) {
                this.setParentForChildren((SPPTBranch) child);
            }
        }
    }

    private ICompleteNode parse3(final NodeType goal, final IInput input) throws ParseFailedException, GrammarRuleNotFoundException, ParseTreeException {

        final int goalRuleNumber = this.getRuntimeRuleSet().getRuleNumber(goal.getIdentity().asPrimitive());
        final RuntimeRule goalRR = this.getRuntimeRuleSet().getRuntimeRule(goalRuleNumber);
        final ICompleteNode node = this.doParse3(goalRR, input);
        // GraphNodeRoot gr = new GraphNodeRoot(goalRR, node.getChildren());
        return node;
    }

    private ICompleteNode doParse3(final RuntimeRule goalRule, final IInput input) throws ParseFailedException, GrammarRuleNotFoundException, ParseTreeException {
        // TODO: handle reader directly without converting to string
        final IParseGraph graph = new ParseGraph(goalRule, input);
        final Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);

        if (Log.on) {
            Log.traceln("%s", this.grammar);
            Log.traceln("");
            Log.traceln("input '%s'", input.getText());
        }
        int seasons = 0;
        if (Log.on) {
            Log.trace("%s", seasons);
        }

        newForrest.start(graph, goalRule, input);
        if (Log.on) {
            Log.traceln("");
        }
        seasons++;

        do {
            if (Log.on) {
                Log.trace("%s", seasons);
            }
            newForrest.grow();
            if (Log.on) {
                Log.traceln("");
            }
            seasons++;
        } while (newForrest.getCanGrow());

        final ICompleteNode match = newForrest.getLongestMatch();
        if (Log.on) {
            for (final ICompleteNode cn : graph.getGoals()) {
                Log.traceln("%s", cn.toStringTree());
            }
        }
        return match;
    }

    private List<RuleItem> expectedAt1(final String goalRuleName, final IInput input, final int position) throws ParseFailedException, ParseTreeException {
        try {
            final NodeType goal = ((GrammarDefault) this.getGrammar()).findRule(goalRuleName).getNodeType();
            if (null == this.runtimeRuleSet) {
                this.build();
            }
            final int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
            final RuntimeRule goalRule = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);

            final IParseGraph graph = new ParseGraph(goalRule, input);
            final Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);
            newForrest.start(graph, goalRule, input);
            int seasons = 1;
            // final int length = text.length();
            final List<IGrowingNode> matches = new ArrayList<>();
            do {
                newForrest.grow();
                seasons++;
                for (final IGrowingNode gn : newForrest.getLastGrown()) {
                    // may need to change this to finalInputPos!
                    if (input.getIsEnd(gn.getNextInputPosition())) {
                        matches.add(gn);
                    }
                }
            } while (newForrest.getCanGrow());

            if (matches.isEmpty()) {

            }

            // TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf
            // must reject the next expected

            final Set<RuntimeRule> expected = new HashSet<>();
            for (final IGrowingNode ep : matches) {
                final IGrowingNode gn = ep;
                boolean done = false;
                // while (!done) {
                if (gn.getCanGrowWidth()) {
                    expected.addAll(gn.getNextExpectedItem());
                    done = true;
                    // TODO: sum from all parents
                    // gn = gn.getPossibleParent().get(0).node;// .getNextExpectedItem();
                } else {
                    // if has height potential?
                    // gn = gn.getPossibleParent().get(0).node;

                }
                // }
            }
            // final List<RuntimeRule> expected = longest.getNextExpectedItem();
            final List<RuleItem> ruleItems = new ArrayList<>();
            for (final RuntimeRule rr : expected) {
                if (RuntimeRuleKind.NON_TERMINAL == rr.getKind()) {
                    final RuleItem ri = this.runtimeRuleSet.getOriginalItem(rr, this.getGrammar());
                    ruleItems.add(ri);
                } else {
                    ruleItems.add(this.getGrammar().findAllTerminal(rr.getTerminalPatternText()));
                }
            }
            // add skip rules at end
            for (final RuntimeRule rr : this.runtimeRuleSet.getAllSkipRules()) {
                // final IRule r = this.grammar.findAllRule(rr.getName());
                final RuleItem ri = this.runtimeRuleSet.getOriginalItem(rr, this.getGrammar());
                ruleItems.add(ri);
                // rules.add(new NonTerminalRuleReference(this.getGrammar(), rr.getName()));
            }
            return ruleItems;
        } catch (final GrammarRuleNotFoundException e) {
            // Should never happen!
            throw new RuntimeException("Should never happen", e);
        }
    }

    // --- IParser ---
    @Override
    public void build() {
        this.init();
        this.runtimeRuleSet.build();
    }

    @Override
    public Set<NodeType> getNodeTypes() {
        return this.getGrammar().findAllNodeType();
    }

    @Override
    public SharedPackedParseTree parse(final String goalRuleName, final CharSequence inputText)
            throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
        final IInput input = new Input3(this.runtimeBuilder, inputText);
        final SharedPackedParseTree f = this.parse2(goalRuleName, input);
        return f;
    }

    @Override
    public SharedPackedParseTree parse(final String goalRuleName, final Reader inputText)
            throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
        // TODO: find a way to parse straight from the input, without reading it all
        final BufferedReader br = new BufferedReader(inputText);
        final String text = br.lines().collect(Collectors.joining(System.lineSeparator()));

        final Input3 input = new Input3(this.runtimeBuilder, text);
        final SharedPackedParseTree f = this.parse2(goalRuleName, input);
        return f;
    }

    @Override
    public List<RuleItem> expectedAt(final String goalRuleName, final CharSequence inputText, final int position)
            throws ParseFailedException, ParseTreeException {
        final CharSequence text = inputText.subSequence(0, Math.min(position, inputText.length()));
        final Input3 input = new Input3(this.runtimeBuilder, text);
        return this.expectedAt1(goalRuleName, input, position);
    }

    @Override
    public List<RuleItem> expectedAt(final String goalRuleName, final Reader inputReader, final int position) throws ParseFailedException, ParseTreeException {
        final BufferedReader br = new BufferedReader(inputReader);
        String text = br.lines().collect(Collectors.joining(System.lineSeparator()));
        text = text.substring(0, Math.min(position, text.length()));
        final Input3 input = new Input3(this.runtimeBuilder, text);
        return this.expectedAt1(goalRuleName, input, position);
    }
}
