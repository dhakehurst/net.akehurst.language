package net.akehurst.language.core.sppf;

/**
 * A leaf node has no children.
 */
public interface ILeaf extends ISPPFNode {

	/**
	 * Indicates if the leaf was constructed by matching a regular expression pattern or not.
	 *
	 * @return true if the leaf was created by matching a regular expression pattern, false if not.
	 */
	boolean isPattern();

}
