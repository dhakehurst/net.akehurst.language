package net.akehurst.language.parse.graph;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.sppt.IParseTreeVisitor;
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISPLeaf;
import net.akehurst.language.core.sppt.ISPNode;
import net.akehurst.language.core.sppt.ISPNodeIdentity;
import net.akehurst.language.core.sppt.SPNodeIdentity;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.sppf.Leaf;

public class GraphNodeLeaf implements ICompleteNode, ISPLeaf {

	private final Leaf leaf;
	private final int finalMatchedTextLength;

	private ISPBranch parent;

	public GraphNodeLeaf(final ParseGraph graph, final Leaf leaf) {
		this.leaf = leaf;
		this.finalMatchedTextLength = leaf.getMatchedTextLength();

	}

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
	public int getNextInputPosition() {
		return this.leaf.getNextInputPosition();
	}

	@Override
	public int getPriority() {
		return 0; // should never be called!;
	}

	@Override
	public boolean isEmptyLeaf() {
		return this.leaf.isEmptyLeaf();
	}

	@Override
	public Set<List<ISPNode>> getChildrenAlternatives() {
		return Collections.emptySet();
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
	public ISPBranch getParent() {
		return this.parent;
	}

	@Override
	public void setParent(final ISPBranch value) {
		this.parent = value;
	}

	@Override
	public String getNonSkipMatchedText() {
		return this.isSkip() ? "" : this.getMatchedText();
	}

	@Override
	public ISPNodeIdentity getIdentity() {
		// TODO Auto-generated method stub
		return new SPNodeIdentity(this.getRuntimeRuleNumber(), this.getStartPosition(), this.getMatchedTextLength());
	}

	@Override
	public boolean isSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public boolean isBranch() {
		return false;
	}

	@Override
	public ISPLeaf asLeaf() {
		return this;
	}

	@Override
	public ISPBranch asBranch() {
		return null;
	}

	@Override
	public boolean contains(final ISPNode other) {
		if (other instanceof ISPLeaf) {
			final ISPLeaf otherLeaf = (ISPLeaf) other;
			return Objects.equals(this.getIdentity(), otherLeaf.getIdentity());
		} else {
			return false;
		}
	}
	// @Override
	// public List<ISPPFBranch> findBranches(final String name) {
	// return Collections.emptyList();
	// }

	@Override
	public String toStringTree() {
		String r = "";
		r += this.getStartPosition() + ",";
		r += this.getNextInputPosition() + ",";
		r += "C";
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		r += "'" + this.getMatchedText() + "'";

		return r;
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
		r += this.getNextInputPosition() + ",";
		r += "C";
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		return r;
	}

}
