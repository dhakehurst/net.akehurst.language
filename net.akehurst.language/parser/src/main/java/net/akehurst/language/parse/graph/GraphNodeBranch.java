package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.akehurst.language.core.sppf.ILeaf;
import net.akehurst.language.core.sppf.IParseTreeVisitor;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.ISPPFNodeIdentity;
import net.akehurst.language.core.sppf.SPPFNodeIdentity;
import net.akehurst.language.grammar.parser.ParseTreeToSingleLineTreeString;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeBranch implements ICompleteNode, ISPPFBranch {

	private final RuntimeRule runtimeRule;
	private final int priority;
	private final int startPosition;
	private final int nextInputPosition;
	private final Set<List<ISPPFNode>> childrenAlternatives;
	private ISPPFBranch parent;

	public GraphNodeBranch(final ParseGraph graph, final RuntimeRule runtimeRule, final int priority, final int startPosition, final int nextInputPosition) {
		this.runtimeRule = runtimeRule;
		this.priority = priority;
		this.startPosition = startPosition;
		this.nextInputPosition = nextInputPosition;
		this.childrenAlternatives = new HashSet<>();
	}

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
	public int getNextInputPosition() {
		return this.nextInputPosition;
	}

	@Override
	public int getMatchedTextLength() {
		return this.getNextInputPosition() - this.getStartPosition();
	}

	@Override
	public boolean getIsLeaf() {
		return false;
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean isEmptyLeaf() {
		return false;
	}

	// --- IBranch ---
	// @Override
	// public boolean getIsEmpty() {
	// if (this.getNonSkipChildren().isEmpty()) {
	// return true;
	// } else {
	// if (this.getNonSkipChildren().get(0).isEmptyLeaf()) {
	// return true;
	// } else {
	// return false;
	// }
	// }
	// }

	@Override
	public List<ISPPFNode> getChildren() {
		if (this.getChildrenAlternatives().isEmpty()) {
			return Collections.emptyList();
		} else {
			return this.getChildrenAlternatives().iterator().next();
		}
	}

	public IGraphNode getChildAt(final int index) {
		return (IGraphNode) this.getChildren().get(index);
	}

	@Override
	public ISPPFNode getChild(final int index) {
		final List<ISPPFNode> children = this.getChildren();

		// get first non skip child
		int child = 0;
		ISPPFNode n = children.get(child);
		while (n.isSkip() && child < children.size() - 1) {
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
			while (n.isSkip()) {
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
	public ISPPFBranch getBranchChild(final int i) {
		final ISPPFNode n = this.getChild(i);
		return (ISPPFBranch) n;
	}

	@Override
	public List<ISPPFBranch> getBranchNonSkipChildren() {
		final List<ISPPFBranch> res = this.getNonSkipChildren().stream().filter(ISPPFBranch.class::isInstance).map(ISPPFBranch.class::cast)
				.collect(Collectors.toList());
		return res;
	}

	List<ISPPFNode> nonSkipChildren_cache;

	// --- IBranch ---
	@Override
	public List<ISPPFNode> getNonSkipChildren() {
		if (null == this.nonSkipChildren_cache) {
			this.nonSkipChildren_cache = new ArrayList<>();
			for (final ISPPFNode n : this.getChildren()) {
				if (n.isSkip()) {

				} else {
					this.nonSkipChildren_cache.add(n);
				}
			}
		}
		return this.nonSkipChildren_cache;
	}

	// --- INode ---
	@Override
	public ISPPFNodeIdentity getIdentity() {
		// TODO cache this
		return new SPPFNodeIdentity(this.getRuntimeRuleNumber(), this.getStartPosition(), this.getMatchedTextLength());
	}

	@Override
	public ISPPFBranch getParent() {
		return this.parent;
	}

	@Override
	public void setParent(final ISPPFBranch value) {
		this.parent = value;
	}

	@Override
	public String getName() {
		return this.getRuntimeRule().getName();
	}

	@Override
	public String getMatchedText() {
		String str = "";
		for (final ISPPFNode n : this.getChildren()) {
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

	@Override
	public String getNonSkipMatchedText() {
		String str = "";
		for (final ISPPFNode n : this.getNonSkipChildren()) {
			str += n.getNonSkipMatchedText();
		}
		return str;
	}

	@Override
	public Set<List<ISPPFNode>> getChildrenAlternatives() {
		return this.childrenAlternatives;
	}

	@Override
	public boolean isSkip() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBranch() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ILeaf asLeaf() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISPPFBranch asBranch() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(final ISPPFNode other) {
		if (other instanceof ISPPFBranch) {
			final ISPPFBranch otherBranch = (ISPPFBranch) other;

			if (this.getIdentity().equals(other.getIdentity())) {
				// for each alternative list of other children, check there is a matching list
				// of children in this alternative children
				boolean allOthersAreContained = true; // if no other children alternatives contain is a match
				for (final List<ISPPFNode> otherChildren : otherBranch.getChildrenAlternatives()) {
					// for each of this alternative children, find one that 'contains' otherChildren
					boolean foundContainMatch = false;
					for (final List<ISPPFNode> thisChildren : this.getChildrenAlternatives()) {
						if (thisChildren.size() == otherChildren.size()) {
							// for each pair of nodes, one from each of otherChildren thisChildren
							// check thisChildrenNode contains otherChildrenNode
							boolean thisMatch = true;
							for (int i = 0; i < thisChildren.size(); ++i) {
								final ISPPFNode thisChildrenNode = thisChildren.get(i);
								final ISPPFNode otherChildrenNode = otherChildren.get(i);
								thisMatch &= thisChildrenNode.contains(otherChildrenNode);
							}
							if (thisMatch) {
								foundContainMatch = true;
								break;
							} else {
								// if thisChildren alternative doesn't contain, try the next one
								continue;
							}
						} else {
							// if sizes don't match check next in set of this alternative children
							continue;
						}
					}
					allOthersAreContained &= foundContainMatch;
				}
				return allOthersAreContained;
			} else {
				// if identities don't match
				return false;
			}

		} else {
			// if other is not a branch
			return false;
		}
	}

	// @Override
	// public List<ISPPFBranch> findBranches(final String name) {
	// final List<ISPPFBranch> result = new ArrayList<>();
	// if (Objects.equals(this.getName(), name)) {
	// result.add(this);
	// } else {
	// for (final ISPPFNode child : this.getChildren()) {
	// result.addAll(child.findBranches(name));
	// }
	// }
	// return result;
	// }

	@Override
	public String toStringTree() {
		String r = "";
		r += this.getStartPosition() + ",";
		r += this.getNextInputPosition();
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";

		r += "{";
		for (final ISPPFNode c : this.getChildren()) {
			c.accept(new ParseTreeToSingleLineTreeString(), null);
		}
		r += "}";

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
		r += this.getNextInputPosition();
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		return r;
	}
}
