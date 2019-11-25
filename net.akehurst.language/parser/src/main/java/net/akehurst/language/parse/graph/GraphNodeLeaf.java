package net.akehurst.language.parse.graph;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPNodeIdentity;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.sppf.Leaf;
import net.akehurst.language.parser.sppf.SPPTNodeIdentitySimple;

public class GraphNodeLeaf implements ICompleteNode, SPPTLeaf {

	private final Leaf leaf;
	private final int finalMatchedTextLength;

	private SPPTBranch parent;

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
	public boolean isEmptyRuleMatch() {
		return this.isEmptyLeaf();
	}

	@Override
	public Set<FixedList<SPPTNode>> getChildrenAlternatives() {
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
	public SPPTBranch getParent() {
		return this.parent;
	}

	@Override
	public void setParent(final SPPTBranch value) {
		this.parent = value;
	}

	@Override
	public String getNonSkipMatchedText() {
		return this.isSkip() ? "" : this.getMatchedText();
	}

	@Override
	public SPNodeIdentity getIdentity() {
		// TODO Auto-generated method stub
		return new SPPTNodeIdentitySimple(this.getRuntimeRuleNumber(), this.getStartPosition(), this.getMatchedTextLength());
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
	public SPPTLeaf asLeaf() {
		return this;
	}

	@Override
	public SPPTBranch asBranch() {
		return null;
	}

	@Override
	public boolean contains(final SPPTNode other) {
		if (other instanceof SPPTLeaf) {
			final SPPTLeaf otherLeaf = (SPPTLeaf) other;
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
	public <T, A, E extends Throwable> T accept(final SharedPackedParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
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
