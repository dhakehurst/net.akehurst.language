package net.akehurst.language.core.parser;

import java.util.List;

public interface IBranch extends INode, IParseTreeVisitable {

	List<INode> getChildren();
	IBranch addChild(INode child);
	
}
