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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsList
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.*

//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties
/*internal */ abstract class SPPTNodeFromInputAbstract internal constructor(
    internal val input: InputFromString,
    internal val runtimeRule: RuntimeRule,
    override val option: Int,
    final override val startPosition: Int,
    override val nextInputPosition: Int,
    override val priority: Int               //TODO: no longer needed I think       //not needed as part of the SPPTNode, but needed for the parsing algorithm
) : SPPTNode {

    var embeddedIn: String? = null

    override val identity: SPPTNodeIdentity by lazy {
        SPPTNodeIdentityDefault(
            this.runtimeRule.runtimeRuleSetNumber,
            this.runtimeRule.ruleNumber,
            this.startPosition
        )
    }

    override val runtimeRuleSetNumber: Int get() = this.identity.runtimeRuleSetNumber

    override val runtimeRuleNumber: Int get() = this.identity.runtimeRuleNumber

    override val name: String get() = this.runtimeRule.tag

    override val matchedTextLength: Int get() = this.nextInputPosition - this.startPosition

    override val isSkip: Boolean get() = runtimeRule.isSkip

    override val isList: Boolean get() = this.runtimeRule.rhs is RuntimeRuleRhsList

    override val isOptional: Boolean get() = this.runtimeRule.rhs is RuntimeRuleRhsList
            && (this.runtimeRule.rhs as RuntimeRuleRhsList).min==0
            && (this.runtimeRule.rhs as RuntimeRuleRhsList).max==1

    // match empty if start and next-input positions are the same
    override val isEmptyMatch: Boolean get() = this.startPosition == this.nextInputPosition

    override val numberOfLines: Int by lazy { //TODO: maybe not use lazy
        Regex("\n").findAll(this.matchedText, 0).count()
    }

    abstract override val asLeaf: SPPTLeaf

    abstract override val asBranch: SPPTBranch

    override var parent: SPPTBranch? = null

    private var _tree: SharedPackedParseTree? = null
    override var tree: SharedPackedParseTree?
        get() = if (null != this._tree || null == this.parent) {
            this._tree
        } else {
            var br = this.parent
            while (null != br?.parent) {
                br = br.parent
            }
            br?.tree
        }
        set(value) {
            _tree = value
        }

    override val location: InputLocation get() = input.locationFor(startPosition, matchedTextLength)

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean

    fun toStringIndented(indentIncrement: String): String {
        val visitor = ToStringVisitor("\n", indentIncrement)
        val all: Set<String> = visitor.visitNode(this, "  ")
        val total = all.size
        val sep = "\n"
        var cur = 0
        var res = ""
        for (pt in all) {
            cur++
            res += "Tree ${cur} of ${total}\n"
            res += pt
            res += "\n"
        }
        return all.joinToString(sep)
    }
}