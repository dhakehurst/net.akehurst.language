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

/*
//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties
/*internal */ abstract class SPPTNodeAbstract internal constructor(
        internal val runtimeRule: RuntimeRule,
        override val option: Int,
        override val location: InputLocation,
        override val nextInputPosition: Int,
        override val priority: Int                      //not needed as part of the SPPTNode, but needed for the parsing algorithm
) : SPPTNode {

    var embeddedIn : String? = null

    override val identity: SPPTNodeIdentity = SPPTNodeIdentityDefault(
            this.runtimeRule.number,
            this.startPosition//,
            //this.nextInputPosition - this.startPosition
    )

    override val name: String get() = this.runtimeRule.tag

    override val startPosition get() = location.position

    override val runtimeRuleNumber: Int get() { return this.identity.runtimeRuleNumber }

    override val matchedTextLength: Int get() { return this.nextInputPosition - this.startPosition}//this.identity.matchedTextLength }

    override val isSkip: Boolean get() = runtimeRule.isSkip

    override val isEmptyMatch: Boolean get() {
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

     fun toStringIndented(indentIncrement: String): String {
        val visitor = ToStringVisitor("\n", indentIncrement)
        val all: Set<String> = visitor.visitNode(this, ToStringVisitor.Indent("", true))
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

 */