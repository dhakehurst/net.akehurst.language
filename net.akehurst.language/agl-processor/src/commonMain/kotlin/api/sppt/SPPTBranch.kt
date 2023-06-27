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

package net.akehurst.language.api.sppt;

/**
 *
 * A branch in a SharedPackedParseTree
 *
 */
interface SPPTBranch : SPPTNode {

    /**
     * the set of alternative children for this branch
     */
    val childrenAlternatives: Map<Int, List<SPPTNode>>

    // --- convienience methods ---
    /**
     * the first one of the children alternatives of this branch.
     */
    val children: List<SPPTNode>

    /**
     * the first one of the children alternatives of this branch with all skip nodes removed.
     */
    val nonSkipChildren: List<SPPTNode>

    /**
     * @param index of required child
     * @return index'th non skip child (in first one of the children alternatives)
     */
    fun nonSkipChild(index: Int): SPPTNode

    /**
     * @param index
     * @return the index'th non skip child (in first one of the children alternatives) of this Branch but assumes the child is also a Branch and casts the result.
     */
    fun branchChild(index: Int): SPPTBranch

    /**
     * Filters out any children that are skip nodes or not branches
     *
     * all children that are branches and non skip (in first one of the children alternatives)
     */
    val branchNonSkipChildren: List<SPPTBranch>
}
