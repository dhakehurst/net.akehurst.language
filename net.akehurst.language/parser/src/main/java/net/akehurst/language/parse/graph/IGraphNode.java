package net.akehurst.language.parse.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.ogl.semanticStructure.Grammar;


public interface IGraphNode {


	RuntimeRule getRuntimeRule();

	int getPriority();

	int getStartPosition();


	int getNextItemIndex();
	
//	int getEndPosition();
	
	int getNextInputPosition();

	int getMatchedTextLength();
	
	boolean getIsEmpty();

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

	boolean getIsStacked();

	boolean hasNextExpectedItem();

	RuntimeRule getNextExpectedItem();

	RuntimeRule getExpectedItemAt(int atPosition);
	
//	Map<ParentsIndex, IGraphNode> getParents();
//	void addParent(GraphNodeBranch graphNodeBranch, int nextItemIndex);
	
	IGraphNode getParent();
	List<IGraphNode> getChildren();

	
	/**
	 * push this node onto the stack of next, where net would fit into this 'atPosition'
	 * 
	 * @param next
	 * @return next with this node as its stack
	 */
	IGraphNode pushToStackOf(IGraphNode next, int atPosition);

	/**
	 * return list of things that are stacked previous to this one
	 * 
	 * @return
	 */
	List<PreviousInfo> getPrevious();
	static final class PreviousInfo {
		public PreviousInfo(IGraphNode node, int atPosition) {
			this.node = node;
			this.atPosition = atPosition;
			this.hashCode_cache = Objects.hash(node.getRuntimeRule().getRuleNumber(),atPosition);
		}
		public IGraphNode node;
		public int atPosition;
		
		int hashCode_cache;
		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}
		@Override
		public boolean equals(Object arg) {
			if (!(arg instanceof PreviousInfo)) {
				return false;
			}
			PreviousInfo other = (PreviousInfo)arg;
			return this.atPosition == other.atPosition && this.node==other.node;
		}
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


}
