package net.akehurst.language.parse.graph;

import java.util.List;

import net.akehurst.language.core.parser.IParseTreeVisitable;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface ICompleteNode extends IParseTreeVisitable {

	int getRuntimeRuleNumber();

	int getStartPosition();

	int getEndPosition();

	int getMatchedTextLength();

	static class ChildrenOption {
		public int matchedLength;
		public List<ICompleteNode> nodes;
	}

	List<ChildrenOption> getChildrenOption();

	boolean getIsSkip();

	RuntimeRule getRuntimeRule();

}
