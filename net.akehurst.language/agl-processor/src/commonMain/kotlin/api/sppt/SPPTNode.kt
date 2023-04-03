/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.api.sppt

import net.akehurst.language.api.parser.InputLocation

/**
 * A leaf node has no children.
 */
interface SPPTNode {

    /**
     * the identity of this node
     */
    val identity: SPPTNodeIdentity

    /**
     *
     * the name of the runtime rule that caused this node to be constructed
     */
    val name: String

    /**
     *  the runtime-rule-set number from the runtime grammar that caused this node to be constructed
     */
    val runtimeRuleSetNumber: Int

    /**
     *  the runtime-rule number from the runtime grammar that caused this node to be constructed
     */
    val runtimeRuleNumber: Int

    val option: Int

    val location: InputLocation

    val lastLeaf: SPPTLeaf

    /**
     *
     *  the index position of the input text at which this node starts its match, derived from identity
     */
    val startPosition: Int

    /**
     *
     * the length of the text (in characters) matched by this node, derived from identity
     */
    val matchedTextLength: Int

    /**
     * startPosition + matchedTextLength
     */
    val nextInputPosition: Int

    /**
     * the priority of this node according to the grammar (0 if no priority defined by parent rule)
     */
    val priority: Int

    /**
     *  all text matched by this node
     */
    val matchedText: String

    /**
     *
     *  all text matched by this node excluding text that was matched by skip rules.
     */
    val nonSkipMatchedText: String

    /**
     *
     *  the number of lines (end of line markers) covered by the text that this node matches
     */
    val numberOfLines: Int

    /**
     * an Empty Leaf is constructed by a parse by specifically matching nothing, caused by:
     * <ul>
     * <li>a rule with no items (for example 'rule = ;')
     * <li>an optional item (for example 'rule = item?;')
     * <li>a list of items with 0 multiplicity (for example 'rule = item*;')
     *
     * true if this node is an EmptyLeaf
     */
    val isEmptyLeaf: Boolean

    /*
     * does this node match none of the input.
     * It might match empty leaves, but the matched text will be empty if this is true.
     */
    val isEmptyMatch: Boolean

    /**
     *
     *  true if this node is a Leaf
     */
    val isLeaf: Boolean

    /**
     * does the node represent an optional item (i.e. '?')
     */
    val isOptional:Boolean

    /**
     * does the node represent a List
     */
    val isList:Boolean

    /**
     * does the node represent and embedded tree from another parse
     */
    val isEmbedded:Boolean

    /**
     *
     *  true if this node is a branch
     */
    val isBranch: Boolean

    /**
     * a grammar can define some rules as 'skip' rules, for example a rule to match whitespace is commonly a skip rule.
     *
     *  true if this node was constructed from a skip rule
     */
    val isSkip: Boolean

    /**
     *
     *  this node cast to an ILeaf (or null if the node is not a leaf)
     */
    val asLeaf: SPPTLeaf

    /**
     *
     *  this node cast to an ISPPFBranch (or null if the node is not a branch)
     */
    val asBranch: SPPTBranch

    /**
     * The parent branch of this node.
     *
     * A parent might be null if the construction of the node has not set it (it is not required)
     *
     */
    var parent: SPPTBranch?

    /**
     * The tree which this node is part of
     *
     * Might be null if the construction of the node has not set it (it is not required)
     */
    var tree: SharedPackedParseTree?

    /**
     * <ul>
     * <li>this leaf contains another leaf if they are equal
     * <li>this branch contains another branch if all the children alternatives of the other are contained in this
     *
     * @param other
     * @return true if this node 'contains' the other node
     */
    fun contains(other: SPPTNode): Boolean

}
