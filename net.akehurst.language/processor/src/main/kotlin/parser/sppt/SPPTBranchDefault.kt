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

package net.akehurst.language.parser.sppt

import net.akehurst.language.api.sppt.*
import net.akehurst.language.ogl.runtime.structure.RuntimeRule

class SPPTBranchDefault(runtimeRule: RuntimeRule, override val children: List<SPPTNode>) : SPPTNodeDefault(runtimeRule, children.first().startPosition, children.sumBy { it.matchedTextLength }), SPPTBranch {


    // --- SPPTBranch ---

    override val childrenAlternatives: MutableSet<List<SPPTNode>> = mutableSetOf(children)

    override val nonSkipChildren: List<SPPTNode> by lazy {
        this.children.filter { !it.isSkip }
    }

    override val branchNonSkipChildren: List<SPPTBranch> by lazy {
        this.children.filter { it.isBranch && !it.isSkip }.filterIsInstance<SPPTBranch>()
    }

    override fun nonSkipChild(index: Int): SPPTNode {
        return this.nonSkipChildren[index]
    }


    override fun branchChild(index: Int): SPPTBranch {
        return this.branchNonSkipChildren[index]
    }


    // --- SPPTNode ---

    override val matchedText: String by lazy {
        this.children.joinToString { it.matchedText }
    }

    override val nonSkipMatchedText: String = if (isSkip) "" else this.matchedText

    override fun contains(other: SPPTNode): Boolean {
        return this.identity == other.identity
    }

    override val isEmptyLeaf: Boolean = false

    override val isLeaf: Boolean = false

    override val isBranch: Boolean = true

    override val asLeaf: SPPTLeaf get() { throw SPPTException("Not a Leaf", null) }

    override val asBranch: SPPTBranch = this

    override fun <T, A> accept(visitor: SharedPackedParseTreeVisitor<T, A>, arg: A): T {
        return visitor.visit(this, arg)
    }
}