package net.akehurst.language.parse.graph;

import java.util.List;
import java.util.Set;

import net.akehurst.language.core.parser.IParseTreeVisitable;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface ICompleteNode extends IParseTreeVisitable {

	RuntimeRule getRuntimeRule();

	int getRuntimeRuleNumber();

	int getStartPosition();

	int getNextInputPosition();

	int getMatchedTextLength();

	boolean getIsLeaf();

	boolean isEmptyLeaf();

	boolean getIsSkip();

	// static class ChildrenOption {
	// public int matchedLength;
	// public List<ICompleteNode> nodes;
	//
	// @Override
	// public String toString() {
	// return this.nodes.toString();
	// }
	//
	// @Override
	// public int hashCode() {
	// return this.nodes.hashCode();
	// }
	//
	// @Override
	// public boolean equals(final Object obj) {
	// if (obj instanceof ChildrenOption) {
	// final ChildrenOption other = (ChildrenOption) obj;
	// return Objects.equals(this.nodes, other.nodes);
	// } else {
	// return false;
	// }
	// }
	// }

	// Set<ICompleteNode.ChildrenOption> getChildrenOption();

	String toStringTree();

	Set<List<ISPPFNode>> getChildrenAlternatives();

}
