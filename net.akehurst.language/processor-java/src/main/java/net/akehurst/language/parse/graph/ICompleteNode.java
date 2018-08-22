package net.akehurst.language.parse.graph;

import java.util.Set;

import net.akehurst.language.api.sppt.FixedList;
import net.akehurst.language.api.sppt.SPPTNode;
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitable;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface ICompleteNode extends SharedPackedParseTreeVisitable {

	RuntimeRule getRuntimeRule();

	int getRuntimeRuleNumber();

	int getStartPosition();

	int getNextInputPosition();

	int getPriority();

	int getMatchedTextLength();

	boolean isLeaf();

	boolean isEmptyLeaf();

	boolean isSkip();

	boolean isEmptyRuleMatch();

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

	Set<FixedList<SPPTNode>> getChildrenAlternatives();

}