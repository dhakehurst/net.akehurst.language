package net.akehurst.language.parse.graph;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeLeaf implements ICompleteNode, ILeaf {

	public GraphNodeLeaf(final ParseGraph graph, final Leaf leaf) {
		this.leaf = leaf;
		this.finalMatchedTextLength = leaf.getMatchedTextLength();
	}

	private final Leaf leaf;
	private final int finalMatchedTextLength;
	private IBranch parent;

	@Override
	public RuntimeRule getRuntimeRule() {
		return this.leaf.getRuntimeRule();
	}

	@Override
	public int getRuntimeRuleNumber() {
		return this.getRuntimeRule().getRuleNumber();
	}

	@Override
	public int getStartPosition() {
		return this.leaf.getStartPosition();
	}

	@Override
	public int getEndPosition() {
		return this.leaf.getEndPosition();
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean getIsEmptyLeaf() {
		return this.leaf.getIsEmptyLeaf();
	}

	@Override
	public List<ChildrenOption> getChildrenOption() {
		return Collections.emptyList();
	}

	@Override
	public String getMatchedText() {
		return this.leaf.getMatchedText();// input.get(this.start, this.end).toString();
	}

	// --- ILeaf ---
	@Override
	public int getMatchedTextLength() {
		return this.finalMatchedTextLength;
	}

	@Override
	public boolean isPattern() {
		return this.leaf.isPattern();
	}

	// --- INode ---
	@Override
	public String getName() {
		return this.getRuntimeRule().getName();
	}

	@Override
	public int getNumberOfLines() {
		final String str = this.getMatchedText();
		final Pattern p = Pattern.compile(System.lineSeparator());
		final Matcher m = p.matcher(str);
		int count = 0;
		while (m.find()) {
			count += 1;
		}
		return count;
	}

	@Override
	public IBranch getParent() {
		return this.parent;
	}

	@Override
	public void setParent(final IBranch value) {
		this.parent = value;
	}

	// --- IParseTreeVisitable ---
	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	// --- Object ---
	@Override
	public String toString() {
		String r = "";
		r += this.getStartPosition() + ",";
		r += this.getMatchedTextLength() + ",";
		r += "C";
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		return r;
	}

}
