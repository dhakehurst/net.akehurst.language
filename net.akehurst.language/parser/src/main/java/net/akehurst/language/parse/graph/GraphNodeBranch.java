package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeBranch implements ICompleteNode, IBranch {

	public GraphNodeBranch(final ParseGraph graph, final RuntimeRule runtimeRule, final int priority, final int startPosition, final int endPosition) {
		this.runtimeRule = runtimeRule;
		this.priority = priority;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.childrenOption = new ArrayList<>();
	}

	private final RuntimeRule runtimeRule;
	private final int priority;
	private final int startPosition;
	private final int endPosition;
	private final List<ICompleteNode.ChildrenOption> childrenOption;
	private IBranch parent;

	// --- ICompleteNode ---
	@Override
	public RuntimeRule getRuntimeRule() {
		// TODO Auto-generated method stub
		return this.runtimeRule;
	}

	@Override
	public int getRuntimeRuleNumber() {
		return this.getRuntimeRule().getRuleNumber();
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	@Override
	public int getEndPosition() {
		return this.endPosition;
	}

	@Override
	public int getMatchedTextLength() {
		return this.getEndPosition() - this.getStartPosition();
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean getIsEmptyLeaf() {
		return false;
	}

	@Override
	public List<ICompleteNode.ChildrenOption> getChildrenOption() {
		return this.childrenOption;
	}

	// --- IBranch ---
	@Override
	public boolean getIsEmpty() {
		if (this.getNonSkipChildren().isEmpty()) {
			return true;
		} else {
			if (this.getNonSkipChildren().get(0).getIsEmptyLeaf()) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public List<INode> getChildren() {
		if (this.getChildrenOption().isEmpty()) {
			return Collections.emptyList();
		} else {
			final ICompleteNode.ChildrenOption opt = this.getChildrenOption().get(0);
			return (List<INode>) (List<?>) opt.nodes;
		}
	}

	public IGraphNode getChildAt(final int index) {
		return (IGraphNode) this.getChildren().get(index);
	}

	@Override
	public INode getChild(final int index) {
		final List<INode> children = this.getChildren();

		// get first non skip child
		int child = 0;
		INode n = children.get(child);
		while (n.getIsSkip() && child < children.size() - 1) {
			++child;
			n = children.get(child);
		}
		if (child >= children.size()) {
			return null;
		}
		int count = 0;

		while (count < index && child < children.size() - 1) {
			++child;
			n = children.get(child);
			while (n.getIsSkip()) {
				++child;
				n = children.get(child);
			}
			++count;
		}

		if (child < children.size()) {
			return n;
		} else {
			return null;
		}
	}

	@Override
	public IBranch getBranchChild(final int i) {
		final INode n = this.getChild(i);
		return (IBranch) n;
	}

	@Override
	public List<IBranch> getBranchNonSkipChildren() {
		final List<IBranch> res = this.getNonSkipChildren().stream().filter(IBranch.class::isInstance).map(IBranch.class::cast).collect(Collectors.toList());
		return res;
	}

	List<INode> nonSkipChildren_cache;

	// --- IBranch ---
	@Override
	public List<INode> getNonSkipChildren() {
		if (null == this.nonSkipChildren_cache) {
			this.nonSkipChildren_cache = new ArrayList<>();
			for (final INode n : this.getChildren()) {
				if (n.getIsSkip()) {

				} else {
					this.nonSkipChildren_cache.add(n);
				}
			}
		}
		return this.nonSkipChildren_cache;
	}

	// --- INode ---
	@Override
	public IBranch getParent() {
		return this.parent;
	}

	@Override
	public void setParent(final IBranch value) {
		this.parent = value;
	}

	@Override
	public String getName() {
		return this.getRuntimeRule().getName();
	}

	@Override
	public String getMatchedText() {
		String str = "";
		for (final INode n : this.getChildren()) {
			str += n.getMatchedText();
		}
		return str;
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
		r += this.getEndPosition();
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		return r;
	}
}
