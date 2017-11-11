package net.akehurst.language.core.sppf;

import net.akehurst.language.core.parser.IParseTreeVisitable;

/**
 * A Shared Packed Parse Forest is a collection of parse trees which share INodes when possible. There is a set of Root Nodes (though this commonly contains one
 * root as the result of a parse, because a parse always starts looking for a valid tree with a specific goal rule). Each INode in a tree is either an ILeaf or
 * an ISPBranch. An ISPBranch contains a Set of Lists of child INodes. Each list of child nodes is an alternative possible list of children for the ISPBranch
 *
 * An IParseTree is a special case (sub type) of an ISharedPackedParseForest that contains only one tree.
 */
public interface ISharedPackedParseTree extends IParseTreeVisitable {

	ISPPFNode getRoot();

	/**
	 * Determines if there is an equivalent tree in this forest for every tree in the other forest.
	 *
	 * @param other
	 * @return
	 */
	boolean contains(ISharedPackedParseTree other);

	String asString();
}
