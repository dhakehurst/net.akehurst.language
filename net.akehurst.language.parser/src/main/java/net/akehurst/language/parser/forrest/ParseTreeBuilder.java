package net.akehurst.language.parser.forrest;

import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNodeType;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;

public class ParseTreeBuilder {

	public ParseTreeBuilder(Factory factory, Grammar grammar, String goal, CharSequence text) {
		this.factory = factory;
		this.input = new Input(factory, " "+text);
		this.grammar = grammar;
		this.textAccumulator = "";
		this.textLength = 0;
	}

	Factory factory;
	Grammar grammar;
	Input input;
	String textAccumulator;
	int textLength;

	public ILeaf leaf(String terminalPattern, String text) {
		int start = this.textLength+1;
		this.textLength+=text.length();
		int end = this.textLength +1;
		Terminal terminal = this.grammar.getAllTerminal().stream().filter(t -> t.getPattern().pattern().equals(terminalPattern)).findFirst().get();
		ILeaf l = new Leaf(this.input, start, end, terminal);
		return l;
	}

	public ILeaf emptyLeaf() {
		int start = this.textLength +1;
		return new Leaf(this.input, start, start, new TerminalLiteral(""));
	}

	public IBranch branch(String ruleName, INode... children) {
		try {
			List<INode> childrenLst = Arrays.asList(children);
			Rule rule = this.grammar.findRule(ruleName);
			INodeType nodeType = rule.getNodeType();
			IBranch b = this.factory.createBranch(nodeType, childrenLst);
			return b;
		} catch (RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

	public IParseTree tree(IBranch root) {
		try {
			Rule rule = this.grammar.findRule(root.getName());
			IParseTree t = new ParseTreeBranch(this.factory, input, root, null, rule, Integer.MAX_VALUE);
			return t;
		} catch (RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

}
