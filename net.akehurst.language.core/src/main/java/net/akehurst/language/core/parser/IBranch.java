package net.akehurst.language.core.parser;

import java.util.List;

public interface IBranch extends INode, IParseTreeVisitable {

	List<INode> getChildren();
	/**
	 * this creates a new branch it does not modify current one
	 *  
	 * @param child
	 * @return
	 */
	IBranch addChild(INode child);
	
}
