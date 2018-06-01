package net.akehurst.language.core.sppt;

/**
 *
 * The identity of a node in a Shared Packed Parse Forest is composed from:
 * <ul>
 * <li>a unique rule number,
 * <li>a starting position indicating the index of a position in the input text of the parse at which this node starts,
 * <li>the length (number of characters) of the input text that is matched by this node
 * </ul>
 *
 * If a grammar is ambiguous, a parse result may contain multiple nodes with the same identity but with different children. An SPPF combines these nodes (to
 * avoid duplication) but supports the alternative lists of children.
 *
 */
public interface SPNodeIdentity {

    /**
     * @return the number of the runtime rule used to create the node
     */
    public int getRuntimeRuleNumber();

    /**
     * @return the start position of the text matched by the node
     */
    public int getStartPosition();

    /**
     * @return the length of the text matched by the node
     */
    public int getMatchedTextLength();

}
