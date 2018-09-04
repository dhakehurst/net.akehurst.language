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

package net.akehurst.language.ogl.runtime.graph

import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SPPTNodeIdentity
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.parser.sppt.SPPTBranchDefault
import net.akehurst.language.parser.sppt.SPPTNodeDefault

class ParseGraph {

    // TODO: remove, this is for test
    internal var with = true

    private val completeNodes: MutableMap<SPPTNodeIdentity, SPPTNodeDefault>  = mutableMapOf()
    private val growingHead: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()
    private val growing: MutableMap<GrowingNodeIndex, GrowingNode> = mutableMapOf()

    val canGrow : Boolean = false //TODO:

    val longestMatch : SPPTNode get() {
        throw UnsupportedOperationException()
    }

    private fun createBranchNoChildren(runtimeRule: RuntimeRule, priority: Int, startPosition: Int, nextInputPosition: Int): SPPTBranchDefault {
        val children = emptyList<SPPTNode>()
        val gn = SPPTBranchDefault(runtimeRule, startPosition, nextInputPosition, children, priority)
        this.completeNodes.put(gn.identity, gn)
        return gn
    }

    private fun findCompleteNode(runtimeRuleNumber: Int, startPosition: Int, nextInputPosition: Int) : SPPTNodeDefault? {
        return null // TODO
    }

    private fun addGrowing(gn: GrowingNode) {
        val ruleNumber = gn.runtimeRule.number
        val startPosition = gn.startPosition
        val nextInputPosition = gn.nextInputPosition
        val nextItemIndex = gn.nextItemIndex
        val gnindex = GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex)
        val existing = this.growing[gnindex]
        if (null == existing) {
            this.growing[gnindex] = gn
        } else {
            // merge
            for (info in gn.previous) {
                existing.addPrevious(info.node, info.atPosition)
            }
        }
    }

    private fun addGrowingHead(gnindex: GrowingNodeIndex, gn: GrowingNode): GrowingNode? {
        val existingGrowing = this.growing[gnindex]
        if (null != existingGrowing) {
            // don't add the head, previous should already have been merged
            return null
        } else {
            val existing = this.growingHead.get(gnindex)
            if (null == existing) {
                this.growingHead.put(gnindex, gn)
                return gn
            } else {
                // merge
                for (info in gn.previous) {
                    existing.addPrevious(info.node, info.atPosition)
                }
                return existing
            }
        }
    }

    private fun findOrCreateGrowingNode(runtimeRule: RuntimeRule, startPosition: Int, nextInputPosition: Int, nextItemIndex: Int,
                                        priority: Int, children: List<SPPTNodeDefault>, numNonSkipChildren: Int, previous: Set<PreviousInfo>): GrowingNode {
        val ruleNumber = runtimeRule.number
        val gnindex = GrowingNodeIndex(ruleNumber, startPosition, nextInputPosition, nextItemIndex)
        val existing = this.growing.get(gnindex)
        var result: GrowingNode? = null
        if (null == existing) {
            val nn = GrowingNode(runtimeRule, startPosition, nextInputPosition, nextItemIndex, priority, children, numNonSkipChildren)
            for (info in previous) {
                nn.addPrevious(info.node, info.atPosition)
                // TODO: remove, this is for test
                if (this.with) {
                    this.addGrowing(info.node)
                }
            }
            this.addGrowingHead(gnindex, nn)
            if (nn.hasCompleteChildren) {
                this.complete(nn)
            }
            result = nn
        } else {
            for (info in previous) {
                existing!!.addPrevious(info.node, info.atPosition)
                // TODO: remove, this is for test
                if (this.with) {
                    this.addGrowing(info.node)
                }
            }
            this.addGrowingHead(gnindex, existing)
            result = existing
        }
        return result
    }

    fun complete(gn: GrowingNode): SPPTNode? {
        if (gn.hasCompleteChildren) {
            val runtimeRule = gn.runtimeRule
            val priority = gn.priority
            val startPosition = gn.startPosition
            val nextInputPosition = gn.nextInputPosition
            var cn: SPPTNodeDefault? = this.findCompleteNode(runtimeRule.number, startPosition, nextInputPosition)
            if (null == cn) {
                cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, nextInputPosition)
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    cn.childrenAlternatives.add(gn.children)
                }
            } else {
                if (gn.isLeaf) {
                    // dont try and add children...can't for a leaf
                } else {
                    // final ICompleteNode.ChildrenOption opt = new ICompleteNode.ChildrenOption();
                    // opt.matchedLength = gn.getMatchedTextLength();
                    // opt.nodes = gn.getGrowingChildren();
                    cn = (cn as SPPTBranchDefault)
                    // TODO: don't add duplicate children
                    // somewhere resolve priorities!
                    val existingPriority = cn.priority
                    val newPriority = gn.priority
                    if (existingPriority == newPriority) {
                        // TODO: record/log ambiguity!
                        cn.childrenAlternatives.add(gn.children)
                        if (gn.isEmptyRuleMatch && cn.isEmptyRuleMatch) {
                            if (cn.childrenAlternatives.isEmpty()) {
                                cn.childrenAlternatives.add(gn.children)
                            } else {
                                if (cn.childrenAlternatives.iterator().next().get(0).isEmptyLeaf) {
                                    // leave it, no need to add empty alternatives
                                } else {
                                    if (gn.children.get(0).isEmptyLeaf) {
                                        // use just the empty leaf
                                        cn.childrenAlternatives.clear()
                                        cn.childrenAlternatives.add(gn.children)
                                    } else {
                                        // add the alternatives
                                        cn.childrenAlternatives.add(gn.children)
                                    }
                                }
                            }

                        } else {
                            cn.childrenAlternatives.add(gn.children)

                        }
                    } else if (existingPriority < newPriority) {
                        // do nothing, drop new one
                        val i = 0
                    } else if (newPriority < existingPriority) {
                        // replace existing with new
                        cn.childrenAlternatives.clear()
                        cn.childrenAlternatives.add(gn.children)
                    }
                }
            }

            this.checkForGoal(cn)
            return cn
        } else {
            return null
        }
    }


    fun start(goalRule:RuntimeRule) {
        val gnindex = GrowingNodeIndex(goalRule.number, 0, 0, 0)
        val gn = GrowingNode(goalRule, 0, 0, 0, 0, emptyList<SPPTNodeDefault>(), 0)
        this.addGrowingHead(gnindex, gn)
        if (gn.hasCompleteChildren) {
            val cn = this.createBranchNoChildren(goalRule, 0, 0, 0)
            if (gn.isLeaf) {
                // dont try and add children...can't for a leaf
            } else {
                cn.childrenAlternatives.add(gn.children)
            }
        }
    }

    fun checkForGoal(node: SPPTNodeDefault) {
        //TODO
    }

    fun grow() {
        //TODO
    }

}