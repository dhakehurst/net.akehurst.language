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
package net.akehurst.language.parser.sppf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPNodeIdentity;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.InputFromCharSequence;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalAbstract;
import net.akehurst.language.ogl.semanticStructure.TerminalEmptyDefault;

public class ParseTreeBuilder {

    private static Pattern WS = Pattern.compile("(\\s)+");
    private static Pattern EMPTY = Pattern.compile("[$]empty");
    private static Pattern NAME = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
    private static Pattern LITERAL = Pattern.compile("'(?:\\\\?.)*?'");
    private static Pattern COLON = Pattern.compile("[:]");
    private static Pattern CHILDREN_START = Pattern.compile("[{]");
    private static Pattern CHILDREN_END = Pattern.compile("[}]");

    private final RuntimeRuleSetBuilder runtimeBuilder;
    private final GrammarDefault grammar;
    private final InputFromCharSequence input;
    // private final String textAccumulator;
    private int textLength;
    private final int offset;
    private final StringBuilder sb;
    private SharedPackedParseTree currentTree;
    private final Map<SPNodeIdentity, SPPTNode> node_cache;

    public ParseTreeBuilder(final RuntimeRuleSetBuilder runtimeRules, final GrammarDefault grammar, final String goal, final CharSequence text,
            final int offset) {
        this.runtimeBuilder = runtimeRules;
        this.input = new InputFromCharSequence(runtimeRules, text);
        this.grammar = grammar;
        // this.textAccumulator = "";
        this.offset = offset;
        this.sb = new StringBuilder();
        this.node_cache = new HashMap<>();
        this.reset();
        this.clear();
    }

    private void reset() {
        // this.textAccumulator = "";
        this.textLength = 0;
        this.sb.delete(0, this.sb.length());
    }

    private void cacheNode(final SPPTNode node) {
        this.node_cache.put(node.getIdentity(), node);
    }

    private SPPTNode findNode(final SPNodeIdentity id) {
        return this.node_cache.get(id);
    }

    private SPPTLeaf findLeaf(final SPNodeIdentity id) {
        final SPPTNode n = this.findNode(id);
        return (SPPTLeaf) n;
    }

    private SPPTBranch findBranch(final SPNodeIdentity id) {
        final SPPTNode n = this.findNode(id);
        return (SPPTBranch) n;
    }

    public SPPTLeaf leaf(final String text) {
        return this.leaf(text, text);
    }

    public SPPTLeaf leaf(final String terminalPattern, final String text) {
        final int start = this.textLength + this.offset;
        this.textLength += text.length();
        final int end = this.textLength + this.offset;
        TerminalAbstract terminal = null;
        if (terminalPattern.isEmpty()) {
            terminal = new TerminalEmptyDefault();
        } else {
            final Optional<Terminal> op = this.grammar.getAllTerminal().stream()
                    .filter(t -> ((TerminalAbstract) t).getPattern().pattern().equals(terminalPattern)).findFirst();
            if (op.isPresent()) {
                terminal = (TerminalAbstract) op.get();
            } else {
                throw new RuntimeException("No such terminal \"" + terminalPattern + "\"");
            }
        }
        final RuntimeRule terminalRule = this.runtimeBuilder.getRuntimeRuleSet().getForTerminal(terminal.getValue());
        final SPPTLeaf n = this.runtimeBuilder.createLeaf(text, start, end, terminalRule);

        SPPTLeaf existing = this.findLeaf(n.getIdentity());
        if (null == existing) {
            this.cacheNode(n);
            existing = n;
        }

        return existing;
    }

    public SPPTLeaf emptyLeaf(final String ruleNameThatIsEmpty) {
        final int start = this.textLength + this.offset;
        final RuntimeRule ruleThatIsEmpty = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(ruleNameThatIsEmpty);
        final RuntimeRule terminalRule = this.runtimeBuilder.getRuntimeRuleSet().getEmptyRule(ruleThatIsEmpty);
        final SPPTLeaf n = this.runtimeBuilder.createEmptyLeaf(start, terminalRule);

        SPPTLeaf existing = this.findLeaf(n.getIdentity());
        if (null == existing) {
            this.cacheNode(n);
            existing = n;
        }

        return existing;
    }

    @Deprecated
    public SPPTBranch branch(final String ruleName, final SPPTNode... children) {
        try {
            final Rule rule = this.grammar.findRule(ruleName);
            final RuntimeRule rr = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(rule);
            final SPPTBranch b = this.runtimeBuilder.createBranch(rr, children);
            return b;
        } catch (final GrammarRuleNotFoundException e) {
            throw new RuntimeException("Error", e);
        }
    }

    public SPPTBranch branch(final String ruleName, final List<SPPTNode> children) {
        try {
            final Rule rule = this.grammar.findRule(ruleName);
            final RuntimeRule rr = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(rule);
            final SPPTBranch n = this.runtimeBuilder.createBranch(rr, children.toArray(new SPPTNode[children.size()]));

            SPPTBranch existing = this.findBranch(n.getIdentity());
            if (null == existing) {
                this.cacheNode(n);
                existing = n;
            } else {
                final FixedList<SPPTNode> newChildren = n.getChildren(); // no need to clone as list is fixed!
                existing.getChildrenAlternatives().add(newChildren);
            }

            return existing;
        } catch (final GrammarRuleNotFoundException e) {
            throw new RuntimeException("Error", e);
        }
    }

    public void define(final String s) {
        this.sb.append(s);
    }

    // private IGrammar getTreeGrammar() {
    // if (null == this.treeGrammar) {
    // try {
    // final OGLGrammar ogl = new OGLGrammar();
    // final IParser grammarParser = new ScannerLessParser3(new RuntimeRuleSetBuilder(), ogl);
    // final InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream("net/akehurst/language/parser/Tree.ogl");
    // final Reader reader = new InputStreamReader(input);
    // final ISharedPackedParseTree grammarTree = grammarParser.parse("grammarDefinition", reader);
    // final ISemanticAnalyser sa = new SemanicAnalyser();
    // this.treeGrammar = sa.analyse(IGrammar.class, grammarTree);
    //
    // } catch (final ParseFailedException | ParseTreeException | RuleNotFoundException | UnableToAnalyseExeception e) {
    // e.printStackTrace();
    // }
    // }
    // return this.treeGrammar;
    // }

    private static class SimpleScanner {
        private final CharSequence input;
        private int nextPosition;

        public SimpleScanner(final CharSequence input) {
            this.input = input;
            this.nextPosition = 0;

        }

        public boolean hasMore() {
            return this.nextPosition < this.input.length();
        }

        public boolean hasNext(final Pattern pattern) {
            final Matcher m = pattern.matcher(this.input).region(this.nextPosition, this.input.length());
            return m.lookingAt();
        }

        public String next(final Pattern pattern) {
            final Matcher m = pattern.matcher(this.input).region(this.nextPosition, this.input.length());
            if (m.lookingAt()) {
                final String match = m.group();
                this.nextPosition += match.length();
                return match;
            } else {
                throw new RuntimeException(String.format("Error scanning for pattern %s at Position %s", pattern, this.nextPosition));
            }
        }

        public int getPosition() {
            return this.nextPosition;
        }
    }

    private SharedPackedParseTree parse(final String treeString) {

        SharedPackedParseTree tree = null;

        final SimpleScanner scanner = new SimpleScanner(treeString);
        {
            final Stack<String> nodeNamesStack = new Stack<>();
            final Stack<List<SPPTNode>> childrenStack = new Stack<>();

            // add rootList
            childrenStack.push(new ArrayList<>());

            while (scanner.hasMore()) {

                if (scanner.hasNext(ParseTreeBuilder.WS)) {
                    scanner.next(ParseTreeBuilder.WS);
                } else if (scanner.hasNext(ParseTreeBuilder.NAME)) {
                    final String name = scanner.next(ParseTreeBuilder.NAME);
                    nodeNamesStack.push(name);
                } else if (scanner.hasNext(ParseTreeBuilder.CHILDREN_START)) {
                    scanner.next(ParseTreeBuilder.CHILDREN_START);
                    childrenStack.push(new ArrayList<>());
                } else if (scanner.hasNext(ParseTreeBuilder.LITERAL)) {
                    final String leafStr = scanner.next(ParseTreeBuilder.LITERAL);
                    final String text = leafStr.substring(1, leafStr.length() - 1);

                    while (scanner.hasNext(ParseTreeBuilder.WS)) {
                        scanner.next(ParseTreeBuilder.WS);
                    }

                    if (scanner.hasNext(ParseTreeBuilder.COLON)) {
                        scanner.next(ParseTreeBuilder.COLON);
                        final String pattern = text;

                        while (scanner.hasNext(ParseTreeBuilder.WS)) {
                            scanner.next(ParseTreeBuilder.WS);
                        }

                        final String newText = scanner.next(ParseTreeBuilder.LITERAL);
                        final String newText2 = newText.substring(1, newText.length() - 1);
                        final SPPTLeaf leaf = this.leaf(pattern, newText2);
                        childrenStack.peek().add(leaf);
                    } else {

                        final SPPTLeaf leaf = this.leaf(text);
                        childrenStack.peek().add(leaf);
                    }
                } else if (scanner.hasNext(ParseTreeBuilder.EMPTY)) {
                    final String empty = scanner.next(ParseTreeBuilder.EMPTY);
                    final String ruleNameThatIsEmpty = nodeNamesStack.peek();
                    final SPPTNode emptyNode = this.emptyLeaf(ruleNameThatIsEmpty);
                    childrenStack.peek().add(emptyNode);

                } else if (scanner.hasNext(ParseTreeBuilder.CHILDREN_END)) {
                    scanner.next(ParseTreeBuilder.CHILDREN_END);
                    final String lastNodeName = nodeNamesStack.pop();

                    final List<SPPTNode> children = childrenStack.pop();
                    final SPPTNode node = this.branch(lastNodeName, children);
                    childrenStack.peek().add(node);

                } else {
                    throw new RuntimeException("Tree String invalid at position " + scanner.getPosition());
                }

            }

            tree = new SharedPackedParseTreeSimple(childrenStack.pop().get(0), -1);
        }
        return tree;
    }

    /**
     * builds the defined parse tree, and adds it to the current Shared Packed Parse Tree.
     *
     * @return current Shared Packed Parse Tree
     */
    public SharedPackedParseTree buildAndAdd() {
        try {
            final String treeStr = this.sb.toString();
            final SharedPackedParseTree treeTree = this.parse(treeStr);
            this.reset();
            return treeTree;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * clears (resets) the builder, ready to build a new Shared Packed Parse Tree
     */
    public void clear() {
        this.currentTree = null;
    }
}
