package net.akehurst.language.parse.graph;

import java.util.List;

import net.akehurst.language.core.parser.IParseTreeVisitable;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface ICompleteNode extends IParseTreeVisitable {

	RuntimeRule getRuntimeRule();

	int getRuntimeRuleNumber();

	int getStartPosition();

	int getEndPosition();

	int getMatchedTextLength();

	boolean getIsSkip();

	static class ChildrenOption {
		public int matchedLength;
		public List<ICompleteNode> nodes;
	}

	List<ICompleteNode.ChildrenOption> getChildrenOption();

}
