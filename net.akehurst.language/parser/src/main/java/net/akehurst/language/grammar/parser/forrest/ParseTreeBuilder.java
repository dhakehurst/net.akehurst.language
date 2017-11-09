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
package net.akehurst.language.grammar.parser.forrest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.analyser.ISemanticAnalyser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.grammar.ITerminal;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parse.tree.ParseTree;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticAnalyser.SemanicAnalyser;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.ogl.semanticStructure.TerminalEmpty;

public class ParseTreeBuilder {

	private static Pattern WS = Pattern.compile("(\\s)+");
	private static Pattern EMPTY = Pattern.compile("$empty");
	private static Pattern NAME = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
	private static Pattern LITERAL = Pattern.compile("'(?:\\\\?.)*?'");
	private static Pattern COLON = Pattern.compile("[:]");
	private static Pattern CHILDREN_START = Pattern.compile("[{]");
	private static Pattern CHILDREN_END = Pattern.compile("[}]");

	public ParseTreeBuilder(final RuntimeRuleSetBuilder runtimeRules, final Grammar grammar, final String goal, final CharSequence text, final int offset) {
		this.runtimeBuilder = runtimeRules;
		this.input = new Input3(runtimeRules, text);
		this.grammar = grammar;
		this.textAccumulator = "";
		this.textLength = 0;
		this.offset = offset;
		this.sb = new StringBuilder();
	}

	RuntimeRuleSetBuilder runtimeBuilder;
	Grammar grammar;
	Input3 input;
	String textAccumulator;
	int textLength;
	int offset;
	StringBuilder sb;
	private IGrammar treeGrammar;

	public ILeaf leaf(final String text) {
		return this.leaf(text, text);
	}

	public ILeaf leaf(final String terminalPattern, final String text) {
		final int start = this.textLength + this.offset;
		this.textLength += text.length();
		final int end = this.textLength + this.offset;
		Terminal terminal = null;
		if (terminalPattern.isEmpty()) {
			terminal = new TerminalEmpty();
		} else {
			final Optional<ITerminal> op = this.grammar.getAllTerminal().stream().filter(t -> ((Terminal) t).getPattern().pattern().equals(terminalPattern))
					.findFirst();
			if (op.isPresent()) {
				terminal = (Terminal) op.get();
			} else {
				throw new RuntimeException("No such terminal \"" + terminalPattern + "\"");
			}
		}
		final RuntimeRule terminalRule = this.runtimeBuilder.getRuntimeRuleSet().getForTerminal(terminal.getValue());
		final ILeaf l = this.runtimeBuilder.createLeaf(text, start, end, terminalRule);
		return l;
	}

	public ILeaf emptyLeaf(final String ruleNameThatIsEmpty) {
		final int start = this.textLength + this.offset;
		final RuntimeRule ruleThatIsEmpty = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(ruleNameThatIsEmpty);
		final RuntimeRule terminalRule = this.runtimeBuilder.getRuntimeRuleSet().getEmptyRule(ruleThatIsEmpty);
		return this.runtimeBuilder.createEmptyLeaf(start, terminalRule);
	}

	public IBranch branch(final String ruleName, final INode... children) {
		try {
			final Rule rule = this.grammar.findRule(ruleName);
			final RuntimeRule rr = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(rule);
			final IBranch b = this.runtimeBuilder.createBranch(rr, children);
			return b;
		} catch (final RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

	public IBranch branch(final String ruleName, final List<INode> children) {
		try {
			final Rule rule = this.grammar.findRule(ruleName);
			final RuntimeRule rr = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(rule);
			final IBranch b = this.runtimeBuilder.createBranch(rr, children.toArray(new INode[children.size()]));
			return b;
		} catch (final RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

	public void define(final String s) {
		this.sb.append(s);
	}

	private IGrammar getTreeGrammar() {
		if (null == this.treeGrammar) {
			try {
				final OGLGrammar ogl = new OGLGrammar();
				final IParser grammarParser = new ScannerLessParser3(new RuntimeRuleSetBuilder(), ogl);
				final InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream("net/akehurst/language/parser/Tree.ogl");
				final Reader reader = new InputStreamReader(input);
				final IParseTree grammarTree = grammarParser.parse("grammarDefinition", reader);
				final ISemanticAnalyser sa = new SemanicAnalyser();
				this.treeGrammar = sa.analyse(IGrammar.class, grammarTree);

			} catch (final ParseFailedException | ParseTreeException | RuleNotFoundException | UnableToAnalyseExeception e) {
				e.printStackTrace();
			}
		}
		return this.treeGrammar;
	}

	class SimpleScaner {
		private final CharSequence input;
		private int nextPosition;

		public SimpleScaner(final CharSequence input) {
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

	private IParseTree parse(final String treeString) {

		IParseTree tree = null;

		final SimpleScaner scanner = new SimpleScaner(treeString);
		{
			final Stack<String> nodeNamesStack = new Stack<>();
			final Stack<List<INode>> childrenStack = new Stack<>();

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
						final ILeaf leaf = this.leaf(pattern, newText2);
						childrenStack.peek().add(leaf);
					} else {

						final ILeaf leaf = this.leaf(text);
						childrenStack.peek().add(leaf);
					}
				} else if (scanner.hasNext(ParseTreeBuilder.EMPTY)) {
					final String empty = scanner.next(ParseTreeBuilder.EMPTY);
					nodeNamesStack.push(empty);
				} else if (scanner.hasNext(ParseTreeBuilder.CHILDREN_END)) {
					scanner.next(ParseTreeBuilder.CHILDREN_END);
					final String lastNodeName = nodeNamesStack.pop();
					if (ParseTreeBuilder.EMPTY.matcher(lastNodeName).matches()) {
						final String ruleNameThatIsEmpty = nodeNamesStack.pop();
						final INode node = this.emptyLeaf(ruleNameThatIsEmpty);
						childrenStack.peek().add(node);
					} else {
						final List<INode> children = childrenStack.pop();
						final INode node = this.branch(lastNodeName, children);
						childrenStack.peek().add(node);
					}
				} else {
					throw new RuntimeException("Tree String invalid at position " + scanner.getPosition());
				}

			}

			tree = new ParseTree(childrenStack.pop().get(0));
		}
		return tree;
	}

	public IParseTree build() {
		try {
			final String treeStr = this.sb.toString();
			final IParseTree treeTree = this.parse(treeStr);
			return treeTree;
			// final IParseTreeVisitor<Object, String, Throwable> v = new IParseTreeVisitor<Object, String, Throwable>() {
			//
			// @Override
			// public Object visit(final IParseTree target, final String arg) throws Throwable {
			// return target.getRoot().accept(this, arg);
			// }
			//
			// @Override
			// public Object visit(final ILeaf target, final String arg) throws Throwable {
			// // TODO Auto-generated method stub
			// return null;
			// }
			//
			// @Override
			// public Object visit(final IBranch target, final String arg) throws Throwable {
			// final String name = target.getName();
			// switch (name) {
			// case "tree":
			// return new ParseTree((INode) target.getChild(0).accept(this, arg));
			// case "nodes":
			// return target.getBranchNonSkipChildren().stream().map((b) -> {
			// try {
			// return b.accept(this, arg);
			// } catch (final Throwable e) {
			// e.printStackTrace();
			// return null;
			// }
			// }).collect(Collectors.toList()).toArray(new INode[0]);
			// case "node":
			// return target.getBranchChild(0).accept(this, arg);
			// case "branch":
			// return ParseTreeBuilder.this.branch(target.getBranchChild(0).getChild(0).getMatchedText(),
			// (INode[]) target.getBranchChild(2).accept(this, target.getBranchChild(0).getChild(0).getMatchedText()));
			// case "leaf":
			// return target.getBranchChild(0).accept(this, arg);
			// case "pattern":
			// return ParseTreeBuilder.this.leaf(
			// target.getBranchChild(0).getChild(0).getMatchedText().substring(1,
			// target.getBranchChild(0).getChild(0).getMatchedText().length() - 1),
			// target.getBranchChild(2).getChild(0).getMatchedText().substring(1,
			// target.getBranchChild(2).getChild(0).getMatchedText().length() - 1));
			// case "literal":
			// return ParseTreeBuilder.this
			// .leaf(target.getChild(0).getMatchedText().substring(1, target.getChild(0).getMatchedText().length() - 1));
			// case "empty":
			// return ParseTreeBuilder.this.emptyLeaf(arg);
			// case "NAME":
			// default:
			// throw new RuntimeException("Error, unhandled branch");
			// }
			// }
			//
			// };
			// final IParseTree result = (IParseTree) v.visit(treeTree, null);
			// return result;
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	// public IParseTree tree(IBranch root) {
	// try {
	// Rule rule = this.grammar.findRule(root.getName());
	// RuntimeRule rr = this.factory.getRuntimeRuleSet().getRuntimeRule(rule);
	// IParseTree t = new ParseTreeBranch(this.factory, input, (Branch)root, null, rr, Integer.MAX_VALUE);
	// return t;
	// } catch (RuleNotFoundException e) {
	// throw new RuntimeException("Error", e);
	// }
	// }

}
