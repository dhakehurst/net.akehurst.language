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

/**
 * A leaf node has no children.
 */
interface SPPTLeaf : SPPTNode {

    /**
     * Indicates if the leaf was constructed by matching a regular expression pattern or not.
     *
     * true if the leaf was created by matching a regular expression pattern, false if not.
     */
    val isPattern: Boolean

    /**
     * Indicates if the leaf was constructed by matching a literal or not.
     *
     * true if the leaf was created by matching a literal, false if not.
     */
    val isLiteral: Boolean

    /**
     * true if this node is a leaf node from an named leaf (i.e. literal or pattern marked as a 'leaf' rule)
     */
    val isExplicitelyNamed:Boolean

    /**
     * list of names of all the parent nodes leading to this leaf
     * ( currently  populated by TokensByLineVisitor - called by SharedPackedParseTree.tokensByLine )
     */
    val tagList: List<String>

    val eolPositions: List<Int>

    val metaTags: List<String>

    fun setTags(arg: List<String>)
}
