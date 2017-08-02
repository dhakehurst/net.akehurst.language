package net.akehurst.language.parse.graph;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

abstract public class AbstractGraphNode implements IGraphNode {

	public AbstractGraphNode(final ParseGraph graph, final RuntimeRule runtimeRule, final int startPosition) {
		this.graph = graph;
		this.runtimeRule = runtimeRule;
		this.startPosition = startPosition;
		this.previous = new HashSet<>();
		this.possibleParent = new HashSet<>();
		this.next = new HashSet<>();
		// this.stackHash = 0;
		// this.stack = new int[0];
	}

	protected ParseGraph graph;
	protected RuntimeRule runtimeRule;
	protected int startPosition;

	private final Set<PreviousInfo> previous;
	private final Set<IGraphNode> next;
	private final Set<IGraphNode> possibleParent;

	IBranch parent;
	// int stackHash;
	// int[] stack;

	@Override
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	public int getRuntimeRuleNumber() {
		return this.getRuntimeRule().getRuleNumber();
	}

	public String getName() {
		return this.getRuntimeRule().getName();
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	// @Override
	// public int[] getStackHash() {
	// // //TODO: pre-cache this value when stack changes
	// // if (0==this.stackHash && !this.getPrevious().isEmpty()) {
	// // for(PreviousInfo prev: this.getPrevious()) {
	// // this.stackHash = Objects.hash(prev.node.getRuntimeRule().getRuleNumber(), prev.node.getStackHash());
	// // }
	// // }
	// return this.stack;// Hash;
	// }

	@Override
	public boolean getIsStacked() {
		return !this.getPrevious().isEmpty();
	}

	@Override
	public Set<IGraphNode> getNext() {
		return this.next;
	}

	@Override
	public void addNext(final IGraphNode value) {
		this.next.add(value);
	}

	@Override
	public Set<PreviousInfo> getPrevious() {
		return this.previous;
	}

	@Override
	public void addPrevious(final IGraphNode prev, final int atPosition) {
		final PreviousInfo info = new PreviousInfo(prev, atPosition);
		this.previous.add(info);
		prev.addNext(this);
		// this.stackHash = Objects.hash(this.stackHash, prev.getRuntimeRule().getRuleNumber(), prev.getStackHash());
		// TODO: performance could be better here if done different
		// IGraphNode n = prev;
		// while (null != n) {
		// final int[] newStack = Arrays.copyOf(this.stack, this.stack.length + 1);
		// newStack[this.stack.length] = n.getRuntimeRule().getRuleNumber();
		// this.stack = newStack;
		// if (n.getPossibleParent().isEmpty()) {
		// n = null;
		// } else {
		// n = n.getPossibleParent().get(0).node;
		// }
		// }
	}

	// @Override
	// public void pushToStackOf(final IGraphNode next, final int atPosition) {
	// // next.getPrevious().clear(); // FIXME: maybe good, maybe not!
	// next.addPrevious(this, atPosition);
	// this.graph.tryAddGrowable(next);
	// }

	@Override
	public Set<IGraphNode> getAlreadyGrownInto() {
		final Set<IGraphNode> res = new HashSet<>();
		res.add(this);
		for (final IGraphNode pp : this.getPossibleParent()) {
			final Set<IGraphNode> pph = pp.getAlreadyGrownInto();
			// res.add(pp);
			res.addAll(pph);
		}
		return res;
	}

	@Override
	public Set<IGraphNode> getPossibleParent() {
		return this.possibleParent;
	}

	@Override
	public Set<IGraphNode> getHeads(final Set<IGraphNode> visited) {
		final Set<IGraphNode> res = new HashSet<>();
		if (visited.contains(this)) {
			return res;
		} else {
			final Set<IGraphNode> visited2 = new HashSet<>(visited);
			visited2.add(this);
			for (final IGraphNode pp : this.getPossibleParent()) {
				final Set<IGraphNode> pph = pp.getHeads(visited2);
				if (pph.isEmpty()) {
					res.add(pp);
				} else {
					res.addAll(pph);
				}
			}
			for (final IGraphNode n : this.getNext()) {
				final Set<IGraphNode> ppn = n.getHeads(visited2);
				if (ppn.isEmpty()) {
					res.add(n);
				} else {
					res.addAll(ppn);
				}
			}
		}
		return res;
	}

	public IBranch getParent() {
		return this.parent;
	}

	public void setParent(final IBranch value) {
		this.parent = value;
	}

	public abstract String getMatchedText();

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
	public int hashCode() {
		// throw new RuntimeException("GraphNodes are not comparible");
		return Objects.hash(this.getRuntimeRule().getRuleNumber(), this.getStartPosition(), this.getMatchedTextLength());
	}

	@Override
	public boolean equals(final Object obj) {
		// throw new RuntimeException("GraphNodes are not comparible");
		if (obj instanceof IGraphNode) {
			final IGraphNode other = (IGraphNode) obj;
			return this.getRuntimeRule().getRuleNumber() == other.getRuntimeRule().getRuleNumber() && this.getStartPosition() == other.getStartPosition()
					&& this.getMatchedTextLength() == other.getMatchedTextLength();
		} else {
			return false;
		}
	}

	// @Override
	// public String toString() {
	// String prev = "";
	// if (this.getPrevious().isEmpty()) {
	// // nothing
	// } else if (this.getPrevious().size() == 1) {
	// prev = " --> " + this.getPrevious().iterator().next();
	// } else {
	// prev = " -*> " + this.getPrevious().iterator().next();
	// }
	// String r = "";
	// r += this.getStartPosition() + ",";
	// r += this.getGrowingMatchedTextLength() + ",";
	// r += -1 == this.getNextItemIndex() ? "C" : this.getNextItemIndex();
	// r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
	// r += prev;
	// return r;
	// // return this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + "," + this.getStartPosition() + ","
	// // + this.getMatchedTextLength() + "," + this.getNextItemIndex() + ")" + prev;
	// }
}
