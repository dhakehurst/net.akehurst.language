package net.akehurst.language.parse.graph;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.core.parser.IParseTreeVisitable;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IGraphNode extends IParseTreeVisitable {

	RuntimeRule getRuntimeRule();

	int getPriority();

	int getStartPosition();

	int getEndPosition();

	int getNextItemIndex();

	int getNextInputPosition();

	int getMatchedTextLength();

	int getGrowingEndPosition();

	// TODO: not sure this is useful any where
	int getHeight();

	boolean getIsLeaf();

	/**
	 * isStacked then true else canGrowWidth
	 *
	 * @return
	 */
	boolean getCanGrow();

	boolean getIsSkip();

	boolean getIsComplete();

	/**
	 * isComplete && isStacked
	 *
	 * @return
	 */
	boolean getCanGraftBack();

	boolean getCanGrowWidth();

	boolean getCanGrowWidthWithSkip();

	boolean getIsStacked();

	boolean hasNextExpectedItem();

	List<RuntimeRule> getNextExpectedItem();

	List<RuntimeRule> getNextExpectedTerminals();

	RuntimeRule getExpectedItemAt(int atPosition);

	boolean getExpectsItemAt(RuntimeRule item, int atPosition);

	Set<IGraphNode> getAlreadyGrownInto();

	Set<IGraphNode> getPossibleParent();

	static class ChildrenOption {
		public int matchedLength;
		public List<IGraphNode> nodes;
	}

	List<ChildrenOption> getChildrenOption();

	/**
	 * push this node onto the stack of next, where next would fit into this 'atPosition'
	 *
	 * @param next
	 */
	void pushToStackOf(IGraphNode next, int atPosition);

	/**
	 * follow getNext and getPossibleParent for find the current heads
	 *
	 * @return
	 */
	Set<IGraphNode> getHeads(Set<IGraphNode> visited);

	/**
	 * reverse of getPrevious
	 *
	 * @return
	 */
	Set<IGraphNode> getNext();

	void addNext(IGraphNode value);

	// int[] getStackHash();

	void addNextGrowingChild(IGraphNode nextChild, int nextItemIndex);

	/**
	 * return list of things that are stacked previous to this one
	 *
	 * @return
	 */
	Set<PreviousInfo> getPrevious();

	void addPrevious(IGraphNode prev, int atPosition);

	public static final class PreviousInfo {
		public PreviousInfo(final IGraphNode node, final int atPosition) {
			this.node = node;
			this.atPosition = atPosition;
			this.hashCode_cache = Objects.hash(node, atPosition);
		}

		public IGraphNode node;
		public int atPosition;

		int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(final Object arg) {
			if (!(arg instanceof PreviousInfo)) {
				return false;
			}
			final PreviousInfo other = (PreviousInfo) arg;
			return this.atPosition == other.atPosition && this.node == other.node;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.atPosition)).concat("-").concat(this.node.toString()).concat(")");
		}
	}

	// IGraphNode addNextChild(IGraphNode gn);
	//
	// IGraphNode addSkipChild(IGraphNode gn);
	//
	// IGraphNode replace(IGraphNode newNode);

	// IGraphNode duplicateWithNextChild(IGraphNode nextChild);
	//
	// IGraphNode duplicateWithNextSkipChild(IGraphNode nextChild);
	//
	// IGraphNode duplicateWithOtherStack(int priority, Set<PreviousInfo> previous);
	//
	// IGraphNode reuseWithOtherStack(Set<PreviousInfo> previous);

}
