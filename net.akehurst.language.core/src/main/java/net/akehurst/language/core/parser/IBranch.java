package net.akehurst.language.core.parser;

import java.util.List;

public interface IBranch extends INode, IParseTreeVisitable {
	/**
	 * 
	 * @return the children of this branch.
	 */
	List<INode> getChildren();
	
	/**
	 * this creates a new branch it does not modify current one
	 *  
	 * @param index of required child
	 * @return i'th non skip child.
	 */
	INode getChild(int i);
	
}
