package net.akehurst.language.api.sppt;

/**
 * A Shared Packed Parse Forest is a collection of parse trees which share Nodes when possible. There is a Root Node. Each Node in a tree is either a Leaf or an
 * Branch. An Branch contains a Set of Lists of child Nodes. Each list of child nodes is an alternative possible list of children for the Branch
 *
 * A traditional ParseTree would be a special case (sub type) of an SharedPackedParseForest that contains only one tree.
 */
public interface SharedPackedParseTree extends SharedPackedParseTreeVisitable {

	/**
	 * The root of the tree
	 */
	SPPTNode getRoot();

	/**
	 * Determines if there is an equivalent tree in this forest for every tree in the other forest.
	 *
	 * @param other
	 *            tree
	 * @return true if this tree contains the other
	 */
	boolean contains(SharedPackedParseTree other);

	/**
	 * @return the original input text
	 */
	String asString();

	/**
	 * 
	 * @return count of the trees contained
	 */
	int countTrees();

	/**
	 * @return a string representation of all contained parse trees
	 */
	String toStringAll();
}
