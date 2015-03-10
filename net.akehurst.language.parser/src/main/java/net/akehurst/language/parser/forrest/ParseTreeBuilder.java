package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNodeType;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.ScannerLessParser;

public class ParseTreeBuilder {

	public ParseTreeBuilder(Grammar grammar, String goal, CharSequence text) {
		//this.grammar = grammar;
		this.input = new Input(text);
		this.grammar = grammar;
		this.textAccumulator = "";
		this.textLength = 0;
//		Rule goalRule = new Rule(this.grammar, "$goal$");
//		goalRule.setRhs(new Concatination(new TerminalLiteral(ScannerLessParser.START_SYMBOL), new NonTerminal(goal), new TerminalLiteral(ScannerLessParser.FINISH_SYMBOL)));
//		grammar.setRule(Arrays.asList(new Rule[]{
//				goalRule
//		}));
	}

	Grammar grammar;
	Input input;
	String textAccumulator;
	int textLength;

	public ILeaf leaf(String terminalPattern, String text) {
		int start = this.textLength;
		this.textLength+=text.length();
		int end = this.textLength;
		Terminal terminal = this.grammar.getAllTerminal().stream().filter(t -> t.getPattern().pattern().equals(terminalPattern)).findFirst().get();
		ILeaf l = new Leaf(this.input, start, end, terminal);
		return l;
	}

	public ILeaf emptyLeaf() {
		int start = this.textLength;
		return new Leaf(this.input, start, start, new TerminalLiteral(""));
	}

	public IBranch branch(String ruleName, INode... children) {
		try {
			List<INode> childrenLst = Arrays.asList(children);
			Rule rule = this.grammar.findRule(ruleName);
			INodeType nodeType = new RuleNodeType(rule);
			IBranch b = new Branch(nodeType, childrenLst);
			return b;
		} catch (RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

	public IParseTree tree(IBranch root) {
		try {
			Rule rule = this.grammar.findRule(root.getName());
			IParseTree t = new ParseTreeBranch(input, root, new Stack<>(), rule, Integer.MAX_VALUE);
			return t;
		} catch (RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

}
