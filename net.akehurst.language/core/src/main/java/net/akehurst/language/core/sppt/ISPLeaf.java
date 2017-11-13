package net.akehurst.language.core.sppt;

/**
 * A leaf node has no children.
 */
public interface ISPLeaf extends ISPNode {

	/**
	 * Indicates if the leaf was constructed by matching a regular expression pattern or not.
	 *
	 * @return true if the leaf was created by matching a regular expression pattern, false if not.
	 */
	boolean isPattern();

}
