package net.akehurst.language.parse.graph;

import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTreeVisitable;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;


public interface IGraphNode extends IParseTreeVisitable {


	RuntimeRule getRuntimeRule();

	int getPriority();

	int getStartPosition();

	int getNextItemIndex();
	
	int getNextInputPosition();

	int getMatchedTextLength();
	
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

//	IGraphNode getParent();
	List<INode> getChildren();

	
	/**
	 * push this node onto the stack of next, where net would fit into this 'atPosition'
	 * 
	 * @param next
	 * @return next with this node as its stack
	 */
	IGraphNode pushToStackOf(IGraphNode next, int atPosition);

	int getStackHash();
	
	/**
	 * return list of things that are stacked previous to this one
	 * 
	 * @return
	 */
	List<PreviousInfo> getPossibleParent();
	void addPrevious(IGraphNode prev, int atPosition);

	public static final class PreviousInfo {
		public PreviousInfo(IGraphNode node, int atPosition) {
			this.node = node;
			this.atPosition = atPosition;
//			this.hashCode_cache = Objects.hash(node.getRuntimeRule().getRuleNumber(),atPosition);
		}
		public IGraphNode node;
		public int atPosition;
		
//		int hashCode_cache;
//		@Override
//		public int hashCode() {
//			return this.hashCode_cache;
//		}
//		@Override
//		public boolean equals(Object arg) {
//			if (!(arg instanceof PreviousInfo)) {
//				return false;
//			}
//			PreviousInfo other = (PreviousInfo)arg;
//			return this.atPosition == other.atPosition && this.node==other.node;
//		}
		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.atPosition)).concat(",").concat(this.node.toString()).concat(")");
		}
	}

//	IGraphNode addNextChild(IGraphNode gn);
//
//	IGraphNode addSkipChild(IGraphNode gn);
//
//	IGraphNode replace(IGraphNode newNode);

	IGraphNode duplicateWithNextChild(IGraphNode nextChild);
	IGraphNode duplicateWithNextSkipChild(IGraphNode nextChild);

	IGraphNode duplicateWithOtherStack(int priority, List<PreviousInfo> previous);





}
