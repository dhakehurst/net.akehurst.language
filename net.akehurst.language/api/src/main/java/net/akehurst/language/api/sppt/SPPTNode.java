package net.akehurst.language.api.sppt;

public interface SPPTNode extends SharedPackedParseTreeVisitable {

    /**
     *
     * @return the identity of this node
     */
    SPNodeIdentity getIdentity();

    /**
     *
     * @return the name of the runtime rule that caused this node to be constructed
     */
    String getName();

    /**
     *
     * @return the rule number from the runtime grammar that caused this node to be constructed
     */
    int getRuntimeRuleNumber();

    /**
     *
     * @return the index position of the input text at which this node starts its match
     */
    int getStartPosition();

    /**
     *
     * @return the length of the text (in characters) matched by this node
     */
    int getMatchedTextLength();

    /**
     * @return all text matched by this node
     */
    String getMatchedText();

    /**
     *
     * @return all text matched by this node excluding text that was matched by skip rules.
     */
    String getNonSkipMatchedText();

    /**
     *
     * @return the number of lines (end of line markers) covered by the text that this node matches
     */
    int getNumberOfLines();

    /**
     * an Empty Leaf is constructed by a parse by specifically matching nothing, caused by:
     * <ul>
     * <li>a rule with no items (for example 'rule = ;')
     * <li>an optional item (for example 'rule = item?;')
     * <li>a list of items with 0 multiplicity (for example 'rule = item*;')
     *
     * @return true if this node is an EmptyLeaf
     */
    boolean isEmptyLeaf();

    // /**
    // *
    // * @return true if this node isEmptyLeaf or all its children return true for containsOnlyEmptyLeafs
    // */
    // boolean containsOnlyEmptyLeafs();

    /**
     *
     * @return true if this node is a Leaf
     */
    boolean isLeaf();

    /**
     *
     * @return true if this node is a branch
     */
    boolean isBranch();

    /**
     * a grammar can define some rules as 'skip' rules, for example a rule to match whitespace is commonly a skip rule.
     *
     * @return true if this node was constructed from a skip rule
     */
    boolean isSkip();

    /**
     *
     * @return this node cast to an ILeaf (or null if the node is not a leaf)
     */
    SPPTLeaf asLeaf();

    /**
     *
     * @return this node cast to an ISPPFBranch (or null if the node is not a branch)
     */
    SPPTBranch asBranch();

    /**
     * A parent might be null if the construction of the node has not set it (it is not required)
     *
     * @return the parent branch of this node.
     */
    SPPTBranch getParent();

    void setParent(final SPPTBranch value);

    /**
     * <ul>
     * <li>this leaf contains another leaf if they are equal
     * <li>this branch contains another branch if all the children alternatives of the other are contained in this
     *
     * @param other
     * @return true if this node 'contains' the other node
     */
    boolean contains(SPPTNode other);

}
