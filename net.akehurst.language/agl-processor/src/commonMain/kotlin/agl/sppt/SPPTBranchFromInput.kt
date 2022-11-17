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
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTException
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode

//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties
/*internal */ class SPPTBranchFromInput internal constructor(
        input:InputFromString,
        runtimeRule: RuntimeRule,
        option: Int,
        startPosition: Int,               // can't use children.first.startPosition, there may not be any children
        nextInputPosition: Int,          // don't use children.sumBy { it.matchedTextLength }, it requires unwanted iteration
        priority: Int
) : SPPTNodeFromInputAbstract(
        input, runtimeRule, option, startPosition, nextInputPosition, priority
), SPPTBranch {

    // --- SPPTBranch ---

    override val childrenAlternatives: MutableSet<List<SPPTNode>> = mutableSetOf() //TODO: need to be sure a List is ok here !

    override val children: List<SPPTNode> get() = this.childrenAlternatives.first()

    override val nonSkipChildren: List<SPPTNode> by lazy { //TODO: maybe not use lazy
        this.children.filter { !it.isSkip }
    }

    override val branchNonSkipChildren: List<SPPTBranch> by lazy { //TODO: maybe not use lazy
        this.children.filter { it.isBranch && !it.isSkip }.filterIsInstance<SPPTBranch>()
    }

    override fun nonSkipChild(index: Int): SPPTNode = this.nonSkipChildren[index]

    override fun branchChild(index: Int): SPPTBranch = this.branchNonSkipChildren[index]

    // --- SPPTNode ---
    override val matchedText: String by lazy { //TODO: maybe not use lazy
        this.children.joinToString(separator = "") { it.matchedText }
    }

    override val nonSkipMatchedText: String get() = this.nonSkipChildren.map { it.nonSkipMatchedText }.joinToString("")

    override fun contains(other: SPPTNode): Boolean {
        if (other is SPPTBranch) {
            if (this.identity == other.identity) {
                // for each alternative list of other children, check there is a matching list
                // of children in this alternative children
                var allOthersAreContained = true // if no other children alternatives contain is a match
                for (otherChildren in other.childrenAlternatives) {
                    // for each of this alternative children, find one that 'contains' otherChildren
                    var foundContainMatch = false
                    for (thisChildren in this.childrenAlternatives) {
                        if (thisChildren.size == otherChildren.size) {
                            // for each pair of nodes, one from each of otherChildren thisChildren
                            // check thisChildrenNode contains otherChildrenNode
                            var thisMatch = true
                            for (i in 0 until thisChildren.size) {
                                val thisChildrenNode = thisChildren.get(i)
                                val otherChildrenNode = otherChildren.get(i)
                                thisMatch = thisMatch && thisChildrenNode.contains(otherChildrenNode)
                            }
                            if (thisMatch) {
                                foundContainMatch = true
                                break
                            } else {
                                // if thisChildren alternative doesn't contain, try the next one
                                continue
                            }
                        } else {
                            // if sizes don't match check next in set of this alternative children
                            continue
                        }
                    }
                    allOthersAreContained = allOthersAreContained && foundContainMatch
                }
                return allOthersAreContained
            } else {
                // if identities don't match
                return false
            }

        } else {
            // if other is not a branch
            return false
        }
    }

    override val isEmptyLeaf: Boolean get() = false

    override val isLeaf: Boolean get() = false

    override val isBranch: Boolean get() = true

    override val asLeaf: SPPTLeaf get() = throw SPPTException("Not a Leaf", null)

    override val asBranch: SPPTBranch get() = this

    //override val lastLocation get() = if (children.isEmpty()) this.location else children.last().lastLocation

    override val lastLeaf: SPPTLeaf get() = children.last().lastLeaf

    override val location: InputLocation get() = TODO("not implemented")

    // --- Object ---
    override fun toString(): String {
        val tag = if (null == this.embeddedIn) this.runtimeRule.tag else "${embeddedIn}.${runtimeRule.tag}"
        var r = ""
        r += this.startPosition.toString() + ","
        r += this.nextInputPosition
        r += ":" + tag + "(" + this.runtimeRule.ruleNumber + ")"
        return r
    }

    override fun hashCode(): Int {
        return this.identity.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SPPTBranch) {
            return false
        } else {
            return if (this.identity != other.identity) {
                false
            } else {
                this.contains(other) && other.contains(this) //TODO: inefficient!
            }
        }
    }
}