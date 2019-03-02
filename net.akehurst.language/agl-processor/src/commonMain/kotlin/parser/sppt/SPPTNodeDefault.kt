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

import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SPPTNodeIdentity
import net.akehurst.language.agl.runtime.structure.RuntimeRule

abstract class SPPTNodeDefault(
        val runtimeRule: RuntimeRule,
        override val startPosition: Int,
        override val nextInputPosition: Int,
        val priority: Int                      //not needed as part of the SPPTNode, but needed for the parsing algorithm
) : SPPTNode {

    override val identity: SPPTNodeIdentity = SPPTNodeIdentityDefault(
            this.runtimeRule.number,
            this.startPosition,
            this.nextInputPosition - this.startPosition
    )

    override val name: String = this.runtimeRule.name

    override val runtimeRuleNumber: Int get() { return this.identity.runtimeRuleNumber }

    override val matchedTextLength: Int get() { return this.identity.matchedTextLength }

    override val isSkip: Boolean = runtimeRule.isSkip

    val isEmptyMatch: Boolean get() {
        // match empty if start and next-input positions are the same
        return this.startPosition == this.nextInputPosition
    }

    override val numberOfLines: Int by lazy {
        Regex("\n").findAll(this.matchedText, 0).count()
    }

    abstract override val asLeaf: SPPTLeaf

    abstract override val asBranch: SPPTBranch

    override var parent: SPPTBranch? = null

    abstract override fun hashCode() : Int

    abstract override fun equals(other: Any?): Boolean
}